package wavebooster.gui;

import wavebooster.WaveBooster;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;
import wavebooster.models.GlobalBooster;
import wavebooster.models.PersonalBooster;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class AdminBoosterGUI implements Listener {
	private final WaveBooster plugin;
	private final String mainGuiTitle;
	private final String playerGuiTitle;
	private final String giveGuiTitle;
	private final String globalGuiTitle;

	public AdminBoosterGUI(WaveBooster plugin) {
		this.plugin = plugin;
		this.mainGuiTitle = ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-title", "&8Admin Boosters"));
		this.playerGuiTitle = ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-player-title", "&8Admin: %player%'s Boosters"));
		this.giveGuiTitle = ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-give-title", "&8Give Booster to %player%"));
		this.globalGuiTitle = ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-global-title", "&8Admin: Global Boosters"));

		// Register events
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void openMainGUI(Player admin) {
		Inventory inventory = Bukkit.createInventory(null, 45, mainGuiTitle);

		// Add online players
		int slot = 0;
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (slot >= 36) break; // Leave bottom row for controls

			ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
			meta.setOwningPlayer(player);
			meta.setDisplayName(ChatColor.YELLOW + player.getName());

			List<String> lore = new ArrayList<>();
			lore.add(ChatColor.GRAY + "Click to view and manage");
			lore.add(ChatColor.GRAY + "this player's boosters");
			meta.setLore(lore);

			playerHead.setItemMeta(meta);

			// Store player UUID
			playerHead = plugin.getNBTUtils().setString(playerHead, "playerId", player.getUniqueId().toString());

			inventory.setItem(slot++, playerHead);
		}

		// Add global boosters button
		ItemStack globalButton = new ItemStack(Material.NETHER_STAR);
		ItemMeta globalMeta = globalButton.getItemMeta();
		globalMeta.setDisplayName(ChatColor.GOLD + "Global Boosters");

		List<String> globalLore = new ArrayList<>();
		globalLore.add(ChatColor.GRAY + "Click to view and manage");
		globalLore.add(ChatColor.GRAY + "all global boosters");
		globalMeta.setLore(globalLore);

		globalButton.setItemMeta(globalMeta);

		inventory.setItem(40, globalButton);

		// Close button
		ItemStack closeButton = new ItemStack(Material.BARRIER);
		ItemMeta closeMeta = closeButton.getItemMeta();
		closeMeta.setDisplayName(ChatColor.RED + "Close");
		closeButton.setItemMeta(closeMeta);

		inventory.setItem(44, closeButton);

		admin.openInventory(inventory);
	}

	public void openPlayerGUI(Player admin, UUID targetPlayerId) {
		Player targetPlayer = Bukkit.getPlayer(targetPlayerId);
		if (targetPlayer == null) {
			admin.sendMessage(ChatColor.RED + "Player not found!");
			return;
		}

		String title = playerGuiTitle.replace("%player%", targetPlayer.getName());
		Inventory inventory = Bukkit.createInventory(null, 54, title);

		// Add player's boosters
		List<Booster> boosters = plugin.getBoosterManager().getPlayerBoosters(targetPlayerId);
		int slot = 0;

		for (Booster booster : boosters) {
			if (slot >= 45) break; // Leave bottom row for controls

			Material material;
			if (booster.getType() == BoosterType.XP) {
				material = Material.EXPERIENCE_BOTTLE;
			} else {
				material = Material.CHEST;
			}
			ItemStack item = new ItemStack(material);
			ItemMeta meta = item.getItemMeta();

			String displayName = ChatColor.translateAlternateColorCodes('&',
				plugin.getMessagesConfig().getString("gui.booster-name")
					.replace("%type%", booster.getType().name())
					.replace("%multiplier%", String.valueOf(booster.getMultiplier())));

			meta.setDisplayName(displayName);

			List<String> lore = new ArrayList<>();
			for (String loreLine : plugin.getMessagesConfig().getStringList("gui.booster-lore")) {
				lore.add(ChatColor.translateAlternateColorCodes('&', loreLine
					.replace("%multiplier%", String.valueOf(booster.getMultiplier()))
					.replace("%duration%", formatTime(booster.getDuration()))
					.replace("%type%", booster.isGlobal() ?
						plugin.getMessagesConfig().getString("booster.global") :
						plugin.getMessagesConfig().getString("booster.personal"))));
			}

			// Add admin actions
			lore.add("");
			lore.add(ChatColor.YELLOW + "Left-click to activate");
			lore.add(ChatColor.RED + "Right-click to remove");

			meta.setLore(lore);
			item.setItemMeta(meta);

			// Store booster ID and player ID
			item = plugin.getNBTUtils().setString(item, "boosterId", booster.getId().toString());
			item = plugin.getNBTUtils().setString(item, "playerId", targetPlayerId.toString());

			inventory.setItem(slot++, item);
		}

		// Add "Give Booster" button
		ItemStack giveButton = new ItemStack(Material.EMERALD);
		ItemMeta giveMeta = giveButton.getItemMeta();
		giveMeta.setDisplayName(ChatColor.GREEN + "Give Booster");

		List<String> giveLore = new ArrayList<>();
		giveLore.add(ChatColor.GRAY + "Click to give a new booster");
		giveLore.add(ChatColor.GRAY + "to " + targetPlayer.getName());
		giveMeta.setLore(giveLore);

		giveButton.setItemMeta(giveMeta);
		giveButton = plugin.getNBTUtils().setString(giveButton, "playerId", targetPlayerId.toString());

		inventory.setItem(49, giveButton);

		// Back button
		ItemStack backButton = new ItemStack(Material.ARROW);
		ItemMeta backMeta = backButton.getItemMeta();
		backMeta.setDisplayName(ChatColor.YELLOW + "Back to Main Menu");
		backButton.setItemMeta(backMeta);

		inventory.setItem(45, backButton);

		// Close button
		ItemStack closeButton = new ItemStack(Material.BARRIER);
		ItemMeta closeMeta = closeButton.getItemMeta();
		closeMeta.setDisplayName(ChatColor.RED + "Close");
		closeButton.setItemMeta(closeMeta);

		inventory.setItem(53, closeButton);

		admin.openInventory(inventory);
	}
	public void openGiveBoosterGUI(Player admin, UUID targetPlayerId) {
		Player targetPlayer = Bukkit.getPlayer(targetPlayerId);
		if (targetPlayer == null) {
			admin.sendMessage(ChatColor.RED + "Player not found!");
			return;
		}

		String title = giveGuiTitle.replace("%player%", targetPlayer.getName());
		Inventory inventory = Bukkit.createInventory(null, 45, title);

		// XP Boosters
		ItemStack xp2x = createBoosterItem(Material.EXPERIENCE_BOTTLE, "&aXP Booster &7(2x)",
			BoosterType.XP, 2, 3600, false);
		ItemStack xp3x = createBoosterItem(Material.EXPERIENCE_BOTTLE, "&aXP Booster &7(3x)",
			BoosterType.XP, 3, 3600, false);
		ItemStack xpGlobal2x = createBoosterItem(Material.EXPERIENCE_BOTTLE, "&aXP Booster &7(2x) &6[GLOBAL]",
			BoosterType.XP, 2, 1800, true);
		ItemStack xpGlobal3x = createBoosterItem(Material.EXPERIENCE_BOTTLE, "&aXP Booster &7(3x) &6[GLOBAL]",
			BoosterType.XP, 3, 1800, true);

		// Drop Boosters
		ItemStack drop2x = createBoosterItem(Material.CHEST, "&aDrop Booster &7(2x)",
			BoosterType.DROP, 2, 3600, false);
		ItemStack drop3x = createBoosterItem(Material.CHEST, "&aDrop Booster &7(3x)",
			BoosterType.DROP, 3, 3600, false);
		ItemStack dropGlobal2x = createBoosterItem(Material.CHEST, "&aDrop Booster &7(2x) &6[GLOBAL]",
			BoosterType.DROP, 2, 1800, true);
		ItemStack dropGlobal3x = createBoosterItem(Material.CHEST, "&aDrop Booster &7(3x) &6[GLOBAL]",
			BoosterType.DROP, 3, 1800, true);

		// Add booster items
		inventory.setItem(10, xp2x);
		inventory.setItem(11, xp3x);
		inventory.setItem(12, xpGlobal2x);
		inventory.setItem(13, xpGlobal3x);
		inventory.setItem(19, drop2x);
		inventory.setItem(20, drop3x);
		inventory.setItem(21, dropGlobal2x);
		inventory.setItem(22, dropGlobal3x);

		// Store player ID for all booster items
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			if (item != null && (item.getType() == Material.EXPERIENCE_BOTTLE || item.getType() == Material.CHEST)) {
				inventory.setItem(i, plugin.getNBTUtils().setString(item, "playerId", targetPlayerId.toString()));
			}
		}

		// Back button
		ItemStack backButton = new ItemStack(Material.ARROW);
		ItemMeta backMeta = backButton.getItemMeta();
		backMeta.setDisplayName(ChatColor.YELLOW + "Back to Player Menu");
		backMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Return to " + targetPlayer.getName() + "'s boosters"));
		backButton.setItemMeta(backMeta);
		backButton = plugin.getNBTUtils().setString(backButton, "playerId", targetPlayerId.toString());

		inventory.setItem(36, backButton);

		// Close button
		ItemStack closeButton = new ItemStack(Material.BARRIER);
		ItemMeta closeMeta = closeButton.getItemMeta();
		closeMeta.setDisplayName(ChatColor.RED + "Close");
		closeButton.setItemMeta(closeMeta);

		inventory.setItem(44, closeButton);

		admin.openInventory(inventory);
	}
	public void openGlobalBoostersGUI(Player admin) {
		Inventory inventory = Bukkit.createInventory(null, 54, globalGuiTitle);

		// List all active global boosters
		List<Booster> globalBoosters = plugin.getBoosterManager().getGlobalBoosters();
		int slot = 0;

		for (Booster booster : globalBoosters) {
			if (slot >= 45) break; // Leave bottom row for controls

			Material material;
			if (booster.getType() == BoosterType.XP) {
				material = Material.EXPERIENCE_BOTTLE;
			} else {
				material = Material.CHEST;
			}

			ItemStack item = new ItemStack(material);
			ItemMeta meta = item.getItemMeta();

			String displayName = ChatColor.translateAlternateColorCodes('&',
				plugin.getMessagesConfig().getString("gui.active-booster-name")
					.replace("%type%", booster.getType().name())
					.replace("%multiplier%", String.valueOf(booster.getMultiplier())));

			meta.setDisplayName(displayName);

			List<String> lore = new ArrayList<>();
			for (String loreLine : plugin.getMessagesConfig().getStringList("gui.active-booster-lore")) {
				lore.add(ChatColor.translateAlternateColorCodes('&', loreLine
					.replace("%multiplier%", String.valueOf(booster.getMultiplier()))
					.replace("%remaining%", formatTime(booster.getRemainingTime()))
					.replace("%activator%", Bukkit.getOfflinePlayer(((GlobalBooster) booster).getActivatorId()).getName())));
			}

			// Add cancel option
			lore.add("");
			lore.add(ChatColor.RED + "Click to cancel this global booster");

			meta.setLore(lore);
			item.setItemMeta(meta);

			// Store booster ID
			item = plugin.getNBTUtils().setString(item, "boosterId", booster.getId().toString());

			inventory.setItem(slot++, item);
		}

		// Activate global booster button
		ItemStack activateButton = new ItemStack(Material.NETHER_STAR);
		ItemMeta activateMeta = activateButton.getItemMeta();
		activateMeta.setDisplayName(ChatColor.GOLD + "Activate Global Booster");

		List<String> activateLore = new ArrayList<>();
		activateLore.add(ChatColor.GRAY + "Click to activate a new");
		activateLore.add(ChatColor.GRAY + "global booster for all players");
		activateMeta.setLore(activateLore);

		activateButton.setItemMeta(activateMeta);

		inventory.setItem(49, activateButton);

		// Back button
		ItemStack backButton = new ItemStack(Material.ARROW);
		ItemMeta backMeta = backButton.getItemMeta();
		backMeta.setDisplayName(ChatColor.YELLOW + "Back to Main Menu");
		backButton.setItemMeta(backMeta);

		inventory.setItem(45, backButton);

		// Close button
		ItemStack closeButton = new ItemStack(Material.BARRIER);
		ItemMeta closeMeta = closeButton.getItemMeta();
		closeMeta.setDisplayName(ChatColor.RED + "Close");
		closeButton.setItemMeta(closeMeta);

		inventory.setItem(53, closeButton);

		admin.openInventory(inventory);
	}

	// Methode zum Öffnen des Global Booster Selection GUI
	public void openGlobalBoosterSelectionGUI(Player admin) {
		String title = ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-global-select-title", "&8Activate Global Booster"));
		Inventory inventory = Bukkit.createInventory(null, 36, title);

		// XP Boosters
		ItemStack xp2x = createBoosterItem(Material.EXPERIENCE_BOTTLE, "&aXP Booster &7(2x)",
			BoosterType.XP, 2, 1800, true);
		ItemStack xp3x = createBoosterItem(Material.EXPERIENCE_BOTTLE, "&aXP Booster &7(3x)",
			BoosterType.XP, 3, 1800, true);

		// Drop Boosters
		ItemStack drop2x = createBoosterItem(Material.CHEST, "&aDrop Booster &7(2x)",
			BoosterType.DROP, 2, 1800, true);
		ItemStack drop3x = createBoosterItem(Material.CHEST, "&aDrop Booster &7(3x)",
			BoosterType.DROP, 3, 1800, true);

		// Add booster items
		inventory.setItem(11, xp2x);
		inventory.setItem(12, xp3x);
		inventory.setItem(14, drop2x);
		inventory.setItem(15, drop3x);

		// Back button
		ItemStack backButton = new ItemStack(Material.ARROW);
		ItemMeta backMeta = backButton.getItemMeta();
		backMeta.setDisplayName(ChatColor.YELLOW + "Back to Global Boosters");
		backButton.setItemMeta(backMeta);

		inventory.setItem(27, backButton);

		// Close button
		ItemStack closeButton = new ItemStack(Material.BARRIER);
		ItemMeta closeMeta = closeButton.getItemMeta();
		closeMeta.setDisplayName(ChatColor.RED + "Close");
		closeButton.setItemMeta(closeMeta);

		inventory.setItem(35, closeButton);

		admin.openInventory(inventory);
	}
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		Player player = (Player) event.getWhoClicked();
		String title = event.getView().getTitle();

		// Main admin GUI
		if (title.equals(mainGuiTitle)) {
			event.setCancelled(true);

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

			// Player head - open player GUI
			if (clickedItem.getType() == Material.PLAYER_HEAD && plugin.getNBTUtils().hasKey(clickedItem, "playerId")) {
				String playerIdStr = plugin.getNBTUtils().getString(clickedItem, "playerId");
				UUID playerId = UUID.fromString(playerIdStr);

				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openPlayerGUI(player, playerId), 1L);
			}
			// Global boosters button
			else if (clickedItem.getType() == Material.NETHER_STAR) {
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGlobalBoostersGUI(player), 1L);
			}
			// Close button
			else if (clickedItem.getType() == Material.BARRIER) {
				player.closeInventory();
			}
		}
		// Player boosters GUI
		else if (title.startsWith(ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-player-title", "&8Admin: ").split("%player%")[0]))) {
			event.setCancelled(true);

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

			// Booster item (activate or remove)
			if ((clickedItem.getType() == Material.EXPERIENCE_BOTTLE || clickedItem.getType() == Material.CHEST)
				&& plugin.getNBTUtils().hasKey(clickedItem, "boosterId")) {
				String boosterIdStr = plugin.getNBTUtils().getString(clickedItem, "boosterId");
				String playerIdStr = plugin.getNBTUtils().getString(clickedItem, "playerId");
				UUID boosterId = UUID.fromString(boosterIdStr);
				UUID playerId = UUID.fromString(playerIdStr);

				// Left click - activate
				if (event.isLeftClick()) {
					boolean activated = plugin.getBoosterManager().activateBooster(playerId, boosterId);
					if (activated) {
						player.sendMessage(ChatColor.GREEN + "Booster activated!");
					} else {
						player.sendMessage(ChatColor.RED + "Failed to activate booster!");
					}
				}
				// Right click - remove
				else if (event.isRightClick()) {
					plugin.getBoosterManager().removePlayerBooster(playerId, boosterId);
					player.sendMessage(ChatColor.GREEN + "Booster removed!");
				}

				// Refresh GUI
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openPlayerGUI(player, playerId), 1L);
			}
			// Give booster button
			else if (clickedItem.getType() == Material.EMERALD && plugin.getNBTUtils().hasKey(clickedItem, "playerId")) {
				String playerIdStr = plugin.getNBTUtils().getString(clickedItem, "playerId");
				UUID playerId = UUID.fromString(playerIdStr);

				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGiveBoosterGUI(player, playerId), 1L);
			}
			// Back button
			else if (clickedItem.getType() == Material.ARROW) {
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMainGUI(player), 1L);
			}
			// Close button
			else if (clickedItem.getType() == Material.BARRIER) {
				player.closeInventory();
			}
		}
		// Give booster GUI
		else if (title.startsWith(ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-give-title", "&8Give Booster to ").split("%player%")[0]))) {
			event.setCancelled(true);

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

			// Booster item
			if ((clickedItem.getType() == Material.EXPERIENCE_BOTTLE || clickedItem.getType() == Material.CHEST)
				&& plugin.getNBTUtils().hasKey(clickedItem, "playerId")) {
				String playerIdStr = plugin.getNBTUtils().getString(clickedItem, "playerId");
				UUID playerId = UUID.fromString(playerIdStr);

				// Get booster properties from the item
				BoosterType type = clickedItem.getType() == Material.EXPERIENCE_BOTTLE ? BoosterType.XP : BoosterType.DROP;
				int multiplier = plugin.getNBTUtils().getInt(clickedItem, "multiplier");
				long duration = plugin.getNBTUtils().getInt(clickedItem, "duration");
				boolean isGlobal = plugin.getNBTUtils().getInt(clickedItem, "global") == 1;

				// Give the booster
				plugin.getBoosterManager().givePlayerBooster(playerId, type, multiplier, duration, isGlobal);
				player.sendMessage(ChatColor.GREEN + "Booster given successfully!");

				// Return to player GUI
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openPlayerGUI(player, playerId), 1L);
			}
			// Back button
			else if (clickedItem.getType() == Material.ARROW && plugin.getNBTUtils().hasKey(clickedItem, "playerId")) {
				String playerIdStr = plugin.getNBTUtils().getString(clickedItem, "playerId");
				UUID playerId = UUID.fromString(playerIdStr);

				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openPlayerGUI(player, playerId), 1L);
			}
			// Close button
			else if (clickedItem.getType() == Material.BARRIER) {
				player.closeInventory();
			}
		}
		// Global boosters GUI
		else if (title.equals(globalGuiTitle)) {
			event.setCancelled(true);

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

			// Active global booster (cancel)
			if ((clickedItem.getType() == Material.EXPERIENCE_BOTTLE || clickedItem.getType() == Material.CHEST)
				&& plugin.getNBTUtils().hasKey(clickedItem, "boosterId")) {
				String boosterIdStr = plugin.getNBTUtils().getString(clickedItem, "boosterId");
				UUID boosterId = UUID.fromString(boosterIdStr);

				// Remove from active boosters
				plugin.getBoosterManager().removeActiveBooster(boosterId);
				player.sendMessage(ChatColor.GREEN + "Global booster cancelled!");

				// Refresh GUI
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGlobalBoostersGUI(player), 1L);
			}
			// Activate global booster button
			else if (clickedItem.getType() == Material.NETHER_STAR) {
				// For simplicity, we'll just create a global booster selection similar to the give menu
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGlobalBoosterSelectionGUI(player), 1L);
			}
			// Back button
			else if (clickedItem.getType() == Material.ARROW) {
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMainGUI(player), 1L);
			}
			// Close button
			else if (clickedItem.getType() == Material.BARRIER) {
				player.closeInventory();
			}
		}
		// Global booster selection GUI (similar to give booster GUI)
		else if (title.startsWith(ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.admin-global-select-title", "&8Activate Global Booster")))) {
			event.setCancelled(true);

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

			// Booster item
			if ((clickedItem.getType() == Material.EXPERIENCE_BOTTLE || clickedItem.getType() == Material.CHEST)) {
				// Get booster properties from the item
				BoosterType type = clickedItem.getType() == Material.EXPERIENCE_BOTTLE ? BoosterType.XP : BoosterType.DROP;
				int multiplier = plugin.getNBTUtils().getInt(clickedItem, "multiplier");
				long duration = plugin.getNBTUtils().getInt(clickedItem, "duration");

				// Create and activate global booster
				UUID boosterId = UUID.randomUUID();
				GlobalBooster booster = new GlobalBooster(boosterId, type, multiplier, duration, player.getUniqueId());
				booster.activate();

				plugin.getBoosterManager().addActiveBooster(booster);
				plugin.getSqlManager().saveActiveBooster(booster);

				player.sendMessage(ChatColor.GREEN + "Global booster activated!");

				// Broadcast to all players
				String message = plugin.getMessagesConfig().getString("booster.global-activated")
					.replace("%type%", type.name())
					.replace("%multiplier%", String.valueOf(multiplier))
					.replace("%time%", formatTime(duration))
					.replace("%player%", player.getName());

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
				}

				// Return to global boosters GUI
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGlobalBoostersGUI(player), 1L);
			}
			// Back button
			else if (clickedItem.getType() == Material.ARROW) {
				player.closeInventory();
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGlobalBoostersGUI(player), 1L);
			}
			// Close button
			else if (clickedItem.getType() == Material.BARRIER) {
				player.closeInventory();
			}
		}
	}

	// Helper method to create booster items for the give menu
	private ItemStack createBoosterItem(Material material, String name, BoosterType type, int multiplier, long duration, boolean isGlobal) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();

		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Type: " + type.name());
		lore.add(ChatColor.GRAY + "Multiplier: " + multiplier + "x");
		lore.add(ChatColor.GRAY + "Duration: " + formatTime(duration));
		lore.add(ChatColor.GRAY + "Global: " + (isGlobal ? "Yes" : "No"));
		lore.add("");
		lore.add(ChatColor.YELLOW + "Click to give this booster");

		meta.setLore(lore);
		item.setItemMeta(meta);

		// Store booster properties
		item = plugin.getNBTUtils().setInt(item, "multiplier", multiplier);
		item = plugin.getNBTUtils().setInt(item, "duration", (int) duration);
		item = plugin.getNBTUtils().setInt(item, "global", isGlobal ? 1 : 0);

		return item;
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
