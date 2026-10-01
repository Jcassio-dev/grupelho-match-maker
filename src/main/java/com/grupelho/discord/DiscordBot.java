package com.grupelho.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {

    private final List<DiscordCommand> commands;
    private final Map<String, DiscordCommand> commandsByName;
    private final List<ButtonHandler> buttonHandlers;
    private JDA jda;

    public DiscordBot(List<DiscordCommand> commands, List<ButtonHandler> buttonHandlers) {
        this.commands = commands;
        this.commandsByName = commands.stream()
                .collect(Collectors.toMap(c -> c.definition().getName(), c -> c));
        this.buttonHandlers = buttonHandlers;
    }

    public void start(String token, String guildId) {
        jda = JDABuilder.createDefault(token)
                .addEventListeners(this)
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrompido esperando o JDA conectar", e);
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalStateException(
                    "Guild nao encontrada: " + guildId + ". O bot foi convidado para esse servidor?");
        }

        List<CommandData> definitions = commands.stream()
                .map(DiscordCommand::definition)
                .collect(Collectors.toList());
        guild.updateCommands().addCommands(definitions).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordCommand command = commandsByName.get(event.getName());
        if (command != null) {
            command.handle(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        for (ButtonHandler handler : buttonHandlers) {
            if (handler.supports(event.getComponentId())) {
                handler.handle(event);
                return;
            }
        }
    }
}
