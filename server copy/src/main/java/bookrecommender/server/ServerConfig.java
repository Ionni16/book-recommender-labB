package bookrecommender.server;

/**
 * Configurazione centralizzata per server e database.
 * MODIFICA qui URL, utente e password del tuo PostgreSQL.
 */
public class ServerConfig {

    // Porta TCP su cui il server ascolta i client
    public static final int SERVER_PORT = 5555;

    // Parametri DB - ADATTALI al tuo ambiente!
    public static final String DB_URL  = "jdbc:postgresql://localhost:5432/bookrecommender";
    public static final String DB_USER = "postgres";
    public static final String DB_PASS = "project";

    private ServerConfig() {
        // utility class
    }
}
