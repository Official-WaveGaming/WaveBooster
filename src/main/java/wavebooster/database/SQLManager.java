package wavebooster.database;

import wavebooster.WaveBooster;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;
import wavebooster.models.GlobalBooster;
import wavebooster.models.PersonalBooster;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLManager {
	private final WaveBooster plugin;

	public SQLManager(WaveBooster plugin) {
		this.plugin = plugin;
		initializeTables();
	}

	private void initializeTables() {
		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			// Create player_boosters table
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE IF NOT EXISTS player_boosters (" +
					"id VARCHAR(36) PRIMARY KEY, " +
					"player_id VARCHAR(36) NOT NULL, " +
					"booster_type VARCHAR(10) NOT NULL, " +
					"multiplier INT NOT NULL, " +
					"duration BIGINT NOT NULL, " +
					"active BOOLEAN NOT NULL DEFAULT FALSE, " +
					"start_time BIGINT DEFAULT 0, " +
					"global BOOLEAN NOT NULL DEFAULT FALSE)");
			}

			// Create active_boosters table for currently active boosters
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE IF NOT EXISTS active_boosters (" +
					"id VARCHAR(36) PRIMARY KEY, " +
					"player_id VARCHAR(36) NOT NULL, " +
					"booster_type VARCHAR(10) NOT NULL, " +
					"multiplier INT NOT NULL, " +
					"duration BIGINT NOT NULL, " +
					"start_time BIGINT NOT NULL, " +
					"global BOOLEAN NOT NULL)");
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
		}
	}

	// Save a player's non-active boosters
	public void savePlayerBoosters(UUID playerId, List<Booster> boosters) {
		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			// First, delete all existing inactive boosters for this player
			try (PreparedStatement ps = conn.prepareStatement(
				"DELETE FROM player_boosters WHERE player_id = ? AND active = FALSE")) {
				ps.setString(1, playerId.toString());
				ps.executeUpdate();
			}

			// Then insert all new inactive boosters
			String sql = "INSERT INTO player_boosters (id, player_id, booster_type, multiplier, duration, active, global) " +
				"VALUES (?, ?, ?, ?, ?, FALSE, ?)";

			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				for (Booster booster : boosters) {
					if (!booster.isActive()) {  // Only store inactive boosters here
						ps.setString(1, booster.getId().toString());

						if (booster instanceof PersonalBooster) {
							ps.setString(2, ((PersonalBooster) booster).getPlayerId().toString());
						} else {
							ps.setString(2, ((GlobalBooster) booster).getActivatorId().toString());
						}

						ps.setString(3, booster.getType().name());
						ps.setInt(4, booster.getMultiplier());
						ps.setLong(5, booster.getDuration());
						ps.setBoolean(6, booster.isGlobal());

						ps.addBatch();
					}
				}
				ps.executeBatch();
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to save player boosters for player " + playerId, e);
		}
	}

	// Load a player's non-active boosters
	public List<Booster> loadPlayerBoosters(UUID playerId) {
		List<Booster> boosters = new ArrayList<>();

		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			String sql = "SELECT * FROM player_boosters WHERE player_id = ? AND active = FALSE";

			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, playerId.toString());

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID id = UUID.fromString(rs.getString("id"));
						BoosterType type = BoosterType.valueOf(rs.getString("booster_type"));
						int multiplier = rs.getInt("multiplier");
						long duration = rs.getLong("duration");
						boolean global = rs.getBoolean("global");

						Booster booster;
						if (global) {
							booster = new GlobalBooster(id, type, multiplier, duration, playerId);
						} else {
							booster = new PersonalBooster(id, type, multiplier, duration, playerId);
						}

						boosters.add(booster);
					}
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to load player boosters for player " + playerId, e);
		}

		return boosters;
	}

	// Save an active booster
	public void saveActiveBooster(Booster booster) {
		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			String sql = "INSERT INTO active_boosters (id, player_id, booster_type, multiplier, duration, start_time, global) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE " +
				"player_id = VALUES(player_id), " +
				"booster_type = VALUES(booster_type), " +
				"multiplier = VALUES(multiplier), " +
				"duration = VALUES(duration), " +
				"start_time = VALUES(start_time), " +
				"global = VALUES(global)";

			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, booster.getId().toString());

				if (booster instanceof PersonalBooster) {
					ps.setString(2, ((PersonalBooster) booster).getPlayerId().toString());
				} else {
					ps.setString(2, ((GlobalBooster) booster).getActivatorId().toString());
				}

				ps.setString(3, booster.getType().name());
				ps.setInt(4, booster.getMultiplier());
				ps.setLong(5, booster.getDuration());
				ps.setLong(6, booster.getStartTime());
				ps.setBoolean(7, booster.isGlobal());

				ps.executeUpdate();
			}

			// Also mark it as active in player_boosters if it exists there
			try (PreparedStatement ps = conn.prepareStatement(
				"UPDATE player_boosters SET active = TRUE, start_time = ? WHERE id = ?")) {
				ps.setLong(1, booster.getStartTime());
				ps.setString(2, booster.getId().toString());
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to save active booster " + booster.getId(), e);
		}
	}

	// Load all active boosters
	public List<Booster> loadActiveBoosters() {
		List<Booster> boosters = new ArrayList<>();

		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			String sql = "SELECT * FROM active_boosters";

			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID id = UUID.fromString(rs.getString("id"));
						UUID playerId = UUID.fromString(rs.getString("player_id"));
						BoosterType type = BoosterType.valueOf(rs.getString("booster_type"));
						int multiplier = rs.getInt("multiplier");
						long duration = rs.getLong("duration");
						long startTime = rs.getLong("start_time");
						boolean global = rs.getBoolean("global");

						Booster booster;
						if (global) {
							booster = new GlobalBooster(id, type, multiplier, duration, playerId);
						} else {
							booster = new PersonalBooster(id, type, multiplier, duration, playerId);
						}

						booster.activate();
						// Manually set start time since activate() sets it to current time
						try {
							java.lang.reflect.Field startTimeField = Booster.class.getDeclaredField("startTime");
							startTimeField.setAccessible(true);
							startTimeField.set(booster, startTime);
						} catch (ReflectiveOperationException e) {
							plugin.getLogger().log(Level.SEVERE, "Failed to set start time for booster " + id, e);
						}

						boosters.add(booster);
					}
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to load active boosters", e);
		}

		return boosters;
	}

	// Remove an active booster
	public void removeActiveBooster(UUID boosterId) {
		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(
				"DELETE FROM active_boosters WHERE id = ?")) {
				ps.setString(1, boosterId.toString());
				ps.executeUpdate();
			}

			// Also mark it as inactive in player_boosters if it exists there
			try (PreparedStatement ps = conn.prepareStatement(
				"UPDATE player_boosters SET active = FALSE WHERE id = ?")) {
				ps.setString(1, boosterId.toString());
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to remove active booster " + boosterId, e);
		}
	}

	// Add a booster to a player
	public void addBoosterToPlayer(UUID playerId, Booster booster) {
		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			String sql = "INSERT INTO player_boosters (id, player_id, booster_type, multiplier, duration, active, global) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?)";

			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, booster.getId().toString());

				if (booster instanceof PersonalBooster) {
					ps.setString(2, ((PersonalBooster) booster).getPlayerId().toString());
				} else {
					ps.setString(2, ((GlobalBooster) booster).getActivatorId().toString());
				}

				ps.setString(3, booster.getType().name());
				ps.setInt(4, booster.getMultiplier());
				ps.setLong(5, booster.getDuration());
				ps.setBoolean(6, booster.isActive());
				ps.setBoolean(7, booster.isGlobal());

				ps.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to add booster to player " + playerId, e);
		}
	}

	// Remove a booster from a player
	public void removeBoosterFromPlayer(UUID boosterId) {
		try (Connection conn = plugin.getDatabaseConnector().getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(
				"DELETE FROM player_boosters WHERE id = ?")) {
				ps.setString(1, boosterId.toString());
				ps.executeUpdate();
			}

			// Also remove from active_boosters if it exists there
			try (PreparedStatement ps = conn.prepareStatement(
				"DELETE FROM active_boosters WHERE id = ?")) {
				ps.setString(1, boosterId.toString());
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to remove booster " + boosterId, e);
		}
	}
}
