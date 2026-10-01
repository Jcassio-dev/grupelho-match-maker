package com.grupelho.store;

import com.grupelho.domain.GameMode;
import com.grupelho.domain.Match;
import com.grupelho.domain.MatchStatus;
import com.grupelho.domain.Player;
import com.grupelho.domain.Result;
import com.grupelho.domain.StandingEntry;

import java.util.List;
import java.util.Optional;

public interface Repository {

    void savePlayer(Player player);

    Optional<Player> findPlayerByDiscordUserId(String discordUserId);

    Optional<Player> findPlayerByPuuid(String puuid);

    String createMatch(GameMode mode, String tournamentCode, List<String> teamA, List<String> teamB);

    Optional<Match> findMatchByInternalId(String internalId);

    void updateMatchStatus(String internalId, MatchStatus status);

    void saveResult(Result result);

    Optional<Result> findResultByMatchId(String internalId);

    void confirmResult(String internalId, String confirmedByDiscordId);

    double getRating(GameMode mode, String discordUserId, double defaultRating);

    void setRating(GameMode mode, String discordUserId, double rating);

    List<StandingEntry> listStandings(GameMode mode);
}
