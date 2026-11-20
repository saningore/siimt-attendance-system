package attendance;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Opens a JDBC connection using config.properties.
public class Db {
    private static String url;
    private static String user;
    private static String password;

    static void init() throws Exception {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream("config.properties"), StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read config.properties. Start ClassRoll from the project folder.");
        }

        String host = props.getProperty("db.host", "localhost").trim();
        String port = props.getProperty("db.port", "3306").trim();
        String name = props.getProperty("db.name", "school_attendance").trim();
        user = props.getProperty("db.user", "root").trim();
        password = props.getProperty("db.password", "");
        url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection con = open()) {
            con.createStatement().execute("SELECT 1");
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL.");
            System.out.println("Database: " + name + " on " + host + " as " + user);
            System.out.println("MySQL said: " + e.getMessage());
            System.out.println();
            System.out.println("Check that:");
            System.out.println("1. MySQL is running");
            System.out.println("2. You have run database/schema.sql");
            System.out.println("3. db.password in config.properties matches Workbench");
            throw e;
        }
        System.out.println("Connected to MySQL database " + name + ".");
    }

    static Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
