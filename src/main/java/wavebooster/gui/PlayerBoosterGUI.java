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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerBoosterGUI implements Listener {
	private final WaveBooster plugin;
	private final String guiTitle;

	public PlayerBoosterGUI(WaveBooster plugin) {
		this.plugin = plugin;
		this.guiTitle = ChatColor.translateAlternateColorCodes('&',
			plugin.getConfig().getString("gui.player-title", "&8Your Boosters"));

		// Register events
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void openGUI(Player player) {
		UUID playerId = player.getUniqueId();
		List<Booster> boosters = plugin.getBoosterManager().getPlayerBoosters(playerId);

		// Calculate required inventory size (multiple of 9)
		int size = 9 * (int) Math.ceil(Math.max(boosters.size(), 1) / 9.0) + 9;
		size = Math.min(size, 54); // Max size is 6 rows (54 slots)

		Inventory inventory = Bukkit.createInventory(null, size, guiTitle);

		// Fill GUI with player boosters
		int slot = 0;
		for (Booster booster : boosters) {
			if (slot >= size - 9) break; // Leave the bottom row for controls

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

			// Add activation instructions
			lore.add("");
			lore.add(ChatColor.YELLOW + "Click to activate this booster!");

			meta.setLore(lore);
			item.setItemMeta(meta);

			// Store booster ID in the item's NBT
			item = plugin.getNBTUtils().setString(item, "boosterId", booster.getId().toString());

			inventory.setItem(slot++, item);
		}

		// Add bottom row controls
		ItemStack closeButton = new ItemStack(Material.BARRIER);
		ItemMeta closeMeta = closeButton.getItemMeta();
		closeMeta.setDisplayName(ChatColor.RED + "Close");
		closeButton.setItemMeta(closeMeta);

		inventory.setItem(size - 5, closeButton);

		player.openInventory(inventory);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		if (event.getView().getTitle().equals(guiTitle)) {
			event.setCancelled(true);

			Player player = (Player) event.getWhoClicked();
			ItemStack clickedItem = event.getCurrentItem();

			if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

			// Check if it's a booster item
			if (plugin.getNBTUtils().hasKey(clickedItem, "boosterId")) {
				String boosterIdStr = plugin.getNBTUtils().getString(clickedItem, "boosterId");
				UUID boosterId = UUID.fromString(boosterIdStr);

				// Try to activate the booster
				boolean activated = plugin.getBoosterManager().activateBooster(player.getUniqueId(), boosterId);

				if (activated) {
					// Close the inventory and reopen it to refresh
					player.closeInventory();
					plugin.getServer().getScheduler().runTaskLater(plugin, () -> openGUI(player), 1L);
				}
			}
			// Close button
			else if (clickedItem.getType() == Material.BARRIER) {
				player.closeInventory();
			}
		}
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
