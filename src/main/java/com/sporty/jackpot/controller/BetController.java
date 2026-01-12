package com.sporty.jackpot.controller;

import com.sporty.jackpot.dto.BetRequest;
import com.sporty.jackpot.dto.BetResponse;
import com.sporty.jackpot.entity.Bet;
import com.sporty.jackpot.entity.BetStatus;
import com.sporty.jackpot.kafka.KafkaBetProducer;
import com.sporty.jackpot.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bets", description = "Bet publishing and status tracking")
public class BetController {

    private final KafkaBetProducer kafkaBetProducer;
    private final BetService betService;

    @Operation(summary = "Publish a bet", description = "Publishes a bet to Kafka for async processing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bet published successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping
    public ResponseEntity<BetResponse> publishBet(@Valid @RequestBody BetRequest request) {
        UUID betId = UUID.randomUUID();

        Bet bet = betService.createBet(betId, request);

        kafkaBetProducer.publishBetSync(betService.toMessage(bet));

        betService.updateStatus(betId, BetStatus.PUBLISHED);

        log.info("Published bet {} for jackpot {}", betId, request.getJackpotId());

        BetResponse response = BetResponse.builder()
                .betId(betId)
                .status("PUBLISHED")
                .message("Bet successfully published to processing queue")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get bet status", description = "Retrieves the current status of a bet")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bet found"),
        @ApiResponse(responseCode = "404", description = "Bet not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BetResponse> getBet(
            @Parameter(description = "Bet ID") @PathVariable UUID id) {
        Bet bet = betService.getBet(id);

        BetResponse response = BetResponse.builder()
                .betId(bet.getId())
                .status(bet.getStatus().name())
                .message("Bet status: " + bet.getStatus().name())
                .build();

        return ResponseEntity.ok(response);
    }
}
