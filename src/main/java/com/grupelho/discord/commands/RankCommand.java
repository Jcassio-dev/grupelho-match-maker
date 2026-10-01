package com.grupelho.discord.commands;

import com.grupelho.discord.DiscordCommand;
import com.grupelho.domain.GameMode;
import com.grupelho.domain.StandingEntry;
import com.grupelho.store.Repository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class RankCommand implements DiscordCommand {

    private final Repository repository;

    public RankCommand(Repository repository) {
        this.repository = repository;
    }

    @Override
    public SlashCommandData definition() {
        return Commands.slash("rank", "Mostra os standings do modo")
                .addOptions(
                        new OptionData(OptionType.STRING, "modo", "Modo da partida", true)
                                .addChoice("3v3", "3v3")
                                .addChoice("4v4", "4v4")
                                .addChoice("aram", "aram")
                );
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        GameMode mode = GameMode.fromLabel(event.getOption("modo").getAsString());
        List<StandingEntry> standings = repository.listStandings(mode);

        if (standings.isEmpty()) {
            event.reply("Ainda nao ha standings para o modo " + mode.label() + ".").queue();
            return;
        }

        StringBuilder message = new StringBuilder("Standings " + mode.label() + ":\n");
        for (int i = 0; i < standings.size(); i++) {
            StandingEntry entry = standings.get(i);
            message.append(i + 1).append(". ").append(entry.displayName())
                    .append(" - ").append(Math.round(entry.rating())).append("\n");
        }

        event.reply(message.toString()).queue();
    }
}
