package bookrecommender.server;

import java.sql.*;
import java.util.*;

public class ConsigliDao {

    /**
     * Inserisce suggerimenti per un libro.
     *
     * Regole:
     *  - l'utente deve aver fatto login (gestito a livello GUI / ServerApi)
     *  - il libro di partenza (bookId) deve essere presente in almeno una libreria dell'utente
     *  - si accettano al massimo 3 ID distinti, diversi da bookId
     *  - ognuno dei libri suggeriti deve essere presente in almeno una libreria dell'utente
     *  - niente duplicati (userid, bookId, id_suggerito)
     *
     * Ritorna:
     *   true  = almeno un suggerimento valido inserito
     *   false = nessun ID valido (o libro di partenza non in libreria)
     */
    public boolean insertSuggestion(String userid, int bookId, List<Integer> suggeriti)
            throws SQLException {

        // 1) pulizia base della lista
        LinkedHashSet<Integer> cleanSet = new LinkedHashSet<>();
        for (Integer id : suggeriti) {
            if (id == null) continue;
            if (id <= 0) continue;
            if (id == bookId) continue;     // non si può suggerire il libro stesso
            cleanSet.add(id);
        }

        List<Integer> cleaned = new ArrayList<>(cleanSet);
        if (cleaned.size() > 3) {
            cleaned = cleaned.subList(0, 3);
        }
        if (cleaned.isEmpty()) {
            // niente da inserire
            return false;
        }

        try (Connection conn = DbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 2) tutti i libri che l'utente possiede
                Set<Integer> owned = loadOwnedBookIds(conn, userid);
                if (owned.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                // 2a) il libro di partenza deve essere posseduto
                if (!owned.contains(bookId)) {
                    conn.rollback();
                    return false;
                }

                // 2b) tieni solo i suggeriti che l'utente possiede
                List<Integer> valid = new ArrayList<>();
                for (Integer id : cleaned) {
                    if (owned.contains(id)) {
                        valid.add(id);
                    }
                }

                // se dopo il filtro non resta niente, niente suggerimenti validi
                if (valid.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                // 3) inserimento dei suggerimenti validi
                String sql = """
                        INSERT INTO consigli_libri(userid, id_libro, id_suggerito)
                        VALUES (?,?,?)
                        ON CONFLICT (userid, id_libro, id_suggerito) DO NOTHING
                        """;

                int inserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Integer id : valid) {
                        ps.setString(1, userid);
                        ps.setInt(2, bookId);
                        ps.setInt(3, id);
                        ps.addBatch();
                    }
                    int[] res = ps.executeBatch();
                    for (int r : res) {
                        // per ON CONFLICT DO NOTHING, r può essere 0 o 1
                        if (r > 0) inserted += r;
                    }
                }

                conn.commit();
                // true anche se i suggerimenti erano già presenti: la richiesta è "valida"
                return inserted > 0 || !valid.isEmpty();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Libri presenti in almeno una libreria dell'utente. */
    private Set<Integer> loadOwnedBookIds(Connection conn, String userid) throws SQLException {
        Set<Integer> out = new HashSet<>();
        String sql = """
                SELECT DISTINCT ll.id_libro
                FROM librerie l
                JOIN librerie_libri ll ON ll.id_libreria = l.id
                WHERE l.userid = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getInt(1));
                }
            }
        }
        return out;
    }

    /** Statistiche: libro suggerito -> numero di utenti che l'hanno suggerito per bookId. */
    public Map<Integer,Integer> computeSuggestions(int bookId) throws SQLException {
        Map<Integer,Integer> map = new LinkedHashMap<>();

        String sql = """
                SELECT id_suggerito, COUNT(*) AS c
                FROM consigli_libri
                WHERE id_libro = ?
                GROUP BY id_suggerito
                ORDER BY c DESC
                """;

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("id_suggerito"), rs.getInt("c"));
                }
            }
        }

        return map;
    }
}
