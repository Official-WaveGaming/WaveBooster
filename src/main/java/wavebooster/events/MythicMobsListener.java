package wavebooster.events;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDropItemEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import wavebooster.WaveBooster;
import wavebooster.models.BoosterType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MythicMobsListener implements Listener {
	private final WaveBooster plugin;

	public MythicMobsListener(WaveBooster plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMythicMobDropItem(MythicMobDropItemEvent event) {
		if (event.getKiller() instanceof Player) {
			Player player = (Player) event.getKiller();
			UUID playerId = player.getUniqueId();

			// Get the drop multiplier for this player
			int multiplier = plugin.getBoosterAPI().getActiveMultiplier(playerId, BoosterType.DROP);

			if (multiplier > 1) {
				// Create copies of the dropped items based on the multiplier
				Collection<ItemStack> drops = event.getDrops();
				List<ItemStack> additionalDrops = new ArrayList<>();

				// Multiply the drops (multiplier - 1 times since the original drop is already included)
				for (int i = 1; i < multiplier; i++) {
					for (ItemStack item : drops) {
						if (item != null) {
							additionalDrops.add(item.clone());
						}
					}
				}

				// Add the additional drops
				for (ItemStack item : additionalDrops) {
					event.getDrops().add(item);
				}

				// Send message to player about boosted drops
				String message = plugin.getMessagesConfig().getString("booster.drops-boosted")
					.replace("%multiplier%", String.valueOf(multiplier));
				player.sendMessage(message);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMythicMobDeath(MythicMobDeathEvent event) {
		if (event.getKiller() instanceof Player) {
			Player player = (Player) event.getKiller();
			UUID playerId = player.getUniqueId();

			// Get the XP multiplier for this player
			int multiplier = plugin.getBoosterAPI().getActiveMultiplier(playerId, BoosterType.XP);

			if (multiplier > 1) {
				// Multiply the XP
				double originalXp = event.getExp();
				double boostedXp = originalXp * multiplier;
				event.setExp(boostedXp);

				// Send message to player about boosted XP
				String message = plugin.getMessagesConfig().getString("booster.xp-boosted")
					.replace("%multiplier%", String.valueOf(multiplier));
				player.sendMessage(message);
			}
		}
	}
}
