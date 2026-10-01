package com.grupelho;

import com.grupelho.cli.StubTournamentRunner;
import com.grupelho.config.Config;
import com.grupelho.discord.ButtonHandler;
import com.grupelho.discord.DiscordBot;
import com.grupelho.discord.DiscordCommand;
import com.grupelho.discord.commands.CriarCommand;
import com.grupelho.discord.commands.RankCommand;
import com.grupelho.discord.commands.RegistrarCommand;
import com.grupelho.discord.commands.ResultadoCommand;
import com.grupelho.rating.EloPorTime;
import com.grupelho.rating.RatingSystem;
import com.grupelho.riot.AccountApi;
import com.grupelho.riot.RiotHosts;
import com.grupelho.riot.RiotHttpClient;
import com.grupelho.riot.SummonerApi;
import com.grupelho.riot.TournamentApi;
import com.grupelho.store.Repository;
import com.grupelho.store.SqliteRepository;

import java.util.List;

public class App {

    public static void main(String[] args) {
        Config config = new Config();

        if (args.length > 0 && args[0].equals("stub-demo")) {
            StubTournamentRunner.run(config);
            return;
        }

        RiotHttpClient riotHttp = new RiotHttpClient(config.riotApiKey());
        AccountApi accountApi = new AccountApi(riotHttp, RiotHosts.regional(config.riotRegion()));
        SummonerApi summonerApi = new SummonerApi(riotHttp, RiotHosts.platform(config.riotRegion()));
        TournamentApi tournamentApi = new TournamentApi(riotHttp, RiotHosts.regional(config.riotRegion()), config.useStub());
        Repository repository = new SqliteRepository("grupelho.db");
        RatingSystem ratingSystem = new EloPorTime();

        long providerId = tournamentApi.createProvider(config.riotRegion(), config.callbackUrl());
        long tournamentId = tournamentApi.createTournament(providerId, "grupelho");
        System.out.println("Torneio Riot pronto. providerId=" + providerId + " tournamentId=" + tournamentId);

        ResultadoCommand resultadoCommand = new ResultadoCommand(repository, ratingSystem);

        List<DiscordCommand> commands = List.of(
                new RegistrarCommand(accountApi, summonerApi, repository),
                new CriarCommand(tournamentApi, repository, tournamentId),
                resultadoCommand,
                new RankCommand(repository)
        );
        List<ButtonHandler> buttonHandlers = List.of(resultadoCommand);

        DiscordBot bot = new DiscordBot(commands, buttonHandlers);
        bot.start(config.discordToken(), config.discordGuildId());

        System.out.println("grupelho conectado ao Discord.");
    }
}
