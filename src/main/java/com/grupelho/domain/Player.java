package com.grupelho.domain;

import java.time.Instant;

public record Player(String discordUserId, String puuid, String gameName, String tagLine, Instant registeredAt) {
}
