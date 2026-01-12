package com.sporty.jackpot.exception;

import java.util.UUID;

public class ContributionNotFoundException extends RuntimeException {

    public ContributionNotFoundException(UUID betId) {
        super("Contribution not found for bet: " + betId);
    }

    public ContributionNotFoundException(String message) {
        super(message);
    }
}
