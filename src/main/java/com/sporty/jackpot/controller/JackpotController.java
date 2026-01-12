package com.sporty.jackpot.controller;

import com.sporty.jackpot.dto.JackpotDto;
import com.sporty.jackpot.dto.JackpotRequest;
import com.sporty.jackpot.service.JackpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jackpots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jackpots", description = "Jackpot configuration management")
public class JackpotController {

    private final JackpotService jackpotService;

    @Operation(summary = "Create a jackpot",
            description = "Creates a new jackpot with contribution and reward configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Jackpot created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping
    public ResponseEntity<JackpotDto> createJackpot(@Valid @RequestBody JackpotRequest request) {
        log.info("Creating jackpot with name '{}'", request.getName());
        JackpotDto created = jackpotService.createJackpot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get jackpot", description = "Retrieves a jackpot by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jackpot found"),
        @ApiResponse(responseCode = "404", description = "Jackpot not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<JackpotDto> getJackpot(
            @Parameter(description = "Jackpot ID") @PathVariable UUID id) {
        log.debug("Getting jackpot {}", id);
        JackpotDto jackpot = jackpotService.getJackpot(id);
        return ResponseEntity.ok(jackpot);
    }

    @Operation(summary = "List all jackpots", description = "Retrieves all configured jackpots")
    @ApiResponse(responseCode = "200", description = "List of jackpots")
    @GetMapping
    public ResponseEntity<List<JackpotDto>> getAllJackpots() {
        log.debug("Getting all jackpots");
        List<JackpotDto> jackpots = jackpotService.getAllJackpots();
        return ResponseEntity.ok(jackpots);
    }
}
