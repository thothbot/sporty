package com.sporty.jackpot.repository;

import com.sporty.jackpot.entity.Bet;
import com.sporty.jackpot.entity.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BetRepository extends JpaRepository<Bet, UUID> {

    List<Bet> findByUserId(UUID userId);

    List<Bet> findByJackpotId(UUID jackpotId);

    List<Bet> findByStatus(BetStatus status);
}
