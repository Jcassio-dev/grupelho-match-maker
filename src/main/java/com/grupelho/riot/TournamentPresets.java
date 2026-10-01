package com.grupelho.riot;

import com.grupelho.riot.dto.TournamentCodeParams;

import java.util.List;

public final class TournamentPresets {

    private TournamentPresets() {
    }

    public static TournamentCodeParams threeVThree(String metadata, List<String> allowedParticipants) {
        return new TournamentCodeParams("SUMMONERS_RIFT", "TOURNAMENT_DRAFT", "ALL", 3, true, metadata, allowedParticipants);
    }

    public static TournamentCodeParams fourVFour(String metadata, List<String> allowedParticipants) {
        return new TournamentCodeParams("SUMMONERS_RIFT", "TOURNAMENT_DRAFT", "ALL", 4, true, metadata, allowedParticipants);
    }

    public static TournamentCodeParams aram(int teamSize, String metadata, List<String> allowedParticipants) {
        return new TournamentCodeParams("HOWLING_ABYSS", "ALL_RANDOM", "ALL", teamSize, true, metadata, allowedParticipants);
    }
}
