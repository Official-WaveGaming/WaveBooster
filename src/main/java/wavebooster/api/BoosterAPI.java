// BoosterAPI.java
package wavebooster.api;

import wavebooster.WaveBooster;
import wavebooster.models.Booster;
import wavebooster.models.BoosterType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BoosterAPI {
	/**
	 * Gets all boosters owned by a player
	 * @param playerId UUID of the player
	 * @return List of boosters
	 */
	List<Booster> getPlayerBoosters(UUID playerId);

	/**
	 * Gets all active boosters (personal and global) affecting a player
	 * @param playerId UUID of the player
	 * @return Map with booster types and their combined multipliers
	 */
	Map<BoosterType, Integer> getActiveBoostersForPlayer(UUID playerId);

	/**
	 * Gives a player a new booster
	 * @param playerId UUID of the player
	 * @param type Type of booster (XP or DROP)
	 * @param multiplier Multiplier value (e.g., 2 for 2x)
	 * @param duration Duration in seconds
	 * @param isGlobal Whether this is a global booster
	 */
	void givePlayerBooster(UUID playerId, BoosterType type, int multiplier, long duration, boolean isGlobal);

	/**
	 * Removes a booster from a player
	 * @param playerId UUID of the player
	 * @param boosterId UUID of the booster
	 */
	void removePlayerBooster(UUID playerId, UUID boosterId);

	/**
	 * Activates a booster for a player
	 * @param playerId UUID of the player
	 * @param boosterId UUID of the booster
	 * @return true if successfully activated, false otherwise
	 */
	boolean activateBooster(UUID playerId, UUID boosterId);

	/**
	 * Gets all global boosters currently active
	 * @return List of global boosters
	 */
	List<Booster> getGlobalBoosters();

	/**
	 * Checks if a specific booster type is active for a player
	 * @param playerId UUID of the player
	 * @param type Type of booster to check
	 * @return Multiplier value (1 if none active)
	 */
	int getActiveMultiplier(UUID playerId, BoosterType type);
}
