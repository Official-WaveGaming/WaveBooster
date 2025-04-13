package wavebooster.models;

import java.util.UUID;

public class PersonalBooster extends Booster {
	private final UUID playerId;

	public PersonalBooster(UUID id, BoosterType type, int multiplier, long duration, UUID playerId) {
		super(id, type, multiplier, duration);
		this.playerId = playerId;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	@Override
	public boolean isGlobal() {
		return false;
	}

	@Override
	public String toString() {
		return "PersonalBooster{" +
			"id=" + id +
			", type=" + type +
			", multiplier=" + multiplier +
			", duration=" + duration +
			", active=" + active +
			", playerId=" + playerId +
			'}';
	}
}
