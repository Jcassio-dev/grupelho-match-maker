package com.grupelho.riot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupelho.riot.dto.MatchDto;

public class MatchApi {

    private final RiotHttpClient client;
    private final String regionalHost;
    private final ObjectMapper mapper = new ObjectMapper();

    public MatchApi(RiotHttpClient client, String regionalHost) {
        this.client = client;
        this.regionalHost = regionalHost;
    }

    public MatchDto getMatch(String matchId) {
        try {
            String response = client.get(regionalHost + "/lol/match/v5/matches/" + matchId);
            return mapper.readValue(response, MatchDto.class);
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao buscar partida " + matchId, e);
        }
    }

    public String getTimeline(String matchId) {
        return client.get(regionalHost + "/lol/match/v5/matches/" + matchId + "/timeline");
    }
}
