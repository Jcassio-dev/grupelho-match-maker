package com.grupelho.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface DiscordCommand {

    SlashCommandData definition();

    void handle(SlashCommandInteractionEvent event);
}
