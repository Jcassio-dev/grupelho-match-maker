package com.grupelho.discord.commands;

import com.grupelho.discord.ButtonHandler;
import com.grupelho.discord.DiscordCommand;
import com.grupelho.domain.GameMode;
import com.grupelho.domain.Match;
import com.grupelho.domain.MatchStatus;
import com.grupelho.domain.Result;
import com.grupelho.domain.Side;
import com.grupelho.rating.RatingSystem;
import com.grupelho.rating.RatingUpdate;
import com.grupelho.store.Repository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Fallback manual enquanto nao ha production key da Riot. O resultado so
 * vale depois que alguem do time que perdeu confirma pelo botao. Se a
 * partida foi criada sem times registrados (time_a/time_b vazios), qualquer
 * um pode confirmar e os standings nao sao atualizados.
 */
public class ResultadoCommand implements DiscordCommand, ButtonHandler {

    private static final String BUTTON_PREFIX = "grupelho-confirmar:";

    private final Repository repository;
    private final RatingSystem ratingSystem;

    public ResultadoCommand(Repository repository, RatingSystem ratingSystem) {
        this.repository = repository;
        this.ratingSystem = ratingSystem;
    }

    @Override
    public SlashCommandData definition() {
        return Commands.slash("resultado", "Reporta o vencedor de uma partida (precisa de confirmacao do time perdedor)")
                .addOptions(
                        new OptionData(OptionType.STRING, "partida", "ID interno da partida", true),
                        new OptionData(OptionType.STRING, "vencedor", "Time vencedor", true)
                                .addChoice("A", "A")
                                .addChoice("B", "B")
                );
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        String matchId = event.getOption("partida").getAsString();
        Side winner = Side.valueOf(event.getOption("vencedor").getAsString().toUpperCase());

        Optional<Match> matchOpt = repository.findMatchByInternalId(matchId);
        if (matchOpt.isEmpty()) {
            event.reply("Partida `" + matchId + "` nao encontrada.").setEphemeral(true).queue();
            return;
        }

        Match match = matchOpt.get();
        repository.saveResult(new Result(matchId, winner, event.getUser().getId(), null, Instant.now()));
        repository.updateMatchStatus(matchId, MatchStatus.AWAITING_CONFIRMATION);

        List<String> losingTeam = winner == Side.A ? match.teamB() : match.teamA();
        String confirmInfo = losingTeam.isEmpty()
                ? "Essa partida foi criada sem times registrados: qualquer um pode confirmar."
                : "Alguem do time que perdeu precisa confirmar: "
                        + String.join(", ", losingTeam.stream().map(id -> "<@" + id + ">").toList());

        event.reply(
                "Resultado reportado: time " + winner + " venceu a partida `" + matchId + "`.\n" + confirmInfo
        ).addActionRow(
                Button.success(BUTTON_PREFIX + matchId, "Confirmar resultado")
        ).queue();
    }

    @Override
    public boolean supports(String componentId) {
        return componentId.startsWith(BUTTON_PREFIX);
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        String matchId = event.getComponentId().substring(BUTTON_PREFIX.length());

        Optional<Match> matchOpt = repository.findMatchByInternalId(matchId);
        Optional<Result> resultOpt = repository.findResultByMatchId(matchId);
        if (matchOpt.isEmpty() || resultOpt.isEmpty()) {
            event.reply("Partida ou resultado nao encontrado.").setEphemeral(true).queue();
            return;
        }

        Match match = matchOpt.get();
        Result result = resultOpt.get();
        List<String> losingTeam = result.winner() == Side.A ? match.teamB() : match.teamA();

        if (!losingTeam.isEmpty() && !losingTeam.contains(event.getUser().getId())) {
            event.reply("So alguem do time que perdeu pode confirmar esse resultado.").setEphemeral(true).queue();
            return;
        }

        repository.confirmResult(matchId, event.getUser().getId());
        repository.updateMatchStatus(matchId, MatchStatus.CONFIRMED);
        applyRating(match, result.winner());

        event.reply("Resultado confirmado! Standings do modo " + match.mode().label() + " atualizados.").queue();
    }

    private void applyRating(Match match, Side winner) {
        GameMode mode = match.mode();
        boolean teamAWon = winner == Side.A;

        double ratingA = averageRating(mode, match.teamA());
        double ratingB = averageRating(mode, match.teamB());

        RatingUpdate update = ratingSystem.update(ratingA, ratingB, teamAWon);
        double deltaA = update.newTeamARating() - ratingA;
        double deltaB = update.newTeamBRating() - ratingB;

        applyDelta(mode, match.teamA(), deltaA);
        applyDelta(mode, match.teamB(), deltaB);
    }

    private double averageRating(GameMode mode, List<String> team) {
        if (team.isEmpty()) {
            return ratingSystem.defaultRating();
        }
        return team.stream()
                .mapToDouble(id -> repository.getRating(mode, id, ratingSystem.defaultRating()))
                .average()
                .orElse(ratingSystem.defaultRating());
    }

    private void applyDelta(GameMode mode, List<String> team, double delta) {
        for (String discordUserId : team) {
            double current = repository.getRating(mode, discordUserId, ratingSystem.defaultRating());
            repository.setRating(mode, discordUserId, current + delta);
        }
    }
}
