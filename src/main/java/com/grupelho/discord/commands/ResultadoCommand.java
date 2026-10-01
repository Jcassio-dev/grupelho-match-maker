package com.grupelho.discord.commands;

import com.grupelho.discord.ButtonHandler;
import com.grupelho.discord.DiscordCommand;
import com.grupelho.domain.Match;
import com.grupelho.domain.MatchStatus;
import com.grupelho.domain.Result;
import com.grupelho.domain.Side;
import com.grupelho.rating.RatingApplier;
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
    private final RatingApplier ratingApplier;

    public ResultadoCommand(Repository repository, RatingApplier ratingApplier) {
        this.repository = repository;
        this.ratingApplier = ratingApplier;
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
        ratingApplier.apply(match.mode(), match.teamA(), match.teamB(), result.winner() == Side.A);

        event.reply("Resultado confirmado! Standings do modo " + match.mode().label() + " atualizados.").queue();
    }
}
