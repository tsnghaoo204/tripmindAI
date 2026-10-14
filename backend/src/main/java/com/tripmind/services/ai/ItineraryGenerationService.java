package com.tripmind.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.proposals.ProposalChange;
import com.tripmind.domains.proposals.ProposalChange.PlaceRef;
import com.tripmind.domains.responses.ApplyProposalResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.AiProposalEntity;
import com.tripmind.entities.AiToolExecutionEntity;
import com.tripmind.entities.ConversationEntity;
import com.tripmind.entities.DestinationKnowledgeEntity;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tripmind.domains.ai.ItineraryJobPayload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
            TravelStyle.RELAXED, 4, TravelStyle.BALANCED, 5, TravelStyle.FAST_PACED, 7);
    private static final Set<ActivityType> PLACELESS = Set.of(ActivityType.REST, ActivityType.TRANSPORT, ActivityType.OTHER);
    private static final double MIN_NAME_MATCH = 0.5;

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
    private final ActivityAlternativeService alternativeService;
    private final DestinationKnowledgeService destinationKnowledgeService;
    private final RedissonClient redissonClient;
    private final String template;

    private static final String QUEUE_NAME = "tripmind:itinerary:queue";
    private static final String JOB_KEY_PREFIX = "tripmind:itinerary:job:";
    private static final String TOPIC_PREFIX = "tripmind:itinerary:topic:";
    private static final String LOCK_PREFIX = "tripmind:itinerary:lock:";
    private static final Duration JOB_RETENTION = Duration.ofHours(2);

    private volatile boolean stopped = false;

    public ItineraryGenerationService(LlmGateway llm, TripService tripService, ActivityRepository activityRepository,
                                      AiProposalRepository proposalRepository, AiToolExecutionRepository executionRepository,
                                      ConversationService conversationService, ProposalApplyService applyService,
                                      PlaceService placeService, WeatherService weatherService, CostEstimator costEstimator,
                                      PlaceRatingLookup placeRatingLookup, SystemPromptBuilder promptBuilder,
                                      RateLimiter rateLimiter, AiProperties properties, ObjectMapper objectMapper,
                                      @Qualifier("aiExecutor") ExecutorService executor,
                                      ActivityAlternativeService alternativeService,
                                      DestinationKnowledgeService destinationKnowledgeService,
                                      RedissonClient redissonClient,
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
        this.alternativeService = alternativeService;
        this.destinationKnowledgeService = destinationKnowledgeService;
        this.redissonClient = redissonClient;
        try {
            this.template = template.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc mau sinh lich trinh", e);
        }
    }

    @PostConstruct
    public void startWorkers() {
        // Chạy worker lắng nghe hàng đợi Redisson bằng luồng ảo (Virtual Threads)
        int workerCount = 2;
        for (int i = 0; i < workerCount; i++) {
            executor.submit(this::workerLoop);
        }
    }

    @PreDestroy
    public void stopWorkers() {
        this.stopped = true;
    }

    private void workerLoop() {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(QUEUE_NAME);
        while (!stopped && !Thread.currentThread().isInterrupted()) {
            try {
                String jobId = queue.poll(1, TimeUnit.SECONDS);
                if (jobId != null) {
                    processJob(jobId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Loi trong worker loop cua Redisson queue: {}", e.getMessage());
            }
        }
    }

    private void processJob(String jobId) {
        ItineraryJobPayload job = getJob(jobId);
        if (job == null) {
            log.warn("Khong tim thay job ID={} trong Redis de xu ly", jobId);
            return;
        }

        RLock lock = redissonClient.getLock(LOCK_PREFIX + job.getTripId());
        try {
            // Thử lấy lock trong 2 giây, giữ lock tối đa 3 phút
            if (!lock.tryLock(2, 180, TimeUnit.SECONDS)) {
                job.setError("Lịch trình cho chuyến đi này đang được xử lý ở một tác vụ khác.");
                updateJob(job, "FAILED", "Chuyến đi đang được xử lý", job.getProgress());
                return;
            }

            run(job);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Map<String, Object> start(Long userId, Long tripId) {
        tripService.getOwnedTrip(userId, tripId);
        if (!llm.isConfigured()) {
            throw new AppException(ErrorCode.AI_NOT_CONFIGURED, "AI assistant is not configured (LLM_API_KEY or LLM_MODEL is empty)");
        }
        if (activityRepository.countByItineraryDayTripId(tripId) > 0) {
            throw new AppException(ErrorCode.TRIP_NOT_EMPTY, "Itinerary can only be generated for an empty trip");
        }
        rateLimiter.check("ai", userId, properties.rateLimitPerHour(), Duration.ofHours(1));

        String jobId = UUID.randomUUID().toString();
        ItineraryJobPayload job = ItineraryJobPayload.builder()
                .id(jobId)
                .userId(userId)
                .tripId(tripId)
                .createdAt(Instant.now())
                .state("QUEUED")
                .step("Đang xếp hàng trong hệ thống...")
                .progress(0.0)
                .build();

        saveJob(job);
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(QUEUE_NAME);
        queue.add(jobId);
        log.info("Pushed itinerary generation job ID={} to Redisson queue for trip ID={}", jobId, tripId);

        return job.toView();
    }

    public Map<String, Object> status(Long userId, Long tripId, String jobId) {
        ItineraryJobPayload job = getJob(jobId);
        if (job == null || !job.getUserId().equals(userId) || !job.getTripId().equals(tripId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Generation job not found: " + jobId);
        }
        return job.toView();
    }

    public SseEmitter subscribe(Long userId, Long tripId, String jobId) {
        ItineraryJobPayload job = getJob(jobId);
        if (job == null || !job.getUserId().equals(userId) || !job.getTripId().equals(tripId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Generation job not found: " + jobId);
        }
        SseEmitter emitter = new SseEmitter(180_000L);

        // Snapshot ban đầu
        try {
            String eventName = switch (job.getState()) {
                case "DONE" -> "done";
                case "FAILED" -> "error";
                default -> "progress";
            };
            emitter.send(SseEmitter.event().name(eventName).data(job.toView(), MediaType.APPLICATION_JSON));
            if ("DONE".equals(job.getState()) || "FAILED".equals(job.getState())) {
                emitter.complete();
                return emitter;
            }
        } catch (IOException | IllegalStateException e) {
            return emitter;
        }

        RTopic topic = redissonClient.getTopic(TOPIC_PREFIX + jobId);
        final int[] listenerId = new int[1];
        final AtomicBoolean completed = new AtomicBoolean(false);

        Runnable cleanup = () -> {
            if (completed.compareAndSet(false, true)) {
                try {
                    topic.removeListener(listenerId[0]);
                } catch (Exception ignored) {
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(e -> cleanup.run());

        listenerId[0] = topic.addListener(String.class, (channel, msgJson) -> {
            try {
                ItineraryJobPayload updated = objectMapper.readValue(msgJson, ItineraryJobPayload.class);
                String eventName = switch (updated.getState()) {
                    case "DONE" -> "done";
                    case "FAILED" -> "error";
                    default -> "progress";
                };
                emitter.send(SseEmitter.event().name(eventName).data(updated.toView(), MediaType.APPLICATION_JSON));
                if ("DONE".equals(updated.getState()) || "FAILED".equals(updated.getState())) {
                    cleanup.run();
                    emitter.complete();
                }
            } catch (IOException | IllegalStateException ex) {
                cleanup.run();
            }
        });

        return emitter;
    }

    private void updateJob(ItineraryJobPayload job, String state, String step, double progress) {
        job.setState(state);
        job.setStep(step);
        job.setProgress(progress);
        saveJob(job);
        publishJobEvent(job);
    }

    private void saveJob(ItineraryJobPayload job) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(JOB_KEY_PREFIX + job.getId());
            String json = objectMapper.writeValueAsString(job);
            bucket.set(json, JOB_RETENTION);
        } catch (Exception e) {
            log.error("Loi khi luu job vao Redisson: {}", e.getMessage());
        }
    }

    private ItineraryJobPayload getJob(String jobId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(JOB_KEY_PREFIX + jobId);
            String json = bucket.get();
            if (json != null) {
                return objectMapper.readValue(json, ItineraryJobPayload.class);
            }
        } catch (Exception e) {
            log.error("Loi khi doc job tu Redisson: {}", e.getMessage());
        }
        return null;
    }

    private void publishJobEvent(ItineraryJobPayload job) {
        try {
            RTopic topic = redissonClient.getTopic(TOPIC_PREFIX + job.getId());
            String json = objectMapper.writeValueAsString(job);
            topic.publish(json);
        } catch (Exception e) {
            log.warn("Loi khi publish event len Redisson topic: {}", e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Plan(List<PlanDay> days) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlanDay(Integer dayNumber, List<PlanItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlanItem(String title, String activityType, String placeId, String placeName, String searchQuery, String startTime,
                    String endTime, String notes, Long estimatedCost) {
    }

    record Rejected(int dayNumber, String title, String placeName, String reason) {
    }

    record PoolPlace(String code, String category, PlaceResponse place, double score) {
    }

    private record PoolQuery(String category, String label, String query, int limit) {
    }

    // Điểm Bayes: kéo điểm sao về mức trung bình khi ít lượt đánh giá
    private static final double PRIOR_RATING = 4.0;
    private static final int PRIOR_VOTES = 30;

    void run(ItineraryJobPayload job) {
        try {
            updateJob(job, "RUNNING", "Đang đọc thông tin chuyến đi...", 0.1);
            TripEntity trip = tripService.getOwnedTrip(job.getUserId(), job.getTripId());
            ConversationEntity conversation = conversationService.openOrCreate(job.getUserId(), job.getTripId(), null,
                    "Sinh lịch trình tự động");
            int pace = trip.getTravelStyle() == null ? PACE.get(TravelStyle.BALANCED) : PACE.get(trip.getTravelStyle());
            int maxPerDay = Math.max(3, pace
                    - (trip.getGroupProfile() != null && trip.getGroupProfile().needsGentlePace() ? 1 : 0));

            updateJob(job, "RUNNING", "Đang tìm địa điểm nổi bật từ Google Places...", 0.2);
            List<Long> executionIds = new ArrayList<>();
            Map<String, PoolPlace> pool = buildPool(trip, conversation.getId(), executionIds);

            updateJob(job, "RUNNING", "Đang lên ý tưởng lịch trình...", 0.4);
            Plan plan = askModel(trip, maxPerDay, pool);

            updateJob(job, "RUNNING", "Đang tìm địa điểm có thật...", 0.6);
            List<Rejected> rejected = new ArrayList<>();
            List<ProposalChange> changes = verify(trip, plan, pool, maxPerDay, conversation.getId(), executionIds, rejected);
            if (changes.isEmpty()) {
                throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "No verifiable activity in the generated plan");
            }

            updateJob(job, "RUNNING", "Đang ghi lịch trình...", 0.9);
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
            ApplyProposalResponse applied = applyService.apply(job.getUserId(), job.getTripId(), proposal.getId(), PlaceAdoption.AI_GENERATE);

            // Tự động tạo 3 lựa chọn ngân sách (Alternatives) cho các hoạt động ăn uống và lưu trú
            try {
                List<ActivityEntity> createdActs = activityRepository.findByItineraryDayTripIdOrderByItineraryDayDayNumberAscOrderIndexAsc(trip.getId());
                for (ActivityEntity act : createdActs) {
                    if (act.getActivityType() == ActivityType.FOOD || act.getActivityType() == ActivityType.ACCOMMODATION) {
                        alternativeService.generateForActivity(act, trip);
                    }
                }
            } catch (Exception ex) {
                log.warn("Loi khi sinh lua chon thay the cho cac hoat dong cua trip ID={}: {}", trip.getId(), ex.getMessage());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("proposalId", proposal.getId());
            result.put("activitiesCreated", applied.getApplied());
            result.put("rejectedCount", rejected.size());
            result.put("rejected", rejected);
            result.put("undoAvailableUntil", applied.getUndoAvailableUntil());
            job.setResult(result);
            updateJob(job, "DONE", "Hoàn tất", 1.0);
            log.info("Generated {} activities for trip ID={} ({} rejected)", applied.getApplied(), trip.getId(), rejected.size());
        } catch (AppException e) {
            job.setError(e.getErrorCode().name() + ": " + e.getMessage());
            updateJob(job, "FAILED", "Không sinh được lịch trình", job.getProgress());
            log.warn("Sinh lich trinh that bai cho trip ID={}: {}", job.getTripId(), e.getMessage());
        } catch (Exception e) {
            job.setError("INTERNAL_SERVER_ERROR");
            updateJob(job, "FAILED", "Không sinh được lịch trình", job.getProgress());
            log.error("Sinh lich trinh loi", e);
        }
    }

    /** Lấy địa điểm thật từ Google Places theo từng nhóm, xếp hạng rồi gán mã P1, P2... */
    private Map<String, PoolPlace> buildPool(TripEntity trip, Long conversationId, List<Long> executionIds) {
        int days = trip.lengthInDays();
        List<PoolQuery> queries = new ArrayList<>(List.of(
                new PoolQuery("ATTRACTION", "tham quan", "điểm tham quan nổi tiếng", Math.min(20, 3 * days + 3)),
                new PoolQuery("ATTRACTION", "tham quan", "địa điểm du lịch đẹp", 0),
                new PoolQuery("FOOD", "nhà hàng", PlaceTools.withDietaryKeywords("nhà hàng đặc sản ngon", trip.getGroupProfile()),
                        Math.min(12, 2 * days + 2)),
                new PoolQuery("CAFE", "cà phê", "quán cà phê đẹp", Math.min(8, days + 2)),
                new PoolQuery("LODGING", "lưu trú", "khách sạn tốt", 4)));
        trip.getPreferences().stream().limit(3)
                .forEach(pref -> queries.add(new PoolQuery("ATTRACTION", "tham quan", "địa điểm " + pref, 0)));
        Set<String> disliked = placeRatingLookup.dislikedExternalIds(trip.getUser().getId());
        String destination = trip.getDestination().getName();

        Map<String, Map<String, PlaceResponse>> byCategory = new LinkedHashMap<>();
        Map<String, PoolQuery> limits = new LinkedHashMap<>();
        for (PoolQuery q : queries) {
            limits.merge(q.category(), q, (a, b) -> a.limit() >= b.limit() ? a : b);
            long started = System.nanoTime();
            List<PlaceResponse> results;
            ToolExecutionStatus status = ToolExecutionStatus.OK;
            String error = null;
            try {
                results = placeService.searchPlaces(q.query(), destination);
            } catch (RuntimeException e) {
                results = List.of();
                status = ToolExecutionStatus.ERROR;
                error = e.getMessage();
            }
            Map<String, PlaceResponse> bucket = byCategory.computeIfAbsent(q.category(), k -> new LinkedHashMap<>());
            results.stream()
                    .filter(p -> p.getExternalId() != null && p.getRating() != null && !disliked.contains(p.getExternalId()))
                    .filter(p -> fitsCategory(p, q.category()))
                    .forEach(p -> bucket.putIfAbsent(p.getExternalId(), p));
            AiToolExecutionEntity saved = executionRepository.save(AiToolExecutionEntity.builder()
                    .conversationId(conversationId)
                    .tripId(trip.getId())
                    .userId(trip.getUser().getId())
                    .toolName("search_places")
                    .arguments(objectMapper.valueToTree(Map.of("query", q.query(), "category", q.category())))
                    .result(objectMapper.valueToTree(Map.of("candidates", results.stream().map(PlaceResponse::getName).toList())))
                    .status(status)
                    .errorMessage(error)
                    .executionTimeMs((int) ((System.nanoTime() - started) / 1_000_000))
                    .build());
            executionIds.add(saved.getId());
        }

        // Tra cứu địa điểm từ tri thức cộng đồng (Destination Knowledge / Dataset)
        Map<String, PlaceResponse> communityCandidates = new LinkedHashMap<>();
        try {
            List<DestinationKnowledgeEntity> activeKnowledge = destinationKnowledgeService.getActiveKnowledgeEntities(trip.getDestination());
            for (DestinationKnowledgeEntity k : activeKnowledge) {
                String pName = k.getPlaceName();
                if (pName != null && !pName.isBlank()) {
                    PlaceResponse matched = null;
                    try {
                        List<PlaceResponse> found = placeService.searchPlaces(pName.trim(), destination);
                        for (PlaceResponse p : found) {
                            if (p.getExternalId() != null && !disliked.contains(p.getExternalId())) {
                                if (nameMatch(pName, p.getName()) >= 0.3 || p.getName().toLowerCase(Locale.ROOT).contains(pName.toLowerCase(Locale.ROOT))) {
                                    matched = p;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (matched != null) {
                        communityCandidates.put(matched.getExternalId(), matched);
                    } else {
                        // Không có trên Google Places -> Vẫn tạo thực thể COMMUNITY Place với tọa độ điểm đến!
                        String commId = "comm-know-" + k.getId();
                        BigDecimal lat = trip.getDestination() != null && trip.getDestination().getLatitude() != null
                                ? trip.getDestination().getLatitude() : new BigDecimal("21.0285");
                        BigDecimal lng = trip.getDestination() != null && trip.getDestination().getLongitude() != null
                                ? trip.getDestination().getLongitude() : new BigDecimal("105.8542");
                        PlaceResponse synthetic = PlaceResponse.builder()
                                .provider("COMMUNITY")
                                .externalId(commId)
                                .name(pName.trim())
                                .category(k.getCategory() != null ? k.getCategory().name() : "COMMUNITY")
                                .latitude(lat)
                                .longitude(lng)
                                .rating(new BigDecimal("5.0"))
                                .userRatingsTotal(1)
                                .address(destination)
                                .build();
                        communityCandidates.put(commId, synthetic);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Loi khi tra cuu dia diem tu tri thuc cong dong cho trip ID={}: {}", trip.getId(), ex.getMessage());
        }

        Map<String, PoolPlace> pool = new LinkedHashMap<>();
        Set<String> taken = new HashSet<>();

        // 1. Ưu tiên đưa các địa điểm từ cộng đồng/dataset vào đầu danh sách pool với mã P1, P2...
        for (PlaceResponse cp : communityCandidates.values()) {
            taken.add(cp.getExternalId());
            String code = "P" + (pool.size() + 1);
            pool.put(code, new PoolPlace(code, "gợi ý cộng đồng", cp, 1000.0));
        }

        // 2. Tiếp tục đưa các địa điểm tìm được từ Google Places theo các nhóm
        for (Map.Entry<String, Map<String, PlaceResponse>> entry : byCategory.entrySet()) {
            PoolQuery q = limits.get(entry.getKey());
            entry.getValue().values().stream()
                    .filter(p -> !taken.contains(p.getExternalId()))
                    .sorted(Comparator.comparingDouble(ItineraryGenerationService::popularity).reversed())
                    .limit(q.limit())
                    .forEach(p -> {
                        taken.add(p.getExternalId());
                        String code = "P" + (pool.size() + 1);
                        pool.put(code, new PoolPlace(code, q.label(), p, popularity(p)));
                    });
        }
        log.info("Kho dia diem cho trip ID={}: {} dia diem (trong do {} tu dataset cong dong)",
                trip.getId(), pool.size(), communityCandidates.size());
        return pool;
    }

    static double popularity(PlaceResponse p) {
        double rating = p.getRating() == null ? 0 : p.getRating().doubleValue();
        int votes = p.getUserRatingsTotal() == null ? 0 : p.getUserRatingsTotal();
        return (votes * rating + PRIOR_VOTES * PRIOR_RATING) / (votes + PRIOR_VOTES);
    }

    private static boolean fitsCategory(PlaceResponse p, String category) {
        return switch (category) {
            case "FOOD" -> isRestaurantPlace(p) && !isLodgingPlace(p);
            case "CAFE" -> isCafeOrDrinkPlace(p);
            case "LODGING" -> isLodgingPlace(p);
            default -> !isRestaurantPlace(p) && !isCafeOrDrinkPlace(p) && !isLodgingPlace(p);
        };
    }

    private static String describePool(Map<String, PoolPlace> pool) {
        if (pool.isEmpty()) {
            return "(Không lấy được dữ liệu Google Places, hãy tự đề xuất địa điểm có thật.)";
        }
        return pool.values().stream()
                .map(pp -> {
                    PlaceResponse p = pp.place();
                    boolean isComm = "COMMUNITY".equalsIgnoreCase(p.getProvider()) || pp.category().contains("cộng đồng");
                    return pp.code() + " [" + pp.category() + "] " + p.getName()
                            + (isComm ? " (Kinh nghiệm thực tế từ cộng đồng du lịch)" : "")
                            + (p.getRating() != null && !isComm ? " — " + p.getRating() + "★, " + (p.getUserRatingsTotal() == null ? 0 : p.getUserRatingsTotal()) + " đánh giá" : "")
                            + (p.getPriceLevel() == null ? "" : ", mức giá " + p.getPriceLevel() + "/4")
                            + (p.getAddress() == null ? "" : ", " + p.getAddress());
                })
                .collect(Collectors.joining("\n"));
    }

    private Plan askModel(TripEntity trip, int maxPerDay, Map<String, PoolPlace> pool) {
        TripWeatherResponse weather = weatherService.forTrip(trip);
        String weatherText = weather.getDays().isEmpty() ? "Không có số liệu."
                : weather.getDays().stream()
                .map(d -> "Ngày " + d.getDayNumber() + ": " + d.getSummary() + ", " + d.getTempMin() + "–" + d.getTempMax()
                        + "°C, khả năng mưa " + d.getPrecipitationProbability() + "% (" + d.getSource() + ")")
                .collect(Collectors.joining("\n"));
        List<String> disliked = placeRatingLookup.dislikedNames(trip.getUser().getId());
        String destKnowledge = destinationKnowledgeService.getFormattedKnowledgeForPrompt(trip.getDestination());
        String system = template
                .replace("{tripSummary}", promptBuilder.tripSummary(trip))
                .replace("{groupSummary}", promptBuilder.groupSummary(trip))
                .replace("{weather}", weatherText)
                .replace("{places}", describePool(pool))
                .replace("{disliked}", disliked.isEmpty() ? "(không có)" : String.join(", ", disliked))
                .replace("{destinationKnowledge}", destKnowledge.isBlank() ? "(Chưa có mẹo cụ thể)" : destKnowledge)
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

    private List<ProposalChange> verify(TripEntity trip, Plan plan, Map<String, PoolPlace> pool, int maxPerDay, Long conversationId,
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
                PoolPlace pooled = item.placeId() == null ? null : pool.get(item.placeId().trim().toUpperCase());
                if (pooled != null && used.contains(pooled.place().getExternalId())) {
                    pooled = null;
                }
                PlaceRef place = null;
                if (pooled != null) {
                    PlaceResponse p = pooled.place();
                    used.add(p.getExternalId());
                    place = new PlaceRef(p.getProvider(), p.getExternalId(), p.getName(), p.getLatitude(), p.getLongitude(),
                            p.getPriceLevel());
                } else if (query != null || (item.placeName() != null && !item.placeName().isBlank())) {
                    Optional<PlaceResponse> match = findReal(trip, query, item.placeName(), type, item.title(), item.notes(),
                            disliked, used, conversationId, executionIds);
                    if (match.isPresent()) {
                        PlaceResponse p = match.get();
                        used.add(p.getExternalId());
                        place = new PlaceRef(p.getProvider(), p.getExternalId(), p.getName(), p.getLatitude(), p.getLongitude(),
                                p.getPriceLevel());
                    } else {
                        // HYBRID AGENTIC: Không tìm thấy trên Google Places -> Tự động chuyển thành Trải nghiệm Cộng đồng!
                        String placeName = firstNonBlank(item.placeName(), item.title());
                        BigDecimal lat = trip.getDestination() != null && trip.getDestination().getLatitude() != null
                                ? trip.getDestination().getLatitude() : new BigDecimal("21.0285");
                        BigDecimal lng = trip.getDestination() != null && trip.getDestination().getLongitude() != null
                                ? trip.getDestination().getLongitude() : new BigDecimal("105.8542");
                        String commExtId = "comm-" + Math.abs((trip.getDestination().getName() + "_" + placeName).hashCode());
                        place = new PlaceRef("COMMUNITY", commExtId, placeName, lat, lng, null);
                        used.add(commExtId);
                    }
                } else {
                    if (!PLACELESS.contains(type) && type != ActivityType.SIGHTSEEING) {
                        rejected.add(new Rejected(day.dayNumber(), item.title(), null, "NO_PLACE"));
                        continue;
                    }
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
                if (place != null && !activityTitle.toLowerCase().contains(place.name().toLowerCase()) && !"COMMUNITY".equalsIgnoreCase(place.provider())) {
                    activityTitle = activityTitle + " (" + place.name() + ")";
                }
                String reasonStr = place == null ? null : pooled != null
                        ? (pooled.category().contains("cộng đồng")
                                ? "Gợi ý từ cộng đồng: " + pooled.place().getName()
                                : "Google Places: " + pooled.place().getRating() + "★, " + pooled.place().getUserRatingsTotal() + " đánh giá")
                        : ("COMMUNITY".equalsIgnoreCase(place.provider())
                                ? "Trải nghiệm cộng đồng / Địa phương: " + place.name()
                                : "Khớp địa điểm thật: " + place.name());
                changes.add(new ProposalChange.Add(day.dayNumber(), activityTitle, type, place, start,
                        end, cost, costSource, item.notes(), reasonStr));
                kept++;
            }
        }
        return changes;
    }

    private static final Set<String> CAFE_TAGS = Set.of(
            "cafe", "coffee_shop", "tea_house", "bubble_tea_store", "juice_shop", "bar", "pub", "wine_bar",
            "performing_arts_theater", "event_venue", "night_club", "entertainment"
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
                || text.contains("pub") || text.contains("show") || text.contains("nhạc")
                || text.contains("ca nhạc") || text.contains("âm nhạc") || text.contains("hát")
                || text.contains("lala");
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
