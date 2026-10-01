package com.grupelho.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupelho.domain.Match;
import com.grupelho.domain.MatchStatus;
import com.grupelho.domain.Player;
import com.grupelho.domain.Result;
import com.grupelho.domain.Side;
import com.grupelho.rating.RatingApplier;
import com.grupelho.riot.MatchApi;
import com.grupelho.riot.dto.MatchDto;
import com.grupelho.store.Repository;
import io.javalin.Javalin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Recebe o callback do Tournament-V5 quando a production key existe. No modo
 * stub o Tournament-Stub-V5 nunca chama essa URL, entao esse servidor fica
 * no ar mas inativo na pratica ate a production key sair.
 */
public class CallbackServer {

    private final Repository repository;
    private final MatchApi matchApi;
    private final RatingApplier ratingApplier;
    private final ObjectMapper mapper = new ObjectMapper();

    public CallbackServer(Repository repository, MatchApi matchApi, RatingApplier ratingApplier) {
        this.repository = repository;
        this.matchApi = matchApi;
        this.ratingApplier = ratingApplier;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create().start(port);
        app.post("/riot/callback", ctx -> {
            CallbackPayload payload = mapper.readValue(ctx.body(), CallbackPayload.class);
            handleCallback(payload);
            ctx.status(204);
        });
        return app;
    }

    private void handleCallback(CallbackPayload payload) {
        String internalMatchId = extractInternalMatchId(payload.metaData());
        if (internalMatchId == null) {
            return;
        }

        Optional<Match> matchOpt = repository.findMatchByInternalId(internalMatchId);
        if (matchOpt.isEmpty()) {
            return;
        }
        Match match = matchOpt.get();

        MatchDto matchDto = matchApi.getMatch(payload.region() + "_" + payload.gameId());
        Side winner = determineWinner(match, matchDto);

        repository.saveResult(new Result(internalMatchId, winner, "riot-callback", "riot-callback", Instant.now()));
        repository.updateMatchStatus(internalMatchId, MatchStatus.CONFIRMED);
        ratingApplier.apply(match.mode(), match.teamA(), match.teamB(), winner == Side.A);
    }

    private Side determineWinner(Match match, MatchDto matchDto) {
        List<String> teamAPuuids = resolvePuuids(match.teamA());
        for (MatchDto.ParticipantDto participant : matchDto.info().participants()) {
            if (participant.win() && teamAPuuids.contains(participant.puuid())) {
                return Side.A;
            }
        }
        return Side.B;
    }

    private List<String> resolvePuuids(List<String> discordIds) {
        return discordIds.stream()
                .map(repository::findPlayerByDiscordUserId)
                .flatMap(Optional::stream)
                .map(Player::puuid)
                .toList();
    }

    private String extractInternalMatchId(String metaData) {
        try {
            var node = mapper.readTree(metaData);
            return node.has("internalMatchId") ? node.get("internalMatchId").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
