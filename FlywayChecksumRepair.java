import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FlywayChecksumRepair {
	public static void main(String[] args) throws Exception {
		String url = args[0];
		String user = args[1];
		String password = args[2];
		String migrationVersion = args[3];
		String newChecksum = args[4];
		try (Connection connection = DriverManager.getConnection(url, user, password)) {
			try (PreparedStatement select = connection.prepareStatement("SELECT script, checksum FROM flyway_schema_history WHERE version = ?")) {
				select.setString(1, migrationVersion);
				try (ResultSet rs = select.executeQuery()) {
					while (rs.next()) {
						System.out.println("script=" + rs.getString(1) + " checksum=" + rs.getInt(2));
					}
				}
			}
			try (PreparedStatement update = connection.prepareStatement("UPDATE flyway_schema_history SET checksum = ? WHERE version = ?")) {
				update.setInt(1, Integer.parseInt(newChecksum));
				update.setString(2, migrationVersion);
				int updated = update.executeUpdate();
				System.out.println("updated=" + updated);
			}
		}
	}
}
