package com.grupelho.riot.dto;

import java.util.List;

public record TournamentCodeParams(
        String mapType,
        String pickType,
        String spectatorType,
        int teamSize,
        boolean enoughPlayers,
        String metadata,
        List<String> allowedParticipants
) {
}
