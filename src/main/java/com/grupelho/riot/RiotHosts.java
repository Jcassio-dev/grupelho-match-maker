package com.grupelho.riot;

public final class RiotHosts {

    private RiotHosts() {
    }

    public static String platform(String region) {
        return switch (region.toUpperCase()) {
            case "BR" -> "https://br1.api.riotgames.com";
            default -> throw new IllegalArgumentException("Regiao nao suportada: " + region);
        };
    }

    public static String regional(String region) {
        return switch (region.toUpperCase()) {
            case "BR" -> "https://americas.api.riotgames.com";
            default -> throw new IllegalArgumentException("Regiao nao suportada: " + region);
        };
    }
}
