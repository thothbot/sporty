package com.sporty.jackpot.repository;

import com.sporty.jackpot.entity.JackpotContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JackpotContributionRepository extends JpaRepository<JackpotContribution, UUID> {

    List<JackpotContribution> findByBetId(UUID betId);

    List<JackpotContribution> findByUserId(UUID userId);

    List<JackpotContribution> findByJackpotId(UUID jackpotId);
}
