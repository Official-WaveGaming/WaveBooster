package wavebooster.commands;

import wavebooster.WaveBooster;
import wavebooster.gui.AdminBoosterGUI;
import wavebooster.models.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminBoosterCommand implements CommandExecutor, TabCompleter {
	private final WaveBooster plugin;
	private final AdminBoosterGUI adminBoosterGUI;

	public AdminBoosterCommand(WaveBooster plugin) {
		this.plugin = plugin;
		this.adminBoosterGUI = new AdminBoosterGUI(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
			return true;
		}

		Player player = (Player) sender;

		// Check permission
		if (!player.hasPermission("wavebooster.admin")) {
			player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
			return true;
		}

		// No arguments - open admin GUI
		if (args.length == 0) {
			adminBoosterGUI.openMainGUI(player);
			return true;
		}

		// Check subcommands
		String subCommand = args[0].toLowerCase();

		switch (subCommand) {
			case "give":
				handleGiveCommand(player, args);
				break;
			case "remove":
				handleRemoveCommand(player, args);
				break;
			case "global":
				handleGlobalCommand(player, args);
				break;
			case "reload":
				handleReloadCommand(player);
				break;
			case "help":
				showHelp(player);
				break;
			default:
				player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /adminbooster help for available commands.");
				break;
		}

		return true;
	}

	private void handleGiveCommand(Player player, String[] args) {
		// /adminbooster give <player> <type> <multiplier> <duration> [global]
		if (args.length < 5) {
			player.sendMessage(ChatColor.RED + "Usage: /adminbooster give <player> <type> <multiplier> <duration> [global]");
			return;
		}

		// Get player
		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			player.sendMessage(ChatColor.RED + "Player not found!");
			return;
		}

		// Get booster type
		BoosterType type;
		try {
			type = BoosterType.valueOf(args[2].toUpperCase());
		} catch (IllegalArgumentException e) {
			player.sendMessage(ChatColor.RED + "Invalid booster type! Use XP or DROP.");
			return;
		}

		// Get multiplier
		int multiplier;
		try {
			multiplier = Integer.parseInt(args[3]);
			if (multiplier < 1) {
				player.sendMessage(ChatColor.RED + "Multiplier must be greater than 0!");
				return;
			}
		} catch (NumberFormatException e) {
			player.sendMessage(ChatColor.RED + "Invalid multiplier! Must be a number.");
			return;
		}

		// Get duration (in seconds)
		long duration;
		try {
			duration = Long.parseLong(args[4]);
			if (duration <= 0) {
				player.sendMessage(ChatColor.RED + "Duration must be greater than 0!");
				return;
			}
		} catch (NumberFormatException e) {
			player.sendMessage(ChatColor.RED + "Invalid duration! Must be a number (in seconds).");
			return;
		}

		// Check if global
		boolean isGlobal = args.length >= 6 && args[5].equalsIgnoreCase("global");

		// Give the booster
		plugin.getBoosterManager().givePlayerBooster(target.getUniqueId(), type, multiplier, duration, isGlobal);

		player.sendMessage(ChatColor.GREEN + "Booster given to " + target.getName() + "!");
		target.sendMessage(ChatColor.GREEN + "You received a " + type.name() + " Booster (" + multiplier + "x) from an admin!");
	}

	private void handleRemoveCommand(Player player, String[] args) {
		// /adminbooster remove <player> <boosterId>
		if (args.length < 3) {
			player.sendMessage(ChatColor.RED + "Usage: /adminbooster remove <player> <boosterId>");
			return;
		}

		// Get player
		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			player.sendMessage(ChatColor.RED + "Player not found!");
			return;
		}

		// Get booster ID
		UUID boosterId;
		try {
			boosterId = UUID.fromString(args[2]);
		} catch (IllegalArgumentException e) {
			player.sendMessage(ChatColor.RED + "Invalid booster ID format!");
			return;
		}

		// Remove the booster
		plugin.getBoosterManager().removePlayerBooster(target.getUniqueId(), boosterId);

		player.sendMessage(ChatColor.GREEN + "Booster removed from " + target.getName() + "!");
	}

	private void handleGlobalCommand(Player player, String[] args) {
		// /adminbooster global <action> [type] [multiplier] [duration]
		if (args.length < 2) {
			player.sendMessage(ChatColor.RED + "Usage: /adminbooster global <action> [type] [multiplier] [duration]");
			return;
		}

		String action = args[1].toLowerCase();

		switch (action) {
			case "list":
				showGlobalBoosters(player);
				break;
			case "activate":
				if (args.length < 5) {
					player.sendMessage(ChatColor.RED + "Usage: /adminbooster global activate <type> <multiplier> <duration>");
					return;
				}
				activateGlobalBooster(player, args);
				break;
			case "cancel":
				if (args.length < 3) {
					player.sendMessage(ChatColor.RED + "Usage: /adminbooster global cancel <boosterId>");
					return;
				}
				cancelGlobalBooster(player, args[2]);
				break;
			default:
				player.sendMessage(ChatColor.RED + "Unknown action! Use list, activate, or cancel.");
				break;
		}
	}

	private void showGlobalBoosters(Player player) {
		List<wavebooster.models.Booster> globalBoosters = plugin.getBoosterManager().getGlobalBoosters();

		if (globalBoosters.isEmpty()) {
			player.sendMessage(ChatColor.RED + "There are no active global boosters!");
			return;
		}

		player.sendMessage(ChatColor.GOLD + "=== Active Global Boosters ===");

		for (wavebooster.models.Booster booster : globalBoosters) {
			player.sendMessage(
				ChatColor.YELLOW + "ID: " + booster.getId().toString() + "\n" +
					ChatColor.GREEN + "Type: " + booster.getType().name() + "\n" +
					ChatColor.GREEN + "Multiplier: " + booster.getMultiplier() + "x\n" +
					ChatColor.GREEN + "Remaining: " + formatTime(booster.getRemainingTime())
			);
			player.sendMessage(ChatColor.GRAY + "-------------------");
		}
	}

	private void activateGlobalBooster(Player player, String[] args) {
		// Get booster type
		BoosterType type;
		try {
			type = BoosterType.valueOf(args[2].toUpperCase());
		} catch (IllegalArgumentException e) {
			player.sendMessage(ChatColor.RED + "Invalid booster type! Use XP or DROP.");
			return;
		}

		// Get multiplier
		int multiplier;
		try {
			multiplier = Integer.parseInt(args[3]);
			if (multiplier < 1) {
				player.sendMessage(ChatColor.RED + "Multiplier must be greater than 0!");
				return;
			}
		} catch (NumberFormatException e) {
			player.sendMessage(ChatColor.RED + "Invalid multiplier! Must be a number.");
			return;
		}

		// Get duration (in seconds)
		long duration;
		try {
			duration = Long.parseLong(args[4]);
			if (duration <= 0) {
				player.sendMessage(ChatColor.RED + "Duration must be greater than 0!");
				return;
			}
		} catch (NumberFormatException e) {
			player.sendMessage(ChatColor.RED + "Invalid duration! Must be a number (in seconds).");
			return;
		}

		// Create and activate the global booster
		plugin.getBoosterManager().createAndActivateGlobalBooster(player.getUniqueId(), type, multiplier, duration);

		player.sendMessage(ChatColor.GREEN + "Global " + type.name() + " Booster (" + multiplier + "x) activated!");

		// Broadcast to all players
		String message = plugin.getMessagesConfig().getString("booster.global-activated")
			.replace("%type%", type.name())
			.replace("%multiplier%", String.valueOf(multiplier))
			.replace("%time%", formatTime(duration))
			.replace("%player%", player.getName());

		for (Player p : Bukkit.getOnlinePlayers()) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
		}
	}

	private void cancelGlobalBooster(Player player, String boosterIdStr) {
		// Get booster ID
		UUID boosterId;
		try {
			boosterId = UUID.fromString(boosterIdStr);
		} catch (IllegalArgumentException e) {
			player.sendMessage(ChatColor.RED + "Invalid booster ID format!");
			return;
		}

		// Remove the global booster
		boolean removed = plugin.getBoosterManager().removeActiveBooster(boosterId);

		if (removed) {
			player.sendMessage(ChatColor.GREEN + "Global booster cancelled!");

			// Broadcast to all players
			String message = plugin.getMessagesConfig().getString("booster.global-cancelled")
				.replace("%player%", player.getName());

			for (Player p : Bukkit.getOnlinePlayers()) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
			}
		} else {
			player.sendMessage(ChatColor.RED + "Global booster not found!");
		}
	}

	private void handleReloadCommand(Player player) {
		plugin.reloadConfig();
		plugin.reloadMessages();

		player.sendMessage(ChatColor.GREEN + "WaveBooster configuration reloaded!");
	}

	private void showHelp(Player player) {
		player.sendMessage(ChatColor.GOLD + "=== WaveBooster Admin Help ===");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster " + ChatColor.GRAY + "- Open the admin GUI");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster give <player> <type> <multiplier> <duration> [global] " +
			ChatColor.GRAY + "- Give a booster to a player");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster remove <player> <boosterId> " +
			ChatColor.GRAY + "- Remove a booster from a player");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster global list " +
			ChatColor.GRAY + "- List all active global boosters");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster global activate <type> <multiplier> <duration> " +
			ChatColor.GRAY + "- Activate a global booster");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster global cancel <boosterId> " +
			ChatColor.GRAY + "- Cancel a global booster");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster reload " +
			ChatColor.GRAY + "- Reload plugin configuration");
		player.sendMessage(ChatColor.YELLOW + "/adminbooster help " +
			ChatColor.GRAY + "- Show this help menu");
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			// First argument - subcommands
			String[] subCommands = {"give", "remove", "global", "reload", "help"};
			return filterStartingWith(args[0], subCommands);
		} else if (args.length == 2) {
			// Second argument depends on first argument
			switch (args[0].toLowerCase()) {
				case "give":
				case "remove":
					// Return online players
					return Bukkit.getOnlinePlayers().stream()
						.map(Player::getName)
						.filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
						.collect(Collectors.toList());
				case "global":
					// Return global actions
					String[] globalActions = {"list", "activate", "cancel"};
					return filterStartingWith(args[1], globalActions);
			}
		} else if (args.length == 3) {
			// Third argument depends on first and second arguments
			if (args[0].equalsIgnoreCase("give") ||
				(args[0].equalsIgnoreCase("global") && args[1].equalsIgnoreCase("activate"))) {
				// Booster types
				String[] types = {"XP", "DROP"};
				return filterStartingWith(args[2], types);
			}
		}

		return completions;
	}

	private List<String> filterStartingWith(String prefix, String[] options) {
		return Arrays.stream(options)
			.filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
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
