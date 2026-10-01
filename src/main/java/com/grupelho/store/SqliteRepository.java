package com.grupelho.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupelho.domain.GameMode;
import com.grupelho.domain.Match;
import com.grupelho.domain.MatchStatus;
import com.grupelho.domain.Player;
import com.grupelho.domain.Result;
import com.grupelho.domain.Side;
import com.grupelho.domain.StandingEntry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteRepository implements Repository {

    private final String jdbcUrl;
    private final ObjectMapper mapper = new ObjectMapper();

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
        String players = """
                CREATE TABLE IF NOT EXISTS players (
                    discord_user_id TEXT PRIMARY KEY,
                    puuid TEXT NOT NULL UNIQUE,
                    game_name TEXT NOT NULL,
                    tag_line TEXT NOT NULL,
                    registered_at TEXT NOT NULL
                )
                """;
        String matches = """
                CREATE TABLE IF NOT EXISTS matches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mode TEXT NOT NULL,
                    tournament_code TEXT NOT NULL,
                    team_a TEXT NOT NULL,
                    team_b TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """;
        String results = """
                CREATE TABLE IF NOT EXISTS results (
                    match_id TEXT PRIMARY KEY,
                    winner TEXT NOT NULL,
                    reported_by TEXT NOT NULL,
                    confirmed_by TEXT,
                    reported_at TEXT NOT NULL
                )
                """;
        String ratings = """
                CREATE TABLE IF NOT EXISTS ratings (
                    mode TEXT NOT NULL,
                    discord_user_id TEXT NOT NULL,
                    rating REAL NOT NULL,
                    PRIMARY KEY (mode, discord_user_id)
                )
                """;
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(players);
            stmt.execute(matches);
            stmt.execute(results);
            stmt.execute(ratings);
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

    @Override
    public String createMatch(GameMode mode, String tournamentCode, List<String> teamA, List<String> teamB) {
        String sql = """
                INSERT INTO matches (mode, tournament_code, team_a, team_b, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, mode.name());
            stmt.setString(2, tournamentCode);
            stmt.setString(3, mapper.writeValueAsString(teamA));
            stmt.setString(4, mapper.writeValueAsString(teamB));
            stmt.setString(5, MatchStatus.AWAITING_RESULT.name());
            stmt.setString(6, Instant.now().toString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return String.valueOf(keys.getLong(1));
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar partida", e);
        }
    }

    @Override
    public Optional<Match> findMatchByInternalId(String internalId) {
        String sql = "SELECT * FROM matches WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(internalId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapMatch(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao buscar partida " + internalId, e);
        }
    }

    @Override
    public void updateMatchStatus(String internalId, MatchStatus status) {
        String sql = "UPDATE matches SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, Long.parseLong(internalId));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao atualizar status da partida " + internalId, e);
        }
    }

    private Match mapMatch(ResultSet rs) throws Exception {
        return new Match(
                String.valueOf(rs.getLong("id")),
                GameMode.valueOf(rs.getString("mode")),
                rs.getString("tournament_code"),
                mapper.readValue(rs.getString("team_a"), new TypeReference<List<String>>() {
                }),
                mapper.readValue(rs.getString("team_b"), new TypeReference<List<String>>() {
                }),
                MatchStatus.valueOf(rs.getString("status")),
                Instant.parse(rs.getString("created_at"))
        );
    }

    @Override
    public void saveResult(Result result) {
        String sql = """
                INSERT INTO results (match_id, winner, reported_by, confirmed_by, reported_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(match_id) DO UPDATE SET
                    winner = excluded.winner,
                    reported_by = excluded.reported_by,
                    reported_at = excluded.reported_at
                """;
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.internalMatchId());
            stmt.setString(2, result.winner().name());
            stmt.setString(3, result.reportedByDiscordId());
            stmt.setString(4, result.confirmedByDiscordId());
            stmt.setString(5, result.reportedAt().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao salvar resultado da partida " + result.internalMatchId(), e);
        }
    }

    @Override
    public Optional<Result> findResultByMatchId(String internalId) {
        String sql = "SELECT * FROM results WHERE match_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, internalId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapResult(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar resultado da partida " + internalId, e);
        }
    }

    @Override
    public void confirmResult(String internalId, String confirmedByDiscordId) {
        String sql = "UPDATE results SET confirmed_by = ? WHERE match_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, confirmedByDiscordId);
            stmt.setString(2, internalId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao confirmar resultado da partida " + internalId, e);
        }
    }

    private Result mapResult(ResultSet rs) throws SQLException {
        return new Result(
                rs.getString("match_id"),
                Side.valueOf(rs.getString("winner")),
                rs.getString("reported_by"),
                rs.getString("confirmed_by"),
                Instant.parse(rs.getString("reported_at"))
        );
    }

    @Override
    public double getRating(GameMode mode, String discordUserId, double defaultRating) {
        String sql = "SELECT rating FROM ratings WHERE mode = ? AND discord_user_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mode.name());
            stmt.setString(2, discordUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("rating") : defaultRating;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar rating de " + discordUserId, e);
        }
    }

    @Override
    public void setRating(GameMode mode, String discordUserId, double rating) {
        String sql = """
                INSERT INTO ratings (mode, discord_user_id, rating)
                VALUES (?, ?, ?)
                ON CONFLICT(mode, discord_user_id) DO UPDATE SET rating = excluded.rating
                """;
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mode.name());
            stmt.setString(2, discordUserId);
            stmt.setDouble(3, rating);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao salvar rating de " + discordUserId, e);
        }
    }

    @Override
    public List<StandingEntry> listStandings(GameMode mode) {
        String sql = """
                SELECT r.discord_user_id, r.rating, p.game_name, p.tag_line
                FROM ratings r
                LEFT JOIN players p ON p.discord_user_id = r.discord_user_id
                WHERE r.mode = ?
                ORDER BY r.rating DESC
                """;
        List<StandingEntry> standings = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mode.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String gameName = rs.getString("game_name");
                    String tagLine = rs.getString("tag_line");
                    String displayName = gameName != null
                            ? gameName + "#" + tagLine
                            : rs.getString("discord_user_id");
                    standings.add(new StandingEntry(rs.getString("discord_user_id"), displayName, rs.getDouble("rating")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar standings do modo " + mode, e);
        }
        return standings;
    }
}
