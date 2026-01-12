package com.sporty.jackpot.exception;

import java.util.UUID;

public class BetNotFoundException extends RuntimeException {

    public BetNotFoundException(UUID betId) {
        super("Bet not found: " + betId);
    }
}
