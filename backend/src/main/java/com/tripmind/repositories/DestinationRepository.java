package com.tripmind.repositories;

import com.tripmind.entities.DestinationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<DestinationEntity, Long> {

    List<DestinationEntity> findByCountryOrderByNameAsc(String country);

    /**
     * Khoá trùng lặp của điểm đến lấy từ nhà cung cấp — dùng khi phân giải/nạp.
     */
    Optional<DestinationEntity> findByProviderAndExternalId(String provider, String externalId);

    /**
     * Khoá dự phòng cho bản ghi tự nhập/gieo sẵn ({@code external_id} NULL).
     */
    Optional<DestinationEntity> findByCountryIgnoreCaseAndNameIgnoreCaseAndExternalIdIsNull(String country, String name);

    List<DestinationEntity> findByNameContainingIgnoreCase(String keyword);
}
