package com.grupelho.riot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupelho.riot.dto.ProviderRequest;
import com.grupelho.riot.dto.TournamentCodeParams;
import com.grupelho.riot.dto.TournamentRequest;

import java.util.List;

/**
 * USE_STUB troca so a base do caminho (stub vs real) e nada mais.
 */
public class TournamentApi {

    private final RiotHttpClient client;
    private final String regionalHost;
    private final boolean useStub;
    private final ObjectMapper mapper = new ObjectMapper();

    public TournamentApi(RiotHttpClient client, String regionalHost, boolean useStub) {
        this.client = client;
        this.regionalHost = regionalHost;
        this.useStub = useStub;
    }

    private String base() {
        return useStub ? "/lol/tournament-stub/v5" : "/lol/tournament/v5";
    }

    public long createProvider(String region, String callbackUrl) {
        try {
            String body = mapper.writeValueAsString(new ProviderRequest(region, callbackUrl));
            String response = client.post(regionalHost + base() + "/providers", body);
            return Long.parseLong(response.trim());
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao criar provider de torneio", e);
        }
    }

    public long createTournament(long providerId, String name) {
        try {
            String body = mapper.writeValueAsString(new TournamentRequest(providerId, name));
            String response = client.post(regionalHost + base() + "/tournaments", body);
            return Long.parseLong(response.trim());
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao criar torneio", e);
        }
    }

    public List<String> createCodes(long tournamentId, int count, TournamentCodeParams params) {
        try {
            String body = mapper.writeValueAsString(params);
            String url = regionalHost + base() + "/codes?tournamentId=" + tournamentId + "&count=" + count;
            String response = client.post(url, body);
            return mapper.readValue(response, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao gerar codigos de torneio", e);
        }
    }

    public String getCode(String tournamentCode) {
        return client.get(regionalHost + base() + "/codes/" + tournamentCode);
    }

    public String getLobbyEvents(String tournamentCode) {
        return client.get(regionalHost + base() + "/lobby-events/by-code/" + tournamentCode);
    }

    /**
     * Existe so na Tournament-V5 real, nao na stub.
     */
    public String getGameByCode(String tournamentCode) {
        return client.get(regionalHost + "/lol/tournament/v5/games/by-code/" + tournamentCode);
    }
}
