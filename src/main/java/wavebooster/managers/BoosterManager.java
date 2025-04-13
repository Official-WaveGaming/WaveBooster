package wavebooster.managers;

import wavebooster.WaveBooster;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;
import wavebooster.models.GlobalBooster;
import wavebooster.models.PersonalBooster;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BoosterManager {
	private final WaveBooster plugin;

	// All active boosters (both personal and global)
	private final Map<UUID, Booster> activeBoosters = new ConcurrentHashMap<>();

	// Player boosters that are not active yet
	private final Map<UUID, List<Booster>> playerBoosters = new ConcurrentHashMap<>();

	private BukkitTask boosterTask;

	public BoosterManager(WaveBooster plugin) {
		this.plugin = plugin;
		loadAllBoosters();
	}

	public void startBoosterTask() {
		if (boosterTask != null) {
			boosterTask.cancel();
		}

		// Check boosters every second to see if they've expired
		boosterTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			Iterator<Map.Entry<UUID, Booster>> iterator = activeBoosters.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<UUID, Booster> entry = iterator.next();
				Booster booster = entry.getValue();

				if (booster.isExpired()) {
					iterator.remove();
					plugin.getSqlManager().removeActiveBooster(booster.getId());

					// Notify players about expired boosters
					String message;
					if (booster.isGlobal()) {
						message = plugin.getMessagesConfig().getString("booster.global-expired")
							.replace("%type%", booster.getType().name())
							.replace("%multiplier%", String.valueOf(booster.getMultiplier()));

						for (Player player : Bukkit.getOnlinePlayers()) {
							player.sendMessage(message);
						}
					} else {
						PersonalBooster personalBooster = (PersonalBooster) booster;
						Player player = Bukkit.getPlayer(personalBooster.getPlayerId());

						if (player != null && player.isOnline()) {
							message = plugin.getMessagesConfig().getString("booster.personal-expired")
								.replace("%type%", booster.getType().name())
								.replace("%multiplier%", String.valueOf(booster.getMultiplier()));

							player.sendMessage(message);
						}
					}
				}
			}

			// Update tablist
			plugin.getTablistManager().updateTablist();
		}, 20L, 20L); // 20 ticks = 1 second
	}

	public void loadAllBoosters() {
		// Load all active boosters
		List<Booster> activeBoosters = plugin.getSqlManager().loadActiveBoosters();
		for (Booster booster : activeBoosters) {
			if (!booster.isExpired()) {
				this.activeBoosters.put(booster.getId(), booster);
			} else {
				plugin.getSqlManager().removeActiveBooster(booster.getId());
			}
		}

		// Load player boosters for online players
		for (Player player : Bukkit.getOnlinePlayers()) {
			loadPlayerBoosters(player.getUniqueId());
		}
	}

	public void loadPlayerBoosters(UUID playerId) {
		List<Booster> boosters = plugin.getSqlManager().loadPlayerBoosters(playerId);
		playerBoosters.put(playerId, boosters);
	}

	public void saveAllBoosters() {
		// Save all player boosters
		for (Map.Entry<UUID, List<Booster>> entry : playerBoosters.entrySet()) {
			plugin.getSqlManager().savePlayerBoosters(entry.getKey(), entry.getValue());
		}

		// Active boosters are saved individually when activated/deactivated
	}

	public void savePlayerBoosters(UUID playerId) {
		List<Booster> boosters = playerBoosters.getOrDefault(playerId, new ArrayList<>());
		plugin.getSqlManager().savePlayerBoosters(playerId, boosters);
	}

	public List<Booster> getPlayerBoosters(UUID playerId) {
		return playerBoosters.getOrDefault(playerId, new ArrayList<>());
	}

	public void givePlayerBooster(UUID playerId, BoosterType type, int multiplier, long duration, boolean isGlobal) {
		Booster booster;
		UUID boosterId = UUID.randomUUID();

		if (isGlobal) {
			booster = new GlobalBooster(boosterId, type, multiplier, duration, playerId);
		} else {
			booster = new PersonalBooster(boosterId, type, multiplier, duration, playerId);
		}

		List<Booster> boosters = playerBoosters.getOrDefault(playerId, new ArrayList<>());
		boosters.add(booster);
		playerBoosters.put(playerId, boosters);

		plugin.getSqlManager().addBoosterToPlayer(playerId, booster);

		Player player = Bukkit.getPlayer(playerId);
		if (player != null && player.isOnline()) {
			String message = plugin.getMessagesConfig().getString("booster.received")
				.replace("%type%", type.name())
				.replace("%multiplier%", String.valueOf(multiplier))
				.replace("%global%", isGlobal ? plugin.getMessagesConfig().getString("booster.global") : plugin.getMessagesConfig().getString("booster.personal"));

			player.sendMessage(message);
		}
	}

	public void removePlayerBooster(UUID playerId, UUID boosterId) {
		List<Booster> boosters = playerBoosters.getOrDefault(playerId, new ArrayList<>());
		boosters.removeIf(b -> b.getId().equals(boosterId));
		playerBoosters.put(playerId, boosters);

		// Also remove from active boosters if it's active
		activeBoosters.remove(boosterId);

		plugin.getSqlManager().removeBoosterFromPlayer(boosterId);
	}

	public boolean activateBooster(UUID playerId, UUID boosterId) {
		List<Booster> boosters = playerBoosters.getOrDefault(playerId, new ArrayList<>());
		Optional<Booster> optBooster = boosters.stream()
			.filter(b -> b.getId().equals(boosterId))
			.findFirst();

		if (!optBooster.isPresent()) {
			return false;
		}

		Booster booster = optBooster.get();
		if (booster.isActive()) {
			return false;
		}

		booster.activate();
		activeBoosters.put(booster.getId(), booster);

		// Remove from player boosters
		boosters.removeIf(b -> b.getId().equals(boosterId));

		// Save to database
		plugin.getSqlManager().saveActiveBooster(booster);

		// Notify player
		Player player = Bukkit.getPlayer(playerId);
		if (player != null && player.isOnline()) {
			String message;
			if (booster.isGlobal()) {
				message = plugin.getMessagesConfig().getString("booster.global-activated")
					.replace("%type%", booster.getType().name())
					.replace("%multiplier%", String.valueOf(booster.getMultiplier()))
					.replace("%time%", formatTime(booster.getDuration()));

				// Notify all players
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendMessage(message);
				}
			} else {
				message = plugin.getMessagesConfig().getString("booster.personal-activated")
					.replace("%type%", booster.getType().name())
					.replace("%multiplier%", String.valueOf(booster.getMultiplier()))
					.replace("%time%", formatTime(booster.getDuration()));

				player.sendMessage(message);
			}
		}

		// Update tablist
		plugin.getTablistManager().updateTablist();

		return true;
	}

	// Get active boosters for a player (both personal and applicable globals)
	public Map<BoosterType, Integer> getActiveBoostersForPlayer(UUID playerId) {
		Map<BoosterType, Integer> result = new HashMap<>();

		// Check for personal boosters
		for (Booster booster : activeBoosters.values()) {
			if (!booster.isGlobal() && ((PersonalBooster) booster).getPlayerId().equals(playerId)) {
				int current = result.getOrDefault(booster.getType(), 1);
				result.put(booster.getType(), current * booster.getMultiplier());
			}
		}

		// Check for global boosters
		for (Booster booster : activeBoosters.values()) {
			if (booster.isGlobal()) {
				int current = result.getOrDefault(booster.getType(), 1);
				result.put(booster.getType(), current * booster.getMultiplier());
			}
		}

		return result;
	}

	// Get all global boosters
	public List<Booster> getGlobalBoosters() {
		return activeBoosters.values().stream()
			.filter(Booster::isGlobal)
			.collect(Collectors.toList());
	}

	// Helper method to format time (seconds) to human-readable format
	private String formatTime(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long remainingSeconds = seconds % 60;

		if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes, remainingSeconds);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, remainingSeconds);
		} else {
			return String.format("%ds", remainingSeconds);
		}
	}
}
