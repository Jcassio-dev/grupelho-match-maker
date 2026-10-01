package com.grupelho.rating;

public interface RatingSystem {

    double defaultRating();

    RatingUpdate update(double teamARating, double teamBRating, boolean teamAWon);
}
