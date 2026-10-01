package com.grupelho.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private final Dotenv dotenv;

    public Config() {
        this.dotenv = Dotenv.configure().ignoreIfMissing().load();
    }

    public String riotApiKey() {
        return require("RIOT_API_KEY");
    }

    public String riotRegion() {
        return get("RIOT_REGION", "BR");
    }

    public boolean useStub() {
        return Boolean.parseBoolean(get("USE_STUB", "true"));
    }

    public String callbackUrl() {
        return get("CALLBACK_URL", "https://exemplo.com/riot/callback");
    }

    public String discordToken() {
        return require("DISCORD_TOKEN");
    }

    public String discordGuildId() {
        return require("DISCORD_GUILD_ID");
    }

    public int callbackPort() {
        return Integer.parseInt(get("CALLBACK_PORT", "7000"));
    }

    private String get(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private String require(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variavel de ambiente obrigatoria ausente: " + key);
        }
        return value;
    }
}
