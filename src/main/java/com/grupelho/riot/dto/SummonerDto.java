package com.grupelho.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SummonerDto(
        String id,
        String accountId,
        String puuid,
        Integer profileIconId,
        Long revisionDate,
        Integer summonerLevel
) {
}
