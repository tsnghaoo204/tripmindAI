package com.tripmind.repositories;

import com.tripmind.entities.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<PlaceEntity, Long> {

    Optional<PlaceEntity> findByProviderAndExternalId(String provider, String externalId);
}
