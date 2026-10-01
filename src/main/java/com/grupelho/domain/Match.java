package com.grupelho.domain;

import java.time.Instant;
import java.util.List;

public record Match(
        String internalId,
        GameMode mode,
        String tournamentCode,
        List<String> teamA,
        List<String> teamB,
        MatchStatus status,
        Instant createdAt
) {
}
