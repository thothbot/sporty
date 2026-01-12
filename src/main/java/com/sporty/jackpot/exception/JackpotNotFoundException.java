package com.sporty.jackpot.exception;

import java.util.UUID;

public class JackpotNotFoundException extends RuntimeException {

    public JackpotNotFoundException(UUID jackpotId) {
        super("Jackpot not found: " + jackpotId);
    }

    public JackpotNotFoundException(String message) {
        super(message);
    }
}
