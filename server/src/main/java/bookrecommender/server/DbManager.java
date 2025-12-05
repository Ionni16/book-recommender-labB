package bookrecommender.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe di utilità per ottenere connessioni JDBC a PostgreSQL.
 */
public class DbManager {

    static {
        try {
            // Carica il driver PostgreSQL (di solito non strettamente necessario con JDBC 4+,
            // ma esplicitarlo non fa male).
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL non trovato. Assicurati che il jar sia nel classpath.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                ServerConfig.DB_URL,
                ServerConfig.DB_USER,
                ServerConfig.DB_PASS
        );
    }
}
