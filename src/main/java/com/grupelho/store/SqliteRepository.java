package com.grupelho.store;

import com.grupelho.domain.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class SqliteRepository implements Repository {

    private final String jdbcUrl;

    public SqliteRepository(String dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        migrate();
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao conectar no SQLite: " + jdbcUrl, e);
        }
    }

    private void migrate() {
        String sql = """
                CREATE TABLE IF NOT EXISTS players (
                    discord_user_id TEXT PRIMARY KEY,
                    puuid TEXT NOT NULL UNIQUE,
                    game_name TEXT NOT NULL,
                    tag_line TEXT NOT NULL,
                    registered_at TEXT NOT NULL
                )
                """;
        try (Connection conn = connect(); var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao migrar o banco de dados", e);
        }
    }

    @Override
    public void savePlayer(Player player) {
        String sql = """
                INSERT INTO players (discord_user_id, puuid, game_name, tag_line, registered_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(discord_user_id) DO UPDATE SET
                    puuid = excluded.puuid,
                    game_name = excluded.game_name,
                    tag_line = excluded.tag_line,
                    registered_at = excluded.registered_at
                """;
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.discordUserId());
            stmt.setString(2, player.puuid());
            stmt.setString(3, player.gameName());
            stmt.setString(4, player.tagLine());
            stmt.setString(5, player.registeredAt().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao salvar jogador " + player.discordUserId(), e);
        }
    }

    @Override
    public Optional<Player> findPlayerByDiscordUserId(String discordUserId) {
        String sql = "SELECT * FROM players WHERE discord_user_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, discordUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapPlayer(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar jogador " + discordUserId, e);
        }
    }

    @Override
    public Optional<Player> findPlayerByPuuid(String puuid) {
        String sql = "SELECT * FROM players WHERE puuid = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, puuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapPlayer(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar jogador por puuid " + puuid, e);
        }
    }

    private Player mapPlayer(ResultSet rs) throws SQLException {
        return new Player(
                rs.getString("discord_user_id"),
                rs.getString("puuid"),
                rs.getString("game_name"),
                rs.getString("tag_line"),
                Instant.parse(rs.getString("registered_at"))
        );
    }
}
