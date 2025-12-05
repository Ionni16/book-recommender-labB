package bookrecommender.server;

import java.sql.*;
import java.util.*;

/**
 * Accesso alle tabelle:
 *   - librerie       (id SERIAL PK, userid, nome)
 *   - librerie_libri (id_libreria, id_libro)
 */
public class LibrerieDao {

    public static class LibraryRecord {
        public final int idLibreria;
        public final String userid;
        public final String nome;
        public final Set<Integer> bookIds;

        public LibraryRecord(int idLibreria, String userid, String nome, Set<Integer> bookIds) {
            this.idLibreria = idLibreria;
            this.userid = userid;
            this.nome = nome;
            this.bookIds = bookIds;
        }
    }

    /** Restituisce tutte le librerie di un utente, con gli ID libri associati. */
    public List<LibraryRecord> listLibraries(String userid) throws SQLException {
        List<LibraryRecord> result = new ArrayList<>();

        try (Connection conn = DbManager.getConnection()) {
            // 1) prendo le librerie
            String sqlLibs = """
                    SELECT id, nome
                    FROM librerie
                    WHERE userid = ?
                    ORDER BY nome
                    """;

            Map<Integer,LibraryRecord> map = new LinkedHashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(sqlLibs)) {
                ps.setString(1, userid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idLib = rs.getInt("id");
                        String nome = rs.getString("nome");
                        LibraryRecord rec = new LibraryRecord(
                                idLib, userid, nome, new LinkedHashSet<>());
                        map.put(idLib, rec);
                    }
                }
            }

            if (map.isEmpty()) {
                return new ArrayList<>();
            }

            // 2) per ciascuna libreria, carico i libri associati
            String sqlBooks = """
                    SELECT id_libreria, id_libro
                    FROM librerie_libri
                    WHERE id_libreria = ANY (?)
                    ORDER BY id_libreria, id_libro
                    """;

            // creo un array di id_libreria per la query
            Integer[] idsArray = map.keySet().toArray(new Integer[0]);
            try (PreparedStatement ps = conn.prepareStatement(sqlBooks)) {
                Array sqlArray = conn.createArrayOf("integer", idsArray);
                ps.setArray(1, sqlArray);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idLib = rs.getInt("id_libreria");
                        int idLibro = rs.getInt("id_libro");
                        LibraryRecord rec = map.get(idLib);
                        if (rec != null) {
                            rec.bookIds.add(idLibro);
                        }
                    }
                }
            }

            result.addAll(map.values());
        }

        return result;
    }

    /**
     * Crea o aggiorna una libreria per (userid, nome), impostando esattamente
     * l’insieme degli ID libri passato.
     */
    public boolean upsertLibrary(String userid, String nome, Set<Integer> bookIds) throws SQLException {
        if (userid == null || userid.isBlank() || nome == null || nome.isBlank()) {
            return false;
        }

        try (Connection conn = DbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idLibreria = findOrCreateLibrary(conn, userid, nome);

                // svuoto i vecchi libri
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM librerie_libri WHERE id_libreria = ?")) {
                    del.setInt(1, idLibreria);
                    del.executeUpdate();
                }

                // inserisco i nuovi libri
                if (bookIds != null && !bookIds.isEmpty()) {
                    String insSql = "INSERT INTO librerie_libri(id_libreria, id_libro) VALUES (?, ?)";
                    try (PreparedStatement ins = conn.prepareStatement(insSql)) {
                        for (Integer idLibro : bookIds) {
                            if (idLibro == null) continue;
                            ins.setInt(1, idLibreria);
                            ins.setInt(2, idLibro);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Trova l’ID della libreria (userid, nome) oppure la crea e restituisce il nuovo ID. */
    private int findOrCreateLibrary(Connection conn, String userid, String nome) throws SQLException {
        String selSql = "SELECT id FROM librerie WHERE userid = ? AND nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(selSql)) {
            ps.setString(1, userid);
            ps.setString(2, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insSql = "INSERT INTO librerie(userid, nome) VALUES(?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(insSql)) {
            ps.setString(1, userid);
            ps.setString(2, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new SQLException("Impossibile creare/ottenere l'id della libreria");
    }
}
