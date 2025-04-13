package wavebooster.models;

import java.util.UUID;

public abstract class Booster {
	protected final UUID id;
	protected final BoosterType type;
	protected final int multiplier;
	protected long duration; // Duration in seconds
	protected long startTime; // Timestamp when the booster was activated
	protected boolean active;

	public Booster(UUID id, BoosterType type, int multiplier, long duration) {
		this.id = id;
		this.type = type;
		this.multiplier = multiplier;
		this.duration = duration;
		this.active = false;
	}

	public UUID getId() {
		return id;
	}

	public BoosterType getType() {
		return type;
	}

	public int getMultiplier() {
		return multiplier;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getStartTime() {
		return startTime;
	}

	public void activate() {
		this.active = true;
		this.startTime = System.currentTimeMillis() / 1000; // Convert to seconds
	}

	public boolean isActive() {
		return active;
	}

	public void deactivate() {
		this.active = false;
	}

	public long getRemainingTime() {
		if (!active) return 0;
		long currentTime = System.currentTimeMillis() / 1000;
		long endTime = startTime + duration;
		return Math.max(0, endTime - currentTime);
	}

	public boolean isExpired() {
		return getRemainingTime() <= 0;
	}

	public abstract boolean isGlobal();
}
