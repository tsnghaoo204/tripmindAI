package com.tripmind.services.ai;

import com.tripmind.domains.proposals.ProposalChange.PlaceRef;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.services.PlaceAdoptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Xác minh một địa điểm mô hình nhắc tới là có thật (QĐ-08, BR-509): đã có trong CSDL, hoặc
 * vừa được công cụ tìm kiếm trả về trong lượt này và còn trong bộ đệm ứng viên. Mã do mô hình
 * tự nghĩ ra không qua được bước này.
 */
@Component
@RequiredArgsConstructor
public class PlaceVerifier {

    private final PlaceRepository placeRepository;
    private final PlaceAdoptionService placeAdoptionService;

    public Optional<PlaceRef> verify(String externalId, AgentTurn turn) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        Optional<PlaceRef> inDb = placeRepository.findFirstByExternalId(externalId).map(PlaceVerifier::ref);
        if (inDb.isPresent()) {
            return inDb;
        }
        if (turn != null && !turn.getSeenPlaceIds().contains(externalId)) {
            return Optional.empty();
        }
        return placeAdoptionService.findCandidate(externalId)
                .map(gp -> new PlaceRef(PlaceAdoptionService.PROVIDER_GOOGLE, gp.placeId(), gp.name(), gp.latitude(),
                        gp.longitude(), gp.priceLevel()));
    }

    public static PlaceRef ref(PlaceEntity p) {
        return new PlaceRef(p.getProvider(), p.getExternalId(), p.getName(), p.getLatitude(), p.getLongitude(),
                p.getPriceLevel() == null ? null : p.getPriceLevel().intValue());
    }
}
