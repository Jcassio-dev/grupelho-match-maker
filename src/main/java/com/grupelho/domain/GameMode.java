package com.grupelho.domain;

public enum GameMode {
    THREE_V_THREE("3v3"),
    FOUR_V_FOUR("4v4"),
    ARAM("aram");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static GameMode fromLabel(String label) {
        for (GameMode mode : values()) {
            if (mode.label.equalsIgnoreCase(label)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Modo invalido: " + label);
    }
}
