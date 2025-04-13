// BoosterAPIImpl.java
package wavebooster.api;

import wavebooster.WaveBooster;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoosterAPIImpl implements BoosterAPI {
	private final WaveBooster plugin;

	public BoosterAPIImpl(WaveBooster plugin) {
		this.plugin = plugin;
	}

	@Override
	public List<Booster> getPlayerBoosters(UUID playerId) {
		return plugin.getBoosterManager().getPlayerBoosters(playerId);
	}

	@Override
	public Map<BoosterType, Integer> getActiveBoostersForPlayer(UUID playerId) {
		return plugin.getBoosterManager().getActiveBoostersForPlayer(playerId);
	}

	@Override
	public void givePlayerBooster(UUID playerId, BoosterType type, int multiplier, long duration, boolean isGlobal) {
		plugin.getBoosterManager().givePlayerBooster(playerId, type, multiplier, duration, isGlobal);
	}

	@Override
	public void removePlayerBooster(UUID playerId, UUID boosterId) {
		plugin.getBoosterManager().removePlayerBooster(playerId, boosterId);
	}

	@Override
	public boolean activateBooster(UUID playerId, UUID boosterId) {
		return plugin.getBoosterManager().activateBooster(playerId, boosterId);
	}

	@Override
	public List<Booster> getGlobalBoosters() {
		return plugin.getBoosterManager().getGlobalBoosters();
	}

	@Override
	public int getActiveMultiplier(UUID playerId, BoosterType type) {
		Map<BoosterType, Integer> boosters = getActiveBoostersForPlayer(playerId);
		return boosters.getOrDefault(type, 1);
	}
}
