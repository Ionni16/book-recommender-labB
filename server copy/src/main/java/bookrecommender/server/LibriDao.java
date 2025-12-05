package bookrecommender.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per accedere alla tabella "libri".
 */
public class LibriDao {

    /**
     * Cerca libri il cui titolo contiene (case-insensitive) la stringa data.
     * Restituisce una lista di righe, ognuna con:
     *  [0] = id
     *  [1] = titolo
     *  [2] = autori
     *  [3] = anno (può essere stringa vuota)
     */
    public List<String[]> searchByTitle(String query) throws SQLException {
        String sql = """
                SELECT id, titolo, autori, anno
                FROM libri
                WHERE LOWER(titolo) LIKE LOWER(?)
                ORDER BY id
                LIMIT 100
                """;

        return runLibriQuery(sql, "%" + query + "%");
    }

    /**
     * Cerca libri per autore (qualsiasi autore contenga la stringa data).
     */
    public List<String[]> searchByAuthor(String author) throws SQLException {
        String sql = """
                SELECT id, titolo, autori, anno
                FROM libri
                WHERE LOWER(autori) LIKE LOWER(?)
                ORDER BY id
                LIMIT 100
                """;

        return runLibriQuery(sql, "%" + author + "%");
    }

    /**
     * Cerca libri per autore e anno preciso.
     */
    public List<String[]> searchByAuthorAndYear(String author, int year) throws SQLException {
        String sql = """
                SELECT id, titolo, autori, anno
                FROM libri
                WHERE LOWER(autori) LIKE LOWER(?)
                  AND anno = ?
                ORDER BY id
                LIMIT 100
                """;

        List<String[]> results = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + author + "%");
            ps.setInt(2, year);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idStr = String.valueOf(rs.getInt("id"));
                    String titolo = rs.getString("titolo");
                    String autori = rs.getString("autori");
                    int anno = rs.getInt("anno");
                    String annoStr = rs.wasNull() ? "" : String.valueOf(anno);

                    results.add(new String[]{idStr, titolo, autori, annoStr});
                }
            }
        }

        return results;
    }

    // ---------- UTIL COMUNE ----------

    private List<String[]> runLibriQuery(String sql, String param1) throws SQLException {
        List<String[]> results = new ArrayList<>();

        try (Connection conn = DbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param1);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idStr = String.valueOf(rs.getInt("id"));
                    String titolo = rs.getString("titolo");
                    String autori = rs.getString("autori");
                    int anno = rs.getInt("anno");
                    String annoStr = rs.wasNull() ? "" : String.valueOf(anno);

                    results.add(new String[]{idStr, titolo, autori, annoStr});
                }
            }
        }

        return results;
    }
}
