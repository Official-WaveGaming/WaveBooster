package wavebooster.events;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobLootDropEvent; // Geändert!
import io.lumine.mythic.core.drops.LootBag;
import wavebooster.WaveBooster;
import wavebooster.models.BoosterType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import javax.sql.rowset.spi.SyncFactoryException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static javax.sql.rowset.spi.SyncFactory.getLogger;

public class MythicMobsListener implements Listener {
	private final WaveBooster plugin;

	public MythicMobsListener(WaveBooster plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMythicMobLootDrop(MythicMobLootDropEvent event) { // Geändert!
		if (event.getKiller() instanceof Player) {
			Player player = (Player) event.getKiller();
			UUID playerId = player.getUniqueId();

			// Get the drop multiplier for this player
			int multiplier = plugin.getBoosterAPI().getActiveMultiplier(playerId, BoosterType.DROP);

			if (multiplier > 1) {
				// Create copies of the dropped items based on the multiplier
				Collection<ItemStack> drops = (Collection<ItemStack>) event.getDrops();
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
					LootBag add = event.getDrops().add(item);
				}

				// Send message to player about boosted drops
				String message = plugin.getMessagesConfig().getString("booster.drops-boosted")
					.replace("%multiplier%", String.valueOf(multiplier));
				player.sendMessage(message);
			}
		}
	}

	@EventHandler
	public void onMythicMobDeath(MythicMobDeathEvent event) throws SyncFactoryException {
		getLogger().info("MythicMobDeathEvent triggered!");

		// Liste alle verfügbaren Methoden auf
		for (Method method : event.getClass().getMethods()) {
			getLogger().info("Available method: " + method.getName() + " returns " + method.getReturnType().getName());
		}
	}
}
