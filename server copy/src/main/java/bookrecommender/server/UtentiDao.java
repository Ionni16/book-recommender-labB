package bookrecommender.server;

import java.sql.*;

/**
 * DAO per la tabella utenti_registrati.
 * La password qui è sempre l'hash (SHA-256) che il client invia.
 */
public class UtentiDao {

    public boolean esisteUserid(String userid) throws SQLException {
        String sql = "SELECT 1 FROM utenti_registrati WHERE userid = ?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean registraNuovoUtente(String userid,
                                       String passwordHash,
                                       String nome,
                                       String cognome,
                                       String codiceFiscale,
                                       String email) throws SQLException {

        String sql = """
                INSERT INTO utenti_registrati
                (userid, password_hash, nome, cognome, codice_fiscale, email)
                VALUES (?,?,?,?,?,?)
                """;

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userid);
            ps.setString(2, passwordHash);
            ps.setString(3, nome);
            ps.setString(4, cognome);
            ps.setString(5, codiceFiscale);
            ps.setString(6, email);
            int n = ps.executeUpdate();
            return n == 1;
        }
    }

    /**
     * Verifica login confrontando l'hash della password.
     */
    public boolean verificaLogin(String userid, String passwordHash) throws SQLException {
        String sql = "SELECT password_hash FROM utenti_registrati WHERE userid = ?";
        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String stored = rs.getString(1);
                return stored != null && stored.equals(passwordHash);
            }
        }
    }
}
