package bookrecommender.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point del server Book Recommender (Lab B).
 * - Verifica la connessione al DB PostgreSQL
 * - Apre un ServerSocket e gestisce i client con Thread separati
 */
public class ServerMain {

    public static void main(String[] args) {
        System.out.println("=== BookRecommender Server (Lab B) ===");

        // 1) Test connessione DB
        try (Connection conn = DbManager.getConnection()) {
            System.out.println("[OK] Connessione al database riuscita: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("[ERRORE] Impossibile connettersi al database:");
            e.printStackTrace();
            System.err.println("Controlla URL, utente e password in ServerConfig.");
            return; // senza DB non ha senso avviare il server
        }

        // 2) ServerSocket per i client
        AtomicInteger clientCounter = new AtomicInteger(1);

        try (ServerSocket serverSocket = new ServerSocket(ServerConfig.SERVER_PORT)) {
            System.out.println("[OK] Server in ascolto sulla porta " + ServerConfig.SERVER_PORT);

            while (true) {
                Socket client = serverSocket.accept();
                int id = clientCounter.getAndIncrement();
                ClientHandler handler = new ClientHandler(client, id);
                Thread t = new Thread(handler, "ClientHandler-" + id);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("[ERRORE] Problema con il ServerSocket: " + e.getMessage());
        }
    }
}
