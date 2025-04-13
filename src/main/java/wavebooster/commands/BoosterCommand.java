package wavebooster.commands;

import wavebooster.WaveBooster;
import wavebooster.gui.PlayerBoosterGUI;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoosterCommand implements CommandExecutor {
	private final WaveBooster plugin;
	private final PlayerBoosterGUI playerBoosterGUI;

	public BoosterCommand(WaveBooster plugin) {
		this.plugin = plugin;
		this.playerBoosterGUI = new PlayerBoosterGUI(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
			return true;
		}

		Player player = (Player) sender;
		UUID playerId = player.getUniqueId();

		// No arguments - open GUI
		if (args.length == 0) {
			playerBoosterGUI.openGUI(player);
			return true;
		}

		// Check subcommands
		String subCommand = args[0].toLowerCase();

		switch (subCommand) {
			case "list":
				listBoosters(player);
				break;
			case "active":
				showActiveBoosters(player);
				break;
			case "help":
				showHelp(player);
				break;
			default:
				player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /boosters help for available commands.");
				break;
		}

		return true;
	}

	private void listBoosters(Player player) {
		UUID playerId = player.getUniqueId();
		List<Booster> boosters = plugin.getBoosterManager().getPlayerBoosters(playerId);

		if (boosters.isEmpty()) {
			player.sendMessage(ChatColor.RED + "You don't have any boosters!");
			return;
		}

		player.sendMessage(ChatColor.GOLD + "=== Your Boosters ===");

		int index = 1;
		for (Booster booster : boosters) {
			String type = booster.getType() == BoosterType.XP ? "XP" : "Drop";
			String global = booster.isGlobal() ? ChatColor.GOLD + "[GLOBAL]" : ChatColor.AQUA + "[PERSONAL]";

			player.sendMessage(ChatColor.YELLOW + "" + index + ". " +
				ChatColor.GREEN + type + " Booster " +
				ChatColor.GRAY + "(" + booster.getMultiplier() + "x) " +
				global + ChatColor.GRAY + " - " +
				formatTime(booster.getDuration()));

			index++;
		}

		player.sendMessage(ChatColor.YELLOW + "Use /boosters to open the booster GUI.");
	}

	private void showActiveBoosters(Player player) {
		UUID playerId = player.getUniqueId();
		Map<BoosterType, Integer> activeMultipliers = plugin.getBoosterManager().getActiveBoostersForPlayer(playerId);

		// Get active personal boosters
		List<Booster> personalBoosters = plugin.getBoosterManager().getActivePersonalBoosters(playerId);
		// Get global boosters
		List<Booster> globalBoosters = plugin.getBoosterManager().getGlobalBoosters();

		player.sendMessage(ChatColor.GOLD + "=== Active Boosters ===");

		// Show active personal boosters
		if (!personalBoosters.isEmpty()) {
			player.sendMessage(ChatColor.AQUA + "Personal Boosters:");

			for (Booster booster : personalBoosters) {
				String type = booster.getType().name();

				player.sendMessage(ChatColor.YELLOW + "- " +
					ChatColor.GREEN + type + " Booster " +
					ChatColor.GRAY + "(" + booster.getMultiplier() + "x) " +
					ChatColor.GRAY + "- " +
					formatTime(booster.getRemainingTime()) + " remaining");
			}
		}

		// Show active global boosters
		if (!globalBoosters.isEmpty()) {
			player.sendMessage(ChatColor.GOLD + "Global Boosters:");

			for (Booster booster : globalBoosters) {
				String type = booster.getType().name();

				player.sendMessage(ChatColor.YELLOW + "- " +
					ChatColor.GREEN + type + " Booster " +
					ChatColor.GRAY + "(" + booster.getMultiplier() + "x) " +
					ChatColor.GRAY + "- " +
					formatTime(booster.getRemainingTime()) + " remaining");
			}
		}

		// Show effective multipliers
		player.sendMessage(ChatColor.GOLD + "Effective Multipliers:");

		for (BoosterType type : BoosterType.values()) {
			int multiplier = activeMultipliers.getOrDefault(type, 1);

			if (multiplier > 1) {
				player.sendMessage(ChatColor.YELLOW + "- " +
					ChatColor.GREEN + type.name() + ": " +
					ChatColor.GRAY + multiplier + "x");
			} else {
				player.sendMessage(ChatColor.YELLOW + "- " +
					ChatColor.GREEN + type.name() + ": " +
					ChatColor.GRAY + "None active");
			}
		}
	}

	private void showHelp(Player player) {
		player.sendMessage(ChatColor.GOLD + "=== WaveBooster Help ===");
		player.sendMessage(ChatColor.YELLOW + "/boosters " + ChatColor.GRAY + "- Open the booster GUI");
		player.sendMessage(ChatColor.YELLOW + "/boosters list " + ChatColor.GRAY + "- List all your boosters");
		player.sendMessage(ChatColor.YELLOW + "/boosters active " + ChatColor.GRAY + "- Show active boosters and multipliers");
		player.sendMessage(ChatColor.YELLOW + "/boosters help " + ChatColor.GRAY + "- Show this help menu");
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
