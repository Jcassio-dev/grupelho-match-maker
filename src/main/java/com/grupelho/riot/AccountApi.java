package com.grupelho.riot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupelho.riot.dto.AccountDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AccountApi {

    private final RiotHttpClient client;
    private final String regionalHost;
    private final ObjectMapper mapper = new ObjectMapper();

    public AccountApi(RiotHttpClient client, String regionalHost) {
        this.client = client;
        this.regionalHost = regionalHost;
    }

    public AccountDto getByRiotId(String gameName, String tagLine) {
        try {
            String url = regionalHost + "/riot/account/v1/accounts/by-riot-id/"
                    + URLEncoder.encode(gameName, StandardCharsets.UTF_8) + "/"
                    + URLEncoder.encode(tagLine, StandardCharsets.UTF_8);
            String response = client.get(url);
            return mapper.readValue(response, AccountDto.class);
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao resolver riot id " + gameName + "#" + tagLine, e);
        }
    }

    public AccountDto getByPuuid(String puuid) {
        try {
            String response = client.get(regionalHost + "/riot/account/v1/accounts/by-puuid/" + puuid);
            return mapper.readValue(response, AccountDto.class);
        } catch (RiotApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RiotApiException("Falha ao buscar conta por puuid " + puuid, e);
        }
    }
}
