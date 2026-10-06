package com.tripmind.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.proposals.ProposalChange;
import com.tripmind.domains.proposals.ProposalChange.PlaceRef;
import com.tripmind.domains.responses.ApplyProposalResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.entities.AiProposalEntity;
import com.tripmind.entities.AiToolExecutionEntity;
import com.tripmind.entities.ConversationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.*;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.AiProposalRepository;
import com.tripmind.repositories.AiToolExecutionRepository;
import com.tripmind.services.*;
import com.tripmind.services.ai.tools.PlaceRatingLookup;
import com.tripmind.services.ai.tools.PlaceTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Sinh lịch trình cho chuyến mới, chạy nền (FR-609, FR-610).
 *
 * <p>Mô hình chỉ đưa ra <i>ý tưởng</i> (tên địa điểm + truy vấn tìm kiếm). Mỗi ý tưởng phải khớp
 * một kết quả tìm kiếm có thật, tên khớp ít nhất một nửa số từ, không trùng lặp và không phải
 * chỗ người dùng đã chê; không khớp thì bị loại và đếm lại (BR-509). Kết quả được ghi bằng
 * đúng luồng đề xuất → áp dụng: mỗi hoạt động truy được về đề xuất gốc và hoàn tác được.
 */
@Slf4j
@Service
public class ItineraryGenerationService {

    private static final Map<TravelStyle, Integer> PACE = Map.of(
            TravelStyle.RELAXED, 3, TravelStyle.BALANCED, 4, TravelStyle.FAST_PACED, 6);
    private static final Set<ActivityType> PLACELESS = Set.of(ActivityType.REST, ActivityType.TRANSPORT, ActivityType.OTHER);
    private static final double MIN_NAME_MATCH = 0.5;
    private static final Duration JOB_RETENTION = Duration.ofHours(1);

    private final LlmGateway llm;
    private final TripService tripService;
    private final ActivityRepository activityRepository;
    private final AiProposalRepository proposalRepository;
    private final AiToolExecutionRepository executionRepository;
    private final ConversationService conversationService;
    private final ProposalApplyService applyService;
    private final PlaceService placeService;
    private final WeatherService weatherService;
    private final CostEstimator costEstimator;
    private final PlaceRatingLookup placeRatingLookup;
    private final SystemPromptBuilder promptBuilder;
    private final RateLimiter rateLimiter;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final String template;

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    public ItineraryGenerationService(LlmGateway llm, TripService tripService, ActivityRepository activityRepository,
                                      AiProposalRepository proposalRepository, AiToolExecutionRepository executionRepository,
                                      ConversationService conversationService, ProposalApplyService applyService,
                                      PlaceService placeService, WeatherService weatherService, CostEstimator costEstimator,
                                      PlaceRatingLookup placeRatingLookup, SystemPromptBuilder promptBuilder,
                                      RateLimiter rateLimiter, AiProperties properties, ObjectMapper objectMapper,
                                      @Qualifier("aiExecutor") ExecutorService executor,
                                      @Value("classpath:prompts/generate-itinerary.md") Resource template) {
        this.llm = llm;
        this.tripService = tripService;
        this.activityRepository = activityRepository;
        this.proposalRepository = proposalRepository;
        this.executionRepository = executionRepository;
        this.conversationService = conversationService;
        this.applyService = applyService;
        this.placeService = placeService;
        this.weatherService = weatherService;
        this.costEstimator = costEstimator;
        this.placeRatingLookup = placeRatingLookup;
        this.promptBuilder = promptBuilder;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.executor = executor;
        try {
            this.template = template.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc mau sinh lich trinh", e);
        }
    }

    /** Trạng thái một lần sinh; giữ trong bộ nhớ tiến trình, khởi động lại thì mất (ADS-30 §9.3). */
    public static final class Job {
        private final String id;
        private final Long userId;
        private final Long tripId;
        private final Instant createdAt = Instant.now();
        private volatile String state = "QUEUED";
        private volatile String step = "Đang xếp hàng...";
        private volatile double progress;
        private volatile Map<String, Object> result;
        private volatile String error;

        Job(String id, Long userId, Long tripId) {
            this.id = id;
            this.userId = userId;
            this.tripId = tripId;
        }

        void update(String state, String step, double progress) {
            this.state = state;
            this.step = step;
            this.progress = progress;
        }

        public Map<String, Object> view() {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("jobId", id);
            view.put("state", state);
            view.put("step", step);
            view.put("progress", progress);
            if (result != null) {
                view.put("result", result);
            }
            if (error != null) {
                view.put("error", error);
            }
            return view;
        }
    }

    public Map<String, Object> start(Long userId, Long tripId) {
        tripService.getOwnedTrip(userId, tripId);
        if (!llm.isConfigured()) {
            throw new AppException(ErrorCode.AI_NOT_CONFIGURED, "AI assistant is not configured (GEMINI_API_KEY is empty)");
        }
        if (activityRepository.countByItineraryDayTripId(tripId) > 0) {
            throw new AppException(ErrorCode.TRIP_NOT_EMPTY, "Itinerary can only be generated for an empty trip");
        }
        rateLimiter.check("ai", userId, properties.rateLimitPerHour(), Duration.ofHours(1));
        jobs.values().removeIf(j -> j.createdAt.isBefore(Instant.now().minus(JOB_RETENTION)));

        Job job = new Job(UUID.randomUUID().toString(), userId, tripId);
        jobs.put(job.id, job);
        executor.submit(() -> run(job));
        return job.view();
    }

    public Map<String, Object> status(Long userId, Long tripId, String jobId) {
        Job job = jobs.get(jobId);
        if (job == null || !job.userId.equals(userId) || !job.tripId.equals(tripId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Generation job not found: " + jobId);
        }
        return job.view();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Plan(List<PlanDay> days) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlanDay(Integer dayNumber, List<PlanItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlanItem(String title, String activityType, String placeName, String searchQuery, String startTime,
                    String endTime, String notes, Long estimatedCost) {
    }

    record Rejected(int dayNumber, String title, String placeName, String reason) {
    }

    void run(Job job) {
        try {
            job.update("RUNNING", "Đang đọc thông tin chuyến đi...", 0.1);
            TripEntity trip = tripService.getOwnedTrip(job.userId, job.tripId);
            ConversationEntity conversation = conversationService.openOrCreate(job.userId, job.tripId, null,
                    "Sinh lịch trình tự động");
            int pace = trip.getTravelStyle() == null ? PACE.get(TravelStyle.BALANCED) : PACE.get(trip.getTravelStyle());
            int maxPerDay = Math.max(2, pace
                    - (trip.getGroupProfile() != null && trip.getGroupProfile().needsGentlePace() ? 1 : 0));

            job.update("RUNNING", "Đang lên ý tưởng lịch trình...", 0.3);
            Plan plan = askModel(trip, maxPerDay);

            job.update("RUNNING", "Đang tìm địa điểm có thật...", 0.6);
            List<Long> executionIds = new ArrayList<>();
            List<Rejected> rejected = new ArrayList<>();
            List<ProposalChange> changes = verify(trip, plan, maxPerDay, conversation.getId(), executionIds, rejected);
            if (changes.isEmpty()) {
                throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "No verifiable activity in the generated plan");
            }

            job.update("RUNNING", "Đang ghi lịch trình...", 0.9);
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("reason", "Sinh lịch trình tự động cho chuyến mới");
            evidence.put("toolExecutionIds", executionIds);
            evidence.put("rejected", rejected);
            AiProposalEntity proposal = proposalRepository.save(AiProposalEntity.builder()
                    .tripId(trip.getId())
                    .conversationId(conversation.getId())
                    .summary("Lịch trình tự động " + trip.lengthInDays() + " ngày: " + changes.size() + " hoạt động")
                    .changes(ProposalChange.toJson(objectMapper, changes))
                    .estimatedCostDelta(changes.stream().mapToLong(c -> c instanceof ProposalChange.Add a && a.estimatedCost() != null ? a.estimatedCost() : 0).sum())
                    .travelTimeDelta(0)
                    .status(ProposalStatus.PENDING)
                    .kind(ProposalKind.ITINERARY)
                    .evidence(objectMapper.valueToTree(evidence))
                    .expiresAt(Instant.now().plus(properties.proposalTtl()))
                    .build());
            if (activityRepository.countByItineraryDayTripId(trip.getId()) > 0) {
                throw new AppException(ErrorCode.TRIP_NOT_EMPTY, "Trip received activities while generating");
            }
            ApplyProposalResponse applied = applyService.apply(job.userId, job.tripId, proposal.getId(), PlaceAdoption.AI_GENERATE);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("proposalId", proposal.getId());
            result.put("activitiesCreated", applied.getApplied());
            result.put("rejectedCount", rejected.size());
            result.put("rejected", rejected);
            result.put("undoAvailableUntil", applied.getUndoAvailableUntil());
            job.result = result;
            job.update("DONE", "Hoàn tất", 1.0);
            log.info("Generated {} activities for trip ID={} ({} rejected)", applied.getApplied(), trip.getId(), rejected.size());
        } catch (AppException e) {
            job.error = e.getErrorCode().name() + ": " + e.getMessage();
            job.update("FAILED", "Không sinh được lịch trình", job.progress);
            log.warn("Sinh lich trinh that bai cho trip ID={}: {}", job.tripId, e.getMessage());
        } catch (Exception e) {
            job.error = "INTERNAL_SERVER_ERROR";
            job.update("FAILED", "Không sinh được lịch trình", job.progress);
            log.error("Sinh lich trinh loi", e);
        }
    }

    private Plan askModel(TripEntity trip, int maxPerDay) {
        TripWeatherResponse weather = weatherService.forTrip(trip);
        String weatherText = weather.getDays().isEmpty() ? "Không có số liệu."
                : weather.getDays().stream()
                .map(d -> "Ngày " + d.getDayNumber() + ": " + d.getSummary() + ", " + d.getTempMin() + "–" + d.getTempMax()
                        + "°C, khả năng mưa " + d.getPrecipitationProbability() + "% (" + d.getSource() + ")")
                .collect(Collectors.joining("\n"));
        List<String> disliked = placeRatingLookup.dislikedNames(trip.getUser().getId());
        String system = template
                .replace("{tripSummary}", promptBuilder.tripSummary(trip))
                .replace("{groupSummary}", promptBuilder.groupSummary(trip))
                .replace("{weather}", weatherText)
                .replace("{disliked}", disliked.isEmpty() ? "(không có)" : String.join(", ", disliked))
                .replace("{maxPerDay}", String.valueOf(maxPerDay))
                .replace("{days}", String.valueOf(trip.lengthInDays()))
                .replace("{travelers}", String.valueOf(trip.getTravelers()))
                .replace("{currency}", trip.getCurrency() != null ? trip.getCurrency() : "VND");
        String text = llm.call(new Prompt(List.of(new SystemMessage(system), new UserMessage("Lập lịch trình.")),
                OpenAiChatOptions.builder().build())).getResult().getOutput().getText();
        try {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                log.error("AI khong tra ve JSON hop le. Raw text: {}", text);
                throw new IllegalArgumentException("No JSON found in response");
            }
            Plan plan = objectMapper.readValue(text.substring(start, end + 1), Plan.class);
            if (plan.days() == null) {
                throw new IllegalArgumentException("missing days");
            }
            return plan;
        } catch (Exception e) {
            log.error("Khong doc duoc plan tu AI: {}. Raw text: {}", e.getMessage(), text);
            throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "Model returned an unreadable plan");
        }
    }

    private List<ProposalChange> verify(TripEntity trip, Plan plan, int maxPerDay, Long conversationId,
                                        List<Long> executionIds, List<Rejected> rejected) {
        Set<String> disliked = placeRatingLookup.dislikedExternalIds(trip.getUser().getId());
        Set<String> used = new HashSet<>();
        List<ProposalChange> changes = new ArrayList<>();
        int days = trip.lengthInDays();
        for (PlanDay day : plan.days()) {
            if (day.dayNumber() == null || day.dayNumber() < 1 || day.dayNumber() > days || day.items() == null) {
                continue;
            }
            int kept = 0;
            for (PlanItem item : day.items()) {
                if (item.title() == null || item.title().isBlank()) {
                    continue;
                }
                if (kept >= maxPerDay) {
                    rejected.add(new Rejected(day.dayNumber(), item.title(), item.placeName(), "DAY_FULL"));
                    continue;
                }
                ActivityType type = parseType(item.activityType());
                String query = firstNonBlank(item.searchQuery(), item.placeName());
                PlaceRef place = null;
                if (query == null) {
                    if (!PLACELESS.contains(type)) {
                        rejected.add(new Rejected(day.dayNumber(), item.title(), null, "NO_PLACE"));
                        continue;
                    }
                } else {
                    Optional<PlaceResponse> match = findReal(trip, query, item.placeName(), type, item.title(),
                            item.notes(), disliked, used, conversationId, executionIds);
                    if (match.isEmpty()) {
                        rejected.add(new Rejected(day.dayNumber(), item.title(), item.placeName(), "PLACE_NOT_FOUND"));
                        continue;
                    }
                    PlaceResponse p = match.get();
                    used.add(p.getExternalId());
                    place = new PlaceRef(p.getProvider(), p.getExternalId(), p.getName(), p.getLatitude(), p.getLongitude(),
                            p.getPriceLevel());
                }
                LocalTime start = parseTime(item.startTime());
                LocalTime end = parseTime(item.endTime());
                if (start != null && end != null && end.isBefore(start)) {
                    end = null;
                }
                // Phase 1: Contextual AI Market Estimation with Google Price Level benchmark sanity check
                Long cost = null;
                CostSource costSource = null;
                Long aiCost = item.estimatedCost();
                if (aiCost != null && aiCost < 0) {
                    aiCost = null;
                }

                if (place != null && place.priceLevel() != null) {
                    var googleEst = costEstimator.estimate(
                            place.priceLevel(), type, trip.getTravelers(), trip.getCurrency());
                    if (googleEst.getSuggested() != null) {
                        long googleSuggested = googleEst.getSuggested();
                        long googleMin = googleEst.getMin() != null ? googleEst.getMin() : 0;
                        long googleMax = googleEst.getMax() != null ? googleEst.getMax() : Long.MAX_VALUE;

                        if (aiCost != null && aiCost > 0) {
                            // If AI cost is within reasonable bound (0.4 * min to 1.8 * max), trust AI's nuanced contextual market price!
                            if (aiCost >= (long) (googleMin * 0.4) && aiCost <= (long) (googleMax * 1.8)) {
                                cost = aiCost;
                                costSource = CostSource.AI;
                            } else {
                                // Clamp to Google benchmark boundaries
                                cost = Math.clamp(aiCost, googleMin, googleMax);
                                costSource = CostSource.PRICE_LEVEL;
                            }
                        } else {
                            cost = googleSuggested;
                            costSource = CostSource.PRICE_LEVEL;
                        }
                    } else {
                        // Google does not price this activity type (e.g. ACCOMMODATION, TRANSPORT)
                        if (aiCost != null) {
                            cost = aiCost;
                            costSource = CostSource.AI;
                        }
                    }
                } else {
                    // No Google price_level available (or placeless activity like transport/rest)
                    if (aiCost != null) {
                        cost = aiCost;
                        costSource = CostSource.AI;
                    }
                }

                String activityTitle = truncate(item.title().strip(), 200);
                if (place != null && !activityTitle.toLowerCase().contains(place.name().toLowerCase())) {
                    activityTitle = activityTitle + " (" + place.name() + ")";
                }
                changes.add(new ProposalChange.Add(day.dayNumber(), activityTitle, type, place, start,
                        end, cost, costSource, item.notes(),
                        place == null ? null : "Khớp địa điểm thật: " + place.name()));
                kept++;
            }
        }
        return changes;
    }

    private static final Set<String> CAFE_TAGS = Set.of(
            "cafe", "coffee_shop", "tea_house", "bubble_tea_store", "juice_shop", "bar", "pub", "wine_bar"
    );

    private static final Set<String> RESTAURANT_TAGS = Set.of(
            "restaurant", "vietnamese_restaurant", "asian_restaurant", "seafood_restaurant",
            "chinese_restaurant", "japanese_restaurant", "korean_restaurant", "italian_restaurant",
            "french_restaurant", "fast_food_restaurant", "buffet_restaurant", "vegetarian_restaurant",
            "food_court", "diner", "meal_takeaway", "meal_delivery"
    );

    private static final Set<String> LODGING_TAGS = Set.of(
            "lodging", "hotel", "resort_hotel", "motel", "guest_house", "bed_and_breakfast",
            "hostel", "campground", "inn"
    );

    private static final Set<String> ATTRACTION_TAGS = Set.of(
            "tourist_attraction", "historical_landmark", "museum", "park", "national_park",
            "amusement_park", "place_of_worship", "church", "hindu_temple", "zoo", "aquarium",
            "art_gallery", "scenic_spot", "natural_feature"
    );

    private static boolean hasTag(PlaceResponse p, Set<String> tags) {
        if (p == null) return false;
        if (p.getPrimaryType() != null && tags.contains(p.getPrimaryType().toLowerCase(Locale.ROOT))) return true;
        if (p.getCategory() != null && tags.contains(p.getCategory().toLowerCase(Locale.ROOT))) return true;
        if (p.getTypes() != null) {
            for (String t : p.getTypes()) {
                if (tags.contains(t.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    private static boolean isCafeOrDrinkPlace(PlaceResponse p) {
        return hasTag(p, CAFE_TAGS);
    }

    private static boolean isRestaurantPlace(PlaceResponse p) {
        if (p == null) return false;
        if (hasTag(p, RESTAURANT_TAGS)) return true;
        if (p.getPrimaryType() != null && p.getPrimaryType().toLowerCase(Locale.ROOT).endsWith("_restaurant")) return true;
        if (p.getCategory() != null && p.getCategory().toLowerCase(Locale.ROOT).endsWith("_restaurant")) return true;
        if (p.getTypes() != null) {
            for (String t : p.getTypes()) {
                if (t.toLowerCase(Locale.ROOT).endsWith("_restaurant")) return true;
            }
        }
        return false;
    }

    private static boolean isLodgingPlace(PlaceResponse p) {
        return hasTag(p, LODGING_TAGS);
    }

    private static boolean isAttractionPlace(PlaceResponse p) {
        return hasTag(p, ATTRACTION_TAGS);
    }

    private static boolean isDrinkActivity(String title, String notes) {
        String text = ((title != null ? title : "") + " " + (notes != null ? notes : "")).toLowerCase(Locale.ROOT);
        return text.contains("cà phê") || text.contains("cafe") || text.contains("coffee")
                || text.contains("uống nước") || text.contains("quán nước") || text.contains("trà")
                || text.contains("đồ uống") || text.contains("chill") || text.contains("ngắm hoàng hôn")
                || text.contains("ngắm mây") || text.contains("ngắm cảnh") || text.contains("bar")
                || text.contains("pub");
    }

    private static boolean isMainMealActivity(String title, String notes, ActivityType type) {
        if (isDrinkActivity(title, notes)) {
            return false;
        }
        if (type == ActivityType.FOOD) {
            return true;
        }
        String text = ((title != null ? title : "") + " " + (notes != null ? notes : "")).toLowerCase(Locale.ROOT);
        return text.contains("ăn trưa") || text.contains("ăn tối") || text.contains("ăn sáng")
                || text.contains("bữa trưa") || text.contains("bữa tối") || text.contains("bữa sáng")
                || text.contains("cơm") || text.contains("lẩu") || text.contains("đặc sản")
                || text.contains("hải sản") || text.contains("nhà hàng") || text.contains("quán ăn")
                || text.contains("ẩm thực");
    }

    private static boolean isLodgingActivity(String title, String notes, ActivityType type) {
        if (type == ActivityType.ACCOMMODATION) {
            return true;
        }
        String text = ((title != null ? title : "") + " " + (notes != null ? notes : "")).toLowerCase(Locale.ROOT);
        return text.contains("khách sạn") || text.contains("hotel") || text.contains("resort")
                || text.contains("homestay") || text.contains("nghỉ đêm") || text.contains("nhận phòng")
                || text.contains("check-in khách sạn") || text.contains("lưu trú");
    }

    private static boolean isTagCompatible(PlaceResponse p, boolean isMeal, boolean isDrink, boolean isLodging, ActivityType type) {
        if (isMeal) {
            // Main meal activity: Must NOT be purely a cafe/drink shop or hotel without restaurant!
            if (isCafeOrDrinkPlace(p) && !isRestaurantPlace(p)) {
                return false;
            }
            if (isLodgingPlace(p) && !isRestaurantPlace(p)) {
                return false;
            }
            return true;
        }
        if (isDrink) {
            if (isLodgingPlace(p) && !isCafeOrDrinkPlace(p)) {
                return false;
            }
            return true;
        }
        if (isLodging) {
            return isLodgingPlace(p);
        }
        if (type == ActivityType.SIGHTSEEING) {
            if (isLodgingPlace(p) && !isAttractionPlace(p)) {
                return false;
            }
            return true;
        }
        return true;
    }

    private static double scoreCandidate(PlaceResponse p, String placeName,
                                         boolean isMeal, boolean isDrink, boolean isLodging) {
        double score = 0.0;
        if (placeName != null && !placeName.isBlank()) {
            score += nameMatch(placeName, p.getName()) * 50.0;
        } else {
            score += 20.0;
        }
        if (isMeal && isRestaurantPlace(p)) {
            score += 40.0;
        }
        if (isDrink && isCafeOrDrinkPlace(p)) {
            score += 40.0;
        }
        if (isLodging && isLodgingPlace(p)) {
            score += 40.0;
        }
        if (isAttractionPlace(p)) {
            score += 10.0;
        }
        if (p.getRating() != null) {
            score += p.getRating().doubleValue() * 2.0;
            if (p.getUserRatingsTotal() != null && p.getUserRatingsTotal() > 0) {
                score += Math.min(5.0, Math.log10(p.getUserRatingsTotal()));
            }
        }
        return score;
    }

    private Optional<PlaceResponse> pickBestMatch(List<PlaceResponse> candidates, String placeName,
                                                  boolean isMeal, boolean isDrink, boolean isLodging,
                                                  ActivityType type, Set<String> disliked, Set<String> used) {
        return candidates.stream()
                .filter(p -> p.getExternalId() != null && !disliked.contains(p.getExternalId()) && !used.contains(p.getExternalId()))
                .filter(p -> isTagCompatible(p, isMeal, isDrink, isLodging, type))
                .filter(p -> placeName == null || placeName.isBlank() || nameMatch(placeName, p.getName()) >= MIN_NAME_MATCH)
                .max(java.util.Comparator.comparingDouble(p -> scoreCandidate(p, placeName, isMeal, isDrink, isLodging)));
    }

    /** Tìm địa điểm thật cho một ý tưởng và ghi lượt tìm vào nhật ký công cụ (BR-505). */
    private Optional<PlaceResponse> findReal(TripEntity trip, String query, String placeName,
                                             ActivityType activityType, String title, String notes,
                                             Set<String> disliked, Set<String> used,
                                             Long conversationId, List<Long> executionIds) {
        boolean isDrink = isDrinkActivity(title, notes);
        boolean isMeal = isMainMealActivity(title, notes, activityType);
        boolean isLodging = isLodgingActivity(title, notes, activityType);

        String effective = PlaceTools.withDietaryKeywords(query, trip.getGroupProfile());
        long started = System.nanoTime();
        List<PlaceResponse> results;
        ToolExecutionStatus status = ToolExecutionStatus.OK;
        String error = null;
        try {
            results = placeService.searchPlaces(effective, trip.getDestination().getName());
        } catch (RuntimeException e) {
            results = List.of();
            status = ToolExecutionStatus.ERROR;
            error = e.getMessage();
        }

        Optional<PlaceResponse> match = pickBestMatch(results, placeName, isMeal, isDrink, isLodging, activityType, disliked, used);

        // Fallback by tag if initial query didn't return any compatible place for this category
        if (match.isEmpty()) {
            if (isMeal) {
                try {
                    List<PlaceResponse> mealFallbacks = placeService.searchPlaces("nhà hàng " + trip.getDestination().getName(), trip.getDestination().getName());
                    match = pickBestMatch(mealFallbacks, null, true, false, false, activityType, disliked, used);
                } catch (Exception ignored) {
                }
            } else if (isDrink) {
                try {
                    List<PlaceResponse> drinkFallbacks = placeService.searchPlaces("quán cà phê " + trip.getDestination().getName(), trip.getDestination().getName());
                    match = pickBestMatch(drinkFallbacks, null, false, true, false, activityType, disliked, used);
                } catch (Exception ignored) {
                }
            } else if (isLodging) {
                try {
                    List<PlaceResponse> lodgingFallbacks = placeService.searchPlaces("khách sạn " + trip.getDestination().getName(), trip.getDestination().getName());
                    match = pickBestMatch(lodgingFallbacks, null, false, false, true, activityType, disliked, used);
                } catch (Exception ignored) {
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidates", results.stream().limit(properties.maxSearchResults()).map(PlaceResponse::getName).toList());
        result.put("picked", match.map(PlaceResponse::getName).orElse(null));
        AiToolExecutionEntity saved = executionRepository.save(AiToolExecutionEntity.builder()
                .conversationId(conversationId)
                .tripId(trip.getId())
                .userId(trip.getUser().getId())
                .toolName("search_places")
                .arguments(objectMapper.valueToTree(Map.of("query", effective, "placeName", String.valueOf(placeName))))
                .result(objectMapper.valueToTree(result))
                .status(status)
                .errorMessage(error)
                .executionTimeMs((int) ((System.nanoTime() - started) / 1_000_000))
                .build());
        executionIds.add(saved.getId());
        return match;
    }

    /** Tỉ lệ số từ của tên mô hình đưa ra có mặt trong tên địa điểm thật (bỏ dấu, không phân biệt hoa thường). */
    static double nameMatch(String expected, String actual) {
        Set<String> want = tokens(expected);
        if (want.isEmpty()) {
            return 0;
        }
        Set<String> have = tokens(actual);
        long hit = want.stream().filter(have::contains).count();
        return hit / (double) want.size();
    }

    private static Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        String plain = Normalizer.normalize(text.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Arrays.stream(plain.split("[^a-z0-9]+")).filter(t -> t.length() >= 2).collect(Collectors.toSet());
    }

    private static ActivityType parseType(String value) {
        try {
            return value == null ? ActivityType.OTHER : ActivityType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActivityType.OTHER;
        }
    }

    private static LocalTime parseTime(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalTime.parse(value.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.strip();
        }
        return b != null && !b.isBlank() ? b.strip() : null;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
