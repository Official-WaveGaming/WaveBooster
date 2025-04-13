package wavebooster.events;

import wavebooster.WaveBooster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
	private final WaveBooster plugin;

	public PlayerListener(WaveBooster plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		// Load player boosters
		plugin.getBoosterManager().loadPlayerBoosters(player.getUniqueId());

		// Schedule tablist update
		plugin.getServer().getScheduler().runTaskLater(plugin,
			() -> plugin.getTablistManager().updateTablist(), 20L);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// Save player boosters
		plugin.getBoosterManager().savePlayerBoosters(player.getUniqueId());

		// Reset tablist
		plugin.getTablistManager().resetTablist(player);
	}
}
