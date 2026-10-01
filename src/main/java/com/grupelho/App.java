package com.grupelho;

import com.grupelho.cli.StubTournamentRunner;
import com.grupelho.config.Config;
import com.grupelho.discord.DiscordBot;
import com.grupelho.discord.commands.RegistrarCommand;
import com.grupelho.riot.AccountApi;
import com.grupelho.riot.RiotHosts;
import com.grupelho.riot.RiotHttpClient;
import com.grupelho.riot.SummonerApi;
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
        Repository repository = new SqliteRepository("grupelho.db");

        DiscordBot bot = new DiscordBot(List.of(
                new RegistrarCommand(accountApi, summonerApi, repository)
        ));
        bot.start(config.discordToken(), config.discordGuildId());

        System.out.println("grupelho conectado ao Discord.");
    }
}
