package wavebooster.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import wavebooster.WaveBooster;

public class NBTUtils {
	private final WaveBooster plugin;

	public NBTUtils(WaveBooster plugin) {
		this.plugin = plugin;
	}

	public ItemStack setString(ItemStack item, String key, String value) {
		if (item == null) return null;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;

		NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
		PersistentDataContainer container = meta.getPersistentDataContainer();
		container.set(namespacedKey, PersistentDataType.STRING, value);

		item.setItemMeta(meta);
		return item;
	}

	public String getString(ItemStack item, String key) {
		if (item == null) return null;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;

		NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
		PersistentDataContainer container = meta.getPersistentDataContainer();

		return container.get(namespacedKey, PersistentDataType.STRING);
	}

	public boolean hasKey(ItemStack item, String key) {
		if (item == null) return false;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;

		NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
		PersistentDataContainer container = meta.getPersistentDataContainer();

		return container.has(namespacedKey, PersistentDataType.STRING);
	}

	public ItemStack setInt(ItemStack item, String key, int value) {
		if (item == null) return null;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;

		NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
		PersistentDataContainer container = meta.getPersistentDataContainer();
		container.set(namespacedKey, PersistentDataType.INTEGER, value);

		item.setItemMeta(meta);
		return item;
	}

	public int getInt(ItemStack item, String key) {
		if (item == null) return 0;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return 0;

		NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
		PersistentDataContainer container = meta.getPersistentDataContainer();

		Integer value = container.get(namespacedKey, PersistentDataType.INTEGER);
		return value != null ? value : 0;
	}
}
