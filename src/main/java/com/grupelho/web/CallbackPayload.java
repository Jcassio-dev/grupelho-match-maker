package com.grupelho.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo do callback da Riot quando uma partida de um codigo de torneio
 * termina. So dispara com production key; no stub nunca chega a acontecer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CallbackPayload(
        String shortCode,
        String metaData,
        long gameId,
        String gameName,
        String gameType,
        int gameMap,
        String gameMode,
        String region
) {
}
