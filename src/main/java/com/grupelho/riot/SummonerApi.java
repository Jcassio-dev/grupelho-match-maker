package com.grupelho.riot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupelho.riot.dto.SummonerDto;

public class SummonerApi {

    private final RiotHttpClient client;
    private final String platformHost;
    private final ObjectMapper mapper = new ObjectMapper();

    public SummonerApi(RiotHttpClient client, String platformHost) {
        this.client = client;
        this.platformHost = platformHost;
    }

    public SummonerDto getByPuuid(String puuid) {
        try {
            String response = client.get(platformHost + "/lol/summoner/v4/summoners/by-puuid/" + puuid);
            return mapper.readValue(response, SummonerDto.class);
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao buscar summoner por puuid " + puuid, e);
        }
    }
}
