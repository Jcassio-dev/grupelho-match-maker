package com.grupelho.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchDto(MatchInfo info) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchInfo(List<ParticipantDto> participants) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParticipantDto(String puuid, boolean win) {
    }
}
