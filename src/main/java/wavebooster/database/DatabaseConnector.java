package wavebooster.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnector {
	private final String host;
	private final int port;
	private final String database;
	private final String username;
	private final String password;
	private Connection connection;
	private final Logger logger = Logger.getLogger(getClass().getName());

	public DatabaseConnector(String host, int port, String database, String username, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;

		try {
			connect();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to connect to the database", e);
		}
	}

	public Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			connect();
		}
		return connection;
	}

	private void connect() throws SQLException {
		String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
		connection = DriverManager.getConnection(url, username, password);
	}

	public void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Failed to close database connection", e);
			}
		}
	}
}
