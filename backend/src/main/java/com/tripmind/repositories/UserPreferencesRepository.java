package com.tripmind.repositories;

import com.tripmind.entities.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, Long> {

    Optional<UserPreferencesEntity> findByUserId(Long userId);
}
