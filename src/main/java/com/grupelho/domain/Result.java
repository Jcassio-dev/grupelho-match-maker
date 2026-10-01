package com.grupelho.domain;

import java.time.Instant;

public record Result(
        String internalMatchId,
        Side winner,
        String reportedByDiscordId,
        String confirmedByDiscordId,
        Instant reportedAt
) {
}
