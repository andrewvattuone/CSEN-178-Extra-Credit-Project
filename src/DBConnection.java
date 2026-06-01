import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Change these to match your MySQL setup
    private static final String URL =
            "jdbc:mysql://localhost:3306/DBAIHardwareBenchmark";

    private static final String USER = "dbproject";

    private static final String PASSWORD = "dbproject123";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }
        catch (SQLException e) {
            System.out.println("Database connection failed.");
            e.printStackTrace();
            return null;
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {

            if (conn != null) {
                System.out.println("Connected to DBAIHardwareBenchmark successfully!");
            }
            else {
                System.out.println("Connection failed.");
            }

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

