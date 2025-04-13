package wavebooster.models;

import java.util.UUID;

public class GlobalBooster extends Booster {
	private final UUID activatorId; // The player who activated this booster

	public GlobalBooster(UUID id, BoosterType type, int multiplier, long duration, UUID activatorId) {
		super(id, type, multiplier, duration);
		this.activatorId = activatorId;
	}

	public UUID getActivatorId() {
		return activatorId;
	}

	@Override
	public boolean isGlobal() {
		return true;
	}

	@Override
	public String toString() {
		return "GlobalBooster{" +
			"id=" + id +
			", type=" + type +
			", multiplier=" + multiplier +
			", duration=" + duration +
			", active=" + active +
			", activatorId=" + activatorId +
			'}';
	}
}
