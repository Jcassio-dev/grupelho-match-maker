package com.grupelho.store;

import com.grupelho.domain.Player;

import java.util.Optional;

public interface Repository {

    void savePlayer(Player player);

    Optional<Player> findPlayerByDiscordUserId(String discordUserId);

    Optional<Player> findPlayerByPuuid(String puuid);
}
