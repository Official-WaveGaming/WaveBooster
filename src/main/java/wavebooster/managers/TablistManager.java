package wavebooster.managers;

import wavebooster.WaveBooster;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;
import wavebooster.models.PersonalBooster;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TablistManager {
	private final WaveBooster plugin;
	private BukkitTask tablistTask;

	// Map to store player's header and footer
	private final Map<UUID, String[]> playerTablist = new HashMap<>();

	public TablistManager(WaveBooster plugin) {
		this.plugin = plugin;
		startTablistTask();
	}

	public void startTablistTask() {
		if (tablistTask != null) {
			tablistTask.cancel();
		}

		tablistTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateTablist, 20L, 20L);
	}

	public void updateTablist() {
		// Get all global boosters
		List<Booster> globalBoosters = plugin.getBoosterManager().getGlobalBoosters();

		// Process for each online player
		for (Player player : Bukkit.getOnlinePlayers()) {
			UUID playerId = player.getUniqueId();

			// Get player's personal active boosters
			List<Booster> personalBoosters = plugin.getBoosterManager().getActiveBoosters().values().stream()
				.filter(b -> !b.isGlobal() && ((PersonalBooster) b).getPlayerId().equals(playerId))
				.collect(Collectors.toList());

			// Create header for global boosters
			StringBuilder headerBuilder = new StringBuilder();
			headerBuilder.append(plugin.getMessagesConfig().getString("tablist.header")).append("\n");

			if (globalBoosters.isEmpty()) {
				headerBuilder.append(plugin.getMessagesConfig().getString("tablist.no-global-boosters"));
			} else {
				headerBuilder.append(plugin.getMessagesConfig().getString("tablist.global-boosters")).append("\n");

				for (Booster booster : globalBoosters) {
					headerBuilder.append(plugin.getMessagesConfig().getString("tablist.booster-format")
							.replace("%type%", booster.getType().name())
							.replace("%multiplier%", String.valueOf(booster.getMultiplier()))
							.replace("%time%", formatTime(booster.getRemainingTime())))
						.append("\n");
				}
			}

			// Create footer for personal boosters
			StringBuilder footerBuilder = new StringBuilder();

			if (personalBoosters.isEmpty()) {
				footerBuilder.append(plugin.getMessagesConfig().getString("tablist.no-personal-boosters"));
			} else {
				footerBuilder.append(plugin.getMessagesConfig().getString("tablist.personal-boosters")).append("\n");

				for (Booster booster : personalBoosters) {
					footerBuilder.append(plugin.getMessagesConfig().getString("tablist.booster-format")
							.replace("%type%", booster.getType().name())
							.replace("%multiplier%", String.valueOf(booster.getMultiplier()))
							.replace("%time%", formatTime(booster.getRemainingTime())))
						.append("\n");
				}
			}

			// Update tablist for this player
			String header = headerBuilder.toString();
			String footer = footerBuilder.toString();

			// Only update if changed
			String[] current = playerTablist.getOrDefault(playerId, new String[]{"", ""});
			if (!current[0].equals(header) || !current[1].equals(footer)) {
				playerTablist.put(playerId, new String[]{header, footer});

				// Schedule update on main thread
				Player playerRef = player;
				Bukkit.getScheduler().runTask(plugin, () -> {
					playerRef.setPlayerListHeader(header);
					playerRef.setPlayerListFooter(footer);
				});
			}
		}
	}

	public void resetTablist(Player player) {
		playerTablist.remove(player.getUniqueId());
		player.setPlayerListHeader("");
		player.setPlayerListFooter("");
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
