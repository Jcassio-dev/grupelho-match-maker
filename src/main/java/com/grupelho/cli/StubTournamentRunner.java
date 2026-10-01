package com.grupelho.cli;

import com.grupelho.config.Config;
import com.grupelho.riot.RiotHosts;
import com.grupelho.riot.RiotHttpClient;
import com.grupelho.riot.TournamentApi;
import com.grupelho.riot.TournamentPresets;
import com.grupelho.riot.dto.TournamentCodeParams;

import java.util.List;

/**
 * Runner de demonstracao: no modo stub, cria provider + torneio e gera 1 codigo 3v3.
 * Exige RIOT_API_KEY valida no .env (uma dev key ja tem acesso a Tournament-Stub-V5).
 */
public class StubTournamentRunner {

    public static void run(Config config) {
        if (!config.useStub()) {
            System.out.println("USE_STUB esta false. Esse runner de demonstracao so roda com USE_STUB=true.");
            return;
        }

        RiotHttpClient http = new RiotHttpClient(config.riotApiKey());
        String regionalHost = RiotHosts.regional(config.riotRegion());
        TournamentApi tournamentApi = new TournamentApi(http, regionalHost, true);

        System.out.println("Criando provider de torneio (stub)...");
        long providerId = tournamentApi.createProvider(config.riotRegion(), config.callbackUrl());
        System.out.println("providerId = " + providerId);

        System.out.println("Criando torneio (stub)...");
        long tournamentId = tournamentApi.createTournament(providerId, "grupelho-demo");
        System.out.println("tournamentId = " + tournamentId);

        System.out.println("Gerando 1 codigo 3v3 (stub)...");
        TournamentCodeParams params = TournamentPresets.threeVThree("{\"internalMatchId\":\"demo-1\"}", List.of());
        List<String> codes = tournamentApi.createCodes(tournamentId, 1, params);
        System.out.println("codigo gerado: " + codes.get(0));
    }
}
