package com.grupelho.rating;

/**
 * Elo calculado por time: cada time e tratado como um unico "jogador" para
 * calcular a expectativa de vitoria, e a variacao resultante e aplicada
 * igualmente ao rating individual de cada jogador do time. Rankeia so por
 * vitoria/derrota, sem considerar KDA ou outras estatisticas.
 */
public class EloPorTime implements RatingSystem {

    private static final double K_FACTOR = 32.0;
    private static final double DEFAULT_RATING = 1000.0;

    @Override
    public double defaultRating() {
        return DEFAULT_RATING;
    }

    @Override
    public RatingUpdate update(double teamARating, double teamBRating, boolean teamAWon) {
        double expectedA = 1.0 / (1.0 + Math.pow(10, (teamBRating - teamARating) / 400.0));
        double expectedB = 1.0 - expectedA;
        double scoreA = teamAWon ? 1.0 : 0.0;
        double scoreB = teamAWon ? 0.0 : 1.0;

        double newA = teamARating + K_FACTOR * (scoreA - expectedA);
        double newB = teamBRating + K_FACTOR * (scoreB - expectedB);
        return new RatingUpdate(newA, newB);
    }
}
