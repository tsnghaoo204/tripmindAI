package com.tripmind.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripmind.domains.requests.DestinationInput;
import com.tripmind.domains.responses.DestinationResponse;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.DestinationRepository;
import com.tripmind.services.clients.GooglePlacesClient;
import com.tripmind.services.clients.GooglePlacesClient.GooglePlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Điểm đến trên toàn cầu, nạp theo hành động của người dùng.
 *
 * <p>Bảng {@code destinations} không phải một danh mục đóng. Mười lăm dòng gieo sẵn trong
 * {@code schemas.sql} chỉ là gợi ý; mọi thành phố khác vào bảng đúng lúc người dùng chọn
 * nó, qua {@link #getOrCreateDestination}. Cùng nguyên tắc với bảng {@code places} —
 * một lượt tìm kiếm KHÔNG sinh dòng nào, chỉ một lượt chọn mới sinh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestinationService {

    private static final String PROVIDER_MANUAL = "MANUAL";
    private static final String PROVIDER_GOOGLE = "GOOGLE";

    /** Phải khớp {@code chk_destinations_provider} trong schemas.sql. */
    private static final Set<String> PROVIDERS = Set.of(PROVIDER_MANUAL, PROVIDER_GOOGLE, "MAPBOX");

    private static final int NAME_MAX = 160;
    private static final int COUNTRY_MAX = 80;
    private static final int TIMEZONE_MAX = 64;

    private final DestinationRepository destinationRepository;
    private final GooglePlacesClient googlePlacesClient;
    private final ObjectMapper objectMapper;

    /**
     * Gợi ý điểm đến cho bước 1 của màn tạo chuyến: những dòng đã có trong cơ sở dữ liệu
     * trước, rồi tới kết quả Google Places cho những nơi chưa từng ai đi.
     *
     * <p>Google hỏng hoặc chưa cấu hình khoá thì vẫn trả về phần catalog nội bộ — người
     * dùng không bị chặn ở màn tạo chuyến chỉ vì một dịch vụ bên ngoài chập chờn.
     */
    @Transactional(readOnly = true)
    public List<DestinationResponse> searchDestinations(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Map<String, DestinationResponse> results = new LinkedHashMap<>();
        Set<String> localNames = new HashSet<>();
        List<DestinationEntity> local = destinationRepository.findByNameContainingIgnoreCase(query.trim());
        for (DestinationEntity entity : local) {
            results.put(dedupeKey(entity.getProvider(), entity.getExternalId(), entity.getCountry(), entity.getName()),
                    toResponse(entity));
            localNames.add(nameKey(entity.getCountry(), entity.getName()));
        }

        try {
            for (GooglePlace place : googlePlacesClient.searchText(query, null)) {
                if (!place.isResolvable() || !place.isLocality()) {
                    continue;
                }
                // Google trả về đúng thành phố đã có trong bảng (thường là một dòng gieo
                // sẵn): giữ dòng nội bộ, đừng hiện "Da Nang" hai lần cạnh nhau.
                if (localNames.contains(nameKey(place.country(), place.name()))) {
                    continue;
                }
                String key = dedupeKey(PROVIDER_GOOGLE, place.placeId(), place.country(), place.name());
                results.putIfAbsent(key, toResponse(place));
            }
        } catch (AppException e) {
            log.warn("Google Places khong tra ve goi y cho '{}': {}. Chi dung catalog noi bo.",
                    query, e.getMessage());
        }

        return new ArrayList<>(results.values());
    }

    /**
     * Phân giải điểm đến của một chuyến đi thành một dòng trong {@code destinations},
     * tạo mới nếu chưa có. Ba đường vào, xét theo thứ tự:
     *
     * <ol>
     *   <li>{@code destinationId} — người dùng chọn một điểm đến đã có;</li>
     *   <li>{@code placeId} — chọn một kết quả Google, máy chủ tự gọi Place Details;</li>
     *   <li>{@code input} — client gửi thẳng dữ liệu đã có từ lượt tìm kiếm.</li>
     * </ol>
     */
    @Transactional
    public DestinationEntity getOrCreateDestination(Long destinationId, String placeId, DestinationInput input) {
        if (destinationId != null) {
            return destinationRepository.findById(destinationId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Destination " + destinationId + " khong ton tai"));
        }
        if (placeId != null && !placeId.isBlank()) {
            return resolveByPlaceId(placeId);
        }
        if (input != null) {
            return resolveByInput(input);
        }
        throw new AppException(ErrorCode.VALIDATION_ERROR,
                "Phai co mot trong ba: destinationId, destinationPlaceId hoac destinationData");
    }

    /**
     * Nạp một điểm đến từ Google {@code place_id}. Đã có trong bảng thì dùng lại, chưa có
     * thì gọi Place Details rồi ghi xuống.
     */
    @Transactional
    public DestinationEntity resolveByPlaceId(String placeId) {
        Optional<DestinationEntity> existing =
                destinationRepository.findByProviderAndExternalId(PROVIDER_GOOGLE, placeId);
        if (existing.isPresent()) {
            return existing.get();
        }

        GooglePlace place = googlePlacesClient.getPlaceDetails(placeId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Google Places khong tim thay place_id " + placeId));
        if (!place.isResolvable()) {
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Google Places tra ve thieu du lieu cho place_id " + placeId);
        }

        String country = trim(requireCountry(place.country(), place.countryCode(), place.name()), COUNTRY_MAX);
        ObjectNode metadata =
                metadataOf(place.placeId(), place.formattedAddress(), place.adminArea(), place.countryCode());

        Optional<DestinationEntity> adopted = adoptSeeded(country, place.name(), place, metadata);
        if (adopted.isPresent()) {
            return adopted.get();
        }

        return save(DestinationEntity.builder()
                .provider(PROVIDER_GOOGLE)
                .externalId(place.placeId())
                .name(trim(place.name(), NAME_MAX))
                .country(country)
                .latitude(place.latitude())
                .longitude(place.longitude())
                .timezone(resolveTimezone(null, place.latitude(), place.longitude(), place.name()))
                .metadata(metadata)
                .build(),
                PROVIDER_GOOGLE, place.placeId(), country, place.name());
    }

    /**
     * Cùng một thành phố đã nằm sẵn trong bảng dưới dạng dòng gieo sẵn ({@code external_id}
     * NULL) thì gắn mã Google vào chính dòng đó, KHÔNG tạo dòng thứ hai. Ràng buộc cơ sở
     * dữ liệu cho phép hai dòng cùng (nước, tên) khi một dòng có {@code external_id} —
     * cần thế để không chặn oan các thành phố trùng tên — nên chỗ gộp là ở đây.
     *
     * <p>Giữ nguyên {@code id} nghĩa là mọi chuyến đi đang trỏ tới dòng gieo sẵn vẫn đúng.
     * Giữ nguyên cả {@code timezone} đã biết: khỏi tốn một lượt gọi Time Zone API.
     */
    private Optional<DestinationEntity> adoptSeeded(String country, String name,
                                                    GooglePlace place, ObjectNode metadata) {
        Optional<DestinationEntity> seeded = destinationRepository
                .findByCountryIgnoreCaseAndNameIgnoreCaseAndExternalIdIsNull(country, name);
        if (seeded.isEmpty()) {
            return Optional.empty();
        }

        DestinationEntity destination = seeded.get();
        log.info("Gan place_id {} vao diem den gieo san #{} ('{}') thay vi tao dong moi",
                place.placeId(), destination.getId(), destination.getName());
        destination.setProvider(PROVIDER_GOOGLE);
        destination.setExternalId(place.placeId());
        destination.setLatitude(place.latitude());
        destination.setLongitude(place.longitude());
        destination.setMetadata(metadata);
        return Optional.of(destinationRepository.saveAndFlush(destination));
    }

    private DestinationEntity resolveByInput(DestinationInput input) {
        String provider = input.getProvider() == null || input.getProvider().isBlank()
                ? PROVIDER_MANUAL
                : input.getProvider().toUpperCase();
        if (!PROVIDERS.contains(provider)) {
            // Chặn ở đây để trả 400 kèm lý do, thay vì để ràng buộc CHECK ném ra 500.
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "provider phai la mot trong " + PROVIDERS + ", nhan duoc '" + provider + "'");
        }
        String externalId = input.getExternalId() == null || input.getExternalId().isBlank()
                ? null
                : input.getExternalId();

        // Bản ghi tự nhập không có external_id nên rơi vào khoá dự phòng (country, name).
        if (externalId == null) {
            provider = PROVIDER_MANUAL;
            Optional<DestinationEntity> existing = destinationRepository
                    .findByCountryIgnoreCaseAndNameIgnoreCaseAndExternalIdIsNull(input.getCountry(), input.getName());
            if (existing.isPresent()) {
                return existing.get();
            }
        } else {
            Optional<DestinationEntity> existing =
                    destinationRepository.findByProviderAndExternalId(provider, externalId);
            if (existing.isPresent()) {
                return existing.get();
            }
            Optional<DestinationEntity> seeded = destinationRepository
                    .findByCountryIgnoreCaseAndNameIgnoreCaseAndExternalIdIsNull(input.getCountry(), input.getName());
            if (seeded.isPresent()) {
                DestinationEntity destination = seeded.get();
                destination.setProvider(provider);
                destination.setExternalId(externalId);
                destination.setLatitude(input.getLatitude());
                destination.setLongitude(input.getLongitude());
                return destinationRepository.saveAndFlush(destination);
            }
        }

        return save(DestinationEntity.builder()
                .provider(provider)
                .externalId(externalId)
                .name(trim(input.getName(), NAME_MAX))
                .country(trim(input.getCountry(), COUNTRY_MAX))
                .latitude(input.getLatitude())
                .longitude(input.getLongitude())
                .timezone(resolveTimezone(input.getTimezone(), input.getLatitude(), input.getLongitude(), input.getName()))
                .metadata(metadataOf(externalId, input.getFormattedAddress(), null, null))
                .build(),
                provider, externalId, input.getCountry(), input.getName());
    }

    /**
     * Ghi một điểm đến mới. Hai yêu cầu song song cho cùng một thành phố thì yêu cầu thua
     * đụng chỉ mục duy nhất — bắt lại và đọc lại dòng của yêu cầu thắng thay vì báo lỗi
     * ra người dùng.
     */
    private DestinationEntity save(DestinationEntity destination,
                                   String provider, String externalId, String country, String name) {
        try {
            return destinationRepository.saveAndFlush(destination);
        } catch (DataIntegrityViolationException e) {
            log.debug("Diem den '{}' vua duoc mot yeu cau khac ghi truoc, dung lai dong do", name);
            Optional<DestinationEntity> winner = externalId == null
                    ? destinationRepository.findByCountryIgnoreCaseAndNameIgnoreCaseAndExternalIdIsNull(country, name)
                    : destinationRepository.findByProviderAndExternalId(provider, externalId);
            return winner.orElseThrow(() -> e);
        }
    }

    /**
     * Múi giờ IANA của điểm đến. Ưu tiên giá trị client gửi lên, sau đó hỏi Time Zone API
     * theo toạ độ.
     *
     * <p>Không có giá trị nào thì dừng hẳn chứ không lấy tạm {@code UTC}: cột này là đầu
     * vào của lịch trình, thời tiết và giờ mở cửa, đoán sai một múi giờ làm sai cả chuyến.
     */
    private String resolveTimezone(String provided, BigDecimal latitude, BigDecimal longitude, String name) {
        if (provided != null && !provided.isBlank()) {
            return trim(provided, TIMEZONE_MAX);
        }
        return googlePlacesClient.resolveTimezone(latitude, longitude)
                .map(timezone -> trim(timezone, TIMEZONE_MAX))
                .orElseThrow(() -> new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "Khong xac dinh duoc mui gio cho '" + name + "'. Bat Time Zone API tren khoa Google,"
                                + " hoac gui kem truong timezone trong destinationData."));
    }

    private String requireCountry(String country, String countryCode, String name) {
        if (country != null && !country.isBlank()) {
            return country;
        }
        if (countryCode != null && !countryCode.isBlank()) {
            return countryCode;
        }
        throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Google Places khong tra ve quoc gia cho '" + name + "'");
    }

    private ObjectNode metadataOf(String externalId, String formattedAddress, String adminArea, String countryCode) {
        ObjectNode metadata = objectMapper.createObjectNode();
        if (externalId != null) {
            metadata.put("placeId", externalId);
        }
        if (formattedAddress != null) {
            metadata.put("address", formattedAddress);
        }
        if (adminArea != null) {
            metadata.put("adminArea", adminArea);
        }
        if (countryCode != null) {
            metadata.put("countryCode", countryCode);
        }
        return metadata;
    }

    /**
     * Khoá gộp trùng giữa dòng nội bộ và kết quả Google: cùng {@code place_id} là một,
     * còn dòng tự nhập thì so theo (nước, tên).
     */
    private String dedupeKey(String provider, String externalId, String country, String name) {
        if (externalId != null && !externalId.isBlank()) {
            return provider + ":" + externalId;
        }
        return nameKey(country, name);
    }

    private String nameKey(String country, String name) {
        return "name:" + lower(country) + ":" + lower(name);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private DestinationResponse toResponse(DestinationEntity entity) {
        return DestinationResponse.builder()
                .id(entity.getId())
                .provider(entity.getProvider())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .country(entity.getCountry())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .timezone(entity.getTimezone())
                .metadata(entity.getMetadata())
                .saved(true)
                .build();
    }

    private DestinationResponse toResponse(GooglePlace place) {
        return DestinationResponse.builder()
                .provider(PROVIDER_GOOGLE)
                .externalId(place.placeId())
                .name(place.name())
                .country(place.country() != null ? place.country() : place.countryCode())
                .latitude(place.latitude())
                .longitude(place.longitude())
                .formattedAddress(place.formattedAddress())
                .saved(false)
                .build();
    }
}
