package wavebooster;

import wavebooster.api.BoosterAPI;
import wavebooster.api.BoosterAPIImpl;
import wavebooster.commands.AdminBoosterCommand;
import wavebooster.commands.BoosterCommand;
import wavebooster.database.DatabaseConnector;
import wavebooster.database.SQLManager;
import wavebooster.events.MythicMobsListener;
import wavebooster.events.PlayerListener;
import wavebooster.managers.BoosterManager;
import wavebooster.managers.TablistManager;
import wavebooster.utils.NBTUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class WaveBooster extends JavaPlugin {

	private static WaveBooster instance;
	private FileConfiguration config;
	private FileConfiguration messagesConfig;
	private DatabaseConnector databaseConnector;
	private SQLManager sqlManager;
	private BoosterManager boosterManager;
	private TablistManager tablistManager;
	private BoosterAPI boosterAPI;
	private NBTUtils nbtUtils;

	@Override
	public void onEnable() {
		instance = this;

		// Load or create config files
		saveDefaultConfig();
		config = getConfig();
		createMessagesFile();

		// Initialize database
		String host = config.getString("database.host");
		int port = config.getInt("database.port");
		String database = config.getString("database.database");
		String username = config.getString("database.username");
		String password = config.getString("database.password");

		databaseConnector = new DatabaseConnector(host, port, database, username, password);
		sqlManager = new SQLManager(this);

		// Initialize managers
		boosterManager = new BoosterManager(this);
		tablistManager = new TablistManager(this);

		// Initialize API
		boosterAPI = new BoosterAPIImpl(this);

		// Initialize utils
		nbtUtils = new NBTUtils(this);

		// Register commands
		getCommand("boosters").setExecutor(new BoosterCommand(this));
		getCommand("adminbooster").setExecutor(new AdminBoosterCommand(this));

		// Register events
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);

		// Check if MythicMobs is present
		if (pm.getPlugin("MythicMobs") != null) {
			pm.registerEvents(new MythicMobsListener(this), this);
			getLogger().info("MythicMobs integration enabled!");
		} else {
			getLogger().warning("MythicMobs not found! Some features will not work properly.");
		}

		// Start boosters task
		boosterManager.startBoosterTask();

		getLogger().info("MythicBoosters has been enabled!");
	}

	@Override
	public void onDisable() {
		// Save all active boosters
		boosterManager.saveAllBoosters();

		// Close database connection
		databaseConnector.closeConnection();

		getLogger().info("MythicBoosters has been disabled!");
	}

	private void createMessagesFile() {
		File messagesFile = new File(getDataFolder(), "messages.yml");
		if (!messagesFile.exists()) {
			saveResource("messages.yml", false);
		}
		messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
	}

	public void reloadMessages() {
		File messagesFile = new File(getDataFolder(), "messages.yml");
		messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
	}

	public void saveMessages() {
		File messagesFile = new File(getDataFolder(), "messages.yml");
		try {
			messagesConfig.save(messagesFile);
		} catch (IOException e) {
			getLogger().severe("Could not save messages.yml: " + e.getMessage());
		}
	}

	public static WaveBooster getInstance() {
		return instance;
	}

	public FileConfiguration getMessagesConfig() {
		return messagesConfig;
	}

	public DatabaseConnector getDatabaseConnector() {
		return databaseConnector;
	}

	public SQLManager getSqlManager() {
		return sqlManager;
	}

	public BoosterManager getBoosterManager() {
		return boosterManager;
	}

	public TablistManager getTablistManager() {
		return tablistManager;
	}

	public BoosterAPI getBoosterAPI() {
		return boosterAPI;
	}

	public NBTUtils getNBTUtils() {
		return nbtUtils;
	}
}
