package bookrecommender.server;

import java.sql.*;
import java.util.Map;
import java.util.TreeMap;

public class ValutazioniDao {

    /** Inserisce una valutazione.
     *  Ritorna:
     *    true  = inserita
     *    false = rifiutata (libro non presente nelle librerie dell’utente
     *                       oppure valutazione già esistente)
     */
    public boolean insertReview(
            String userid,
            int bookId,
            int stile,
            int contenuto,
            int gradevolezza,
            int originalita,
            int edizione,
            int votoFinale,
            String commento
    ) throws SQLException {

        try (Connection conn = DbManager.getConnection()) {
            // 1) controllo che il libro sia in almeno una libreria dell’utente
            if (!isBookInUserLibraries(conn, userid, bookId)) {
                return false;
            }

            // 2) controllo duplicato: stesso utente + stesso libro
            try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT 1 FROM valutazioni_libri WHERE userid = ? AND id_libro = ?")) {
                chk.setString(1, userid);
                chk.setInt(2, bookId);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) {
                        return false; // già presente
                    }
                }
            }

            // 3) inserimento
            String sql = """
                    INSERT INTO valutazioni_libri
                    (userid, id_libro, stile, contenuto, gradevolezza,
                     originalita, edizione, voto_finale, commento)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userid);
                ps.setInt(2, bookId);
                ps.setInt(3, stile);
                ps.setInt(4, contenuto);
                ps.setInt(5, gradevolezza);
                ps.setInt(6, originalita);
                ps.setInt(7, edizione);
                ps.setInt(8, votoFinale);
                ps.setString(9, commento);
                ps.executeUpdate();
            }
        }

        return true;
    }

    private boolean isBookInUserLibraries(Connection conn, String userid, int bookId) throws SQLException {
        String sql = """
                SELECT 1
                FROM librerie l
                JOIN librerie_libri ll ON ll.id_libreria = l.id
                WHERE l.userid = ? AND ll.id_libro = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userid);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ==== Statistiche ====

    public static class ReviewStats {
        public int count;
        public double mediaStile, mediaContenuto, mediaGradevolezza,
                mediaOriginalita, mediaEdizione, mediaVotoFinale;
        public Map<Integer,Integer> distribuzione = new TreeMap<>();
    }

    public ReviewStats computeStats(int bookId) throws SQLException {
        ReviewStats s = new ReviewStats();

        try (Connection conn = DbManager.getConnection()) {
            String aggSql = """
                    SELECT COUNT(*)        AS c,
                           AVG(stile)      AS ms,
                           AVG(contenuto)  AS mc,
                           AVG(gradevolezza) AS mg,
                           AVG(originalita)  AS mo,
                           AVG(edizione)     AS me,
                           AVG(voto_finale)  AS mv
                    FROM valutazioni_libri
                    WHERE id_libro = ?
                    """;

            try (PreparedStatement ps = conn.prepareStatement(aggSql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        s.count = rs.getInt("c");
                        if (s.count == 0) {
                            return s;
                        }
                        s.mediaStile        = rs.getDouble("ms");
                        s.mediaContenuto    = rs.getDouble("mc");
                        s.mediaGradevolezza = rs.getDouble("mg");
                        s.mediaOriginalita  = rs.getDouble("mo");
                        s.mediaEdizione     = rs.getDouble("me");
                        s.mediaVotoFinale   = rs.getDouble("mv");
                    }
                }
            }

            if (s.count == 0) return s;

            String distSql = """
                    SELECT voto_finale, COUNT(*) AS c
                    FROM valutazioni_libri
                    WHERE id_libro = ?
                    GROUP BY voto_finale
                    ORDER BY voto_finale
                    """;

            try (PreparedStatement ps = conn.prepareStatement(distSql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        s.distribuzione.put(rs.getInt("voto_finale"),
                                            rs.getInt("c"));
                    }
                }
            }
        }

        return s;
    }
}
