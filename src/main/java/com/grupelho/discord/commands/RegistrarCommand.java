package com.grupelho.discord.commands;

import com.grupelho.discord.DiscordCommand;
import com.grupelho.domain.Player;
import com.grupelho.riot.AccountApi;
import com.grupelho.riot.RiotApiException;
import com.grupelho.riot.SummonerApi;
import com.grupelho.riot.dto.AccountDto;
import com.grupelho.store.Repository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.time.Instant;

public class RegistrarCommand implements DiscordCommand {

    private final AccountApi accountApi;
    private final SummonerApi summonerApi;
    private final Repository repository;

    public RegistrarCommand(AccountApi accountApi, SummonerApi summonerApi, Repository repository) {
        this.accountApi = accountApi;
        this.summonerApi = summonerApi;
        this.repository = repository;
    }

    @Override
    public SlashCommandData definition() {
        return Commands.slash("registrar", "Cadastra seu Riot ID no grupelho")
                .addOption(OptionType.STRING, "riot_id", "Seu Riot ID no formato Nome#TAG", true);
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        String riotId = event.getOption("riot_id").getAsString();
        String[] parts = riotId.split("#", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            event.reply("Formato invalido. Use `Nome#TAG`, exemplo: `Faker#BR1`.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();
        String gameName = parts[0];
        String tagLine = parts[1];

        try {
            AccountDto account = accountApi.getByRiotId(gameName, tagLine);
            summonerApi.getByPuuid(account.puuid());

            Player player = new Player(
                    event.getUser().getId(),
                    account.puuid(),
                    account.gameName(),
                    account.tagLine(),
                    Instant.now()
            );
            repository.savePlayer(player);

            event.getHook().sendMessage(
                    "Cadastrado! " + account.gameName() + "#" + account.tagLine()
                            + " vinculado a " + event.getUser().getAsMention() + "."
            ).queue();
        } catch (RiotApiException e) {
            event.getHook().sendMessage("Nao consegui confirmar essa conta na Riot: " + e.getMessage()).queue();
        }
    }
}
