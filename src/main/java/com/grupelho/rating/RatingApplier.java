package com.grupelho.rating;

import com.grupelho.domain.GameMode;
import com.grupelho.store.Repository;

import java.util.List;

/**
 * Aplica a atualizacao de rating de um RatingSystem ao rating individual de
 * cada jogador do time, usado tanto pelo fallback manual (/resultado) quanto
 * pelo callback automatico da Riot.
 */
public class RatingApplier {

    private final Repository repository;
    private final RatingSystem ratingSystem;

    public RatingApplier(Repository repository, RatingSystem ratingSystem) {
        this.repository = repository;
        this.ratingSystem = ratingSystem;
    }

    public void apply(GameMode mode, List<String> teamA, List<String> teamB, boolean teamAWon) {
        double ratingA = averageRating(mode, teamA);
        double ratingB = averageRating(mode, teamB);

        RatingUpdate update = ratingSystem.update(ratingA, ratingB, teamAWon);
        double deltaA = update.newTeamARating() - ratingA;
        double deltaB = update.newTeamBRating() - ratingB;

        applyDelta(mode, teamA, deltaA);
        applyDelta(mode, teamB, deltaB);
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
