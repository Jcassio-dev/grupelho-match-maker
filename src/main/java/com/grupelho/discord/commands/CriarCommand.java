package com.grupelho.discord.commands;

import com.grupelho.discord.DiscordCommand;
import com.grupelho.domain.GameMode;
import com.grupelho.domain.Player;
import com.grupelho.riot.RiotApiException;
import com.grupelho.riot.TournamentApi;
import com.grupelho.riot.TournamentPresets;
import com.grupelho.riot.dto.TournamentCodeParams;
import com.grupelho.store.Repository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * time_a e time_b sao opcionais: sem eles o codigo e gerado sem restricao de
 * allowedParticipants e o /resultado dessa partida nao atualiza os standings
 * (nao ha como saber quem estava em cada time). Decisao documentada no README.
 */
public class CriarCommand implements DiscordCommand {

    private final TournamentApi tournamentApi;
    private final Repository repository;
    private final long tournamentId;

    public CriarCommand(TournamentApi tournamentApi, Repository repository, long tournamentId) {
        this.tournamentApi = tournamentApi;
        this.repository = repository;
        this.tournamentId = tournamentId;
    }

    @Override
    public SlashCommandData definition() {
        return Commands.slash("criar", "Cria uma sala de torneio e gera o codigo")
                .addOptions(
                        new OptionData(OptionType.STRING, "modo", "Modo da partida", true)
                                .addChoice("3v3", "3v3")
                                .addChoice("4v4", "4v4")
                                .addChoice("aram", "aram"),
                        new OptionData(OptionType.STRING, "time_a", "Mencoes dos jogadores do time A", false),
                        new OptionData(OptionType.STRING, "time_b", "Mencoes dos jogadores do time B", false)
                );
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        GameMode mode = GameMode.fromLabel(event.getOption("modo").getAsString());
        List<String> teamA = extractMentionedIds(event, "time_a");
        List<String> teamB = extractMentionedIds(event, "time_b");
        List<String> allowedParticipants = resolvePuuids(teamA, teamB);

        event.deferReply().queue();

        try {
            String internalId = repository.createMatch(mode, "PENDENTE", teamA, teamB);
            String metadata = "{\"internalMatchId\":\"" + internalId + "\"}";

            TournamentCodeParams params = buildParams(mode, allowedParticipants, metadata);
            List<String> codes = tournamentApi.createCodes(tournamentId, 1, params);
            String code = codes.get(0);
            repository.setTournamentCode(internalId, code);

            event.getHook().sendMessage(
                    "Sala criada! Modo: " + mode.label() + "\n"
                            + "Codigo de torneio: `" + code + "`\n"
                            + "ID interno da partida: `" + internalId + "`\n"
                            + "Quando a partida acabar, use `/resultado partida:" + internalId + " vencedor:A` ou `vencedor:B`."
            ).queue();
        } catch (RiotApiException e) {
            event.getHook().sendMessage("Nao consegui gerar o codigo de torneio: " + e.getMessage()).queue();
        }
    }

    private TournamentCodeParams buildParams(GameMode mode, List<String> allowedParticipants, String metadata) {
        return switch (mode) {
            case THREE_V_THREE -> TournamentPresets.threeVThree(metadata, allowedParticipants);
            case FOUR_V_FOUR -> TournamentPresets.fourVFour(metadata, allowedParticipants);
            case ARAM -> TournamentPresets.aram(5, metadata, allowedParticipants);
        };
    }

    private List<String> extractMentionedIds(SlashCommandInteractionEvent event, String optionName) {
        OptionMapping option = event.getOption(optionName);
        if (option == null) {
            return List.of();
        }
        return option.getMentions().getMembers().stream()
                .map(Member::getId)
                .toList();
    }

    private List<String> resolvePuuids(List<String> teamA, List<String> teamB) {
        List<String> discordIds = new ArrayList<>();
        discordIds.addAll(teamA);
        discordIds.addAll(teamB);

        return discordIds.stream()
                .map(repository::findPlayerByDiscordUserId)
                .flatMap(Optional::stream)
                .map(Player::puuid)
                .toList();
    }
}
