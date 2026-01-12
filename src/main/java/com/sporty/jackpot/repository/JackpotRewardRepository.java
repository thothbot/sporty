package com.sporty.jackpot.repository;

import com.sporty.jackpot.entity.JackpotReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JackpotRewardRepository extends JpaRepository<JackpotReward, UUID> {

    Optional<JackpotReward> findByBetId(UUID betId);

    List<JackpotReward> findByUserId(UUID userId);
}
