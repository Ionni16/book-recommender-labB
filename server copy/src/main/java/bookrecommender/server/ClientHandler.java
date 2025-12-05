package bookrecommender.server;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestisce un singolo client collegato al server.
 *
 * Comandi supportati:
 *
 *  PING
 *  QUIT
 *
 *  SEARCH_TITLE:query
 *  SEARCH_AUTHOR:autore
 *  SEARCH_AUTHOR_YEAR:autore;anno
 *
 *  LIST_LIBRARIES:userid
 *  SAVE_LIBRARY:userid;nome;id1,id2,id3
 *
 *  ADD_REVIEW:userid;idLibro;stile;contenuto;gradevolezza;originalita;edizione;votoFinale;commento
 *  ADD_SUGGESTION:userid;idLibro;id1,id2,id3
 *
 *  GET_REVIEW_STATS:idLibro
 *  GET_SUGGESTIONS_STATS:idLibro
 *
 *  LOGIN:userid;passwordHash
 *  REGISTER:userid;passwordHash;nome;cognome;codiceFiscale;email
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int clientId;

    private final LibriDao libriDao       = new LibriDao();
    private final UtentiDao utentiDao     = new UtentiDao();
    private final LibrerieDao librerieDao = new LibrerieDao();
    private final ValutazioniDao valDao   = new ValutazioniDao();
    private final ConsigliDao consDao     = new ConsigliDao();

    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        System.out.println("[Client " + clientId + "] Connesso da " + socket.getRemoteSocketAddress());

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"));
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
        ) {
            out.write("OK Benvenuto nel BookRecommenderServer\n");
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                String raw = line.trim();
                if (raw.isEmpty()) continue;

                System.out.println("[Client " + clientId + "] Comando: " + raw);

                if (raw.equalsIgnoreCase("PING")) {
                    out.write("PONG\n");
                    out.flush();
                    continue;
                }
                if (raw.equalsIgnoreCase("QUIT")) {
                    out.write("BYE\n");
                    out.flush();
                    break;
                }

                String upper = raw.toUpperCase(Locale.ITALIAN);

                try {
                    if (upper.startsWith("SEARCH_TITLE:")) {
                        String query = raw.substring("SEARCH_TITLE:".length()).trim();
                        handleSearchTitle(query, out);

                    } else if (upper.startsWith("SEARCH_AUTHOR_YEAR:")) {
                        String payload = raw.substring("SEARCH_AUTHOR_YEAR:".length()).trim();
                        handleSearchAuthorYear(payload, out);

                    } else if (upper.startsWith("SEARCH_AUTHOR:")) {
                        String author = raw.substring("SEARCH_AUTHOR:".length()).trim();
                        handleSearchAuthor(author, out);

                    } else if (upper.startsWith("LIST_LIBRARIES:")) {
                        String userid = raw.substring("LIST_LIBRARIES:".length()).trim();
                        handleListLibraries(userid, out);

                    } else if (upper.startsWith("SAVE_LIBRARY:")) {
                        String payload = raw.substring("SAVE_LIBRARY:".length()).trim();
                        handleSaveLibrary(payload, out);

                    } else if (upper.startsWith("ADD_REVIEW:")) {
                        String payload = raw.substring("ADD_REVIEW:".length()).trim();
                        handleAddReview(payload, out);

                    } else if (upper.startsWith("ADD_SUGGESTION:")) {
                        String payload = raw.substring("ADD_SUGGESTION:".length()).trim();
                        handleAddSuggestion(payload, out);

                    } else if (upper.startsWith("GET_REVIEW_STATS:")) {
                        String payload = raw.substring("GET_REVIEW_STATS:".length()).trim();
                        handleGetReviewStats(payload, out);

                    } else if (upper.startsWith("GET_SUGGESTIONS_STATS:")) {
                        String payload = raw.substring("GET_SUGGESTIONS_STATS:".length()).trim();
                        handleGetSuggestionsStats(payload, out);

                    } else if (upper.startsWith("LOGIN:")) {
                        String payload = raw.substring("LOGIN:".length()).trim();
                        handleLogin(payload, out);

                    } else if (upper.startsWith("REGISTER:")) {
                        String payload = raw.substring("REGISTER:".length()).trim();
                        handleRegister(payload, out);

                    } else {
                        out.write("ERR Comando non riconosciuto.\n");
                        out.flush();
                    }
                } catch (Exception e) {
                    System.err.println("[Client " + clientId + "] Errore comando: " + e.getMessage());
                    out.write("ERR Errore interno\n");
                    out.flush();
                }
            }

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Errore I/O: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("[Client " + clientId + "] Disconnesso.");
        }
    }

    // ==== SEARCH ====

    private void handleSearchTitle(String query, BufferedWriter out) throws IOException, SQLException {
        if (query.isEmpty()) {
            out.write("ERR Query di ricerca vuota.\n");
            out.flush();
            return;
        }
        writeBookResults(libriDao.searchByTitle(query), out);
    }

    private void handleSearchAuthor(String author, BufferedWriter out) throws IOException, SQLException {
        if (author.isEmpty()) {
            out.write("ERR Autore vuoto.\n");
            out.flush();
            return;
        }
        writeBookResults(libriDao.searchByAuthor(author), out);
    }

    private void handleSearchAuthorYear(String payload, BufferedWriter out) throws IOException, SQLException {
        String[] parts = payload.split(";", 2);
        if (parts.length < 2) {
            out.write("ERR Formato per SEARCH_AUTHOR_YEAR non valido. Usa autore;anno\n");
            out.flush();
            return;
        }
        String author = parts[0].trim();
        String annoStr = parts[1].trim();
        int anno;
        try {
            anno = Integer.parseInt(annoStr);
        } catch (NumberFormatException nfe) {
            out.write("ERR Anno non valido\n");
            out.flush();
            return;
        }

        writeBookResults(libriDao.searchByAuthorAndYear(author, anno), out);
    }

    private void writeBookResults(List<String[]> rows, BufferedWriter out) throws IOException {
        out.write("OK SEARCH_RESULTS " + rows.size() + "\n");
        for (String[] row : rows) {
            String id = escape(row[0]);
            String titolo = escape(row[1]);
            String autori = escape(row[2]);
            String anno = escape(row[3]);
            out.write("BOOK;" + id + ";" + titolo + ";" + autori + ";" + anno + "\n");
        }
        out.write("END\n");
        out.flush();
    }

    // ==== LIBRERIE ====

    private void handleListLibraries(String userid, BufferedWriter out) throws IOException, SQLException {
        if (userid.isEmpty()) {
            out.write("ERR LIST_LIBRARIES userid mancante\n");
            out.flush();
            return;
        }

        List<LibrerieDao.LibraryRecord> recs = librerieDao.listLibraries(userid);
        out.write("OK LIBRARIES " + recs.size() + "\n");
        for (LibrerieDao.LibraryRecord r : recs) {
            String idsStr = r.bookIds.stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
            out.write("LIB;" + escape(r.nome) + ";" + idsStr + "\n");
        }
        out.write("END\n");
        out.flush();
    }

    private void handleSaveLibrary(String payload, BufferedWriter out) throws IOException, SQLException {
        String[] parts = payload.split(";", 3);
        if (parts.length < 2) {
            out.write("ERR SAVE_LIBRARY formato non valido\n");
            out.flush();
            return;
        }

        String userid = parts[0].trim();
        String nome   = parts[1].trim();
        String idsPart = parts.length == 3 ? parts[2].trim() : "";

        if (userid.isEmpty() || nome.isEmpty()) {
            out.write("ERR SAVE_LIBRARY userid o nome vuoti\n");
            out.flush();
            return;
        }

        Set<Integer> ids = new LinkedHashSet<>();
        if (!idsPart.isEmpty()) {
            for (String tok : idsPart.split(",")) {
                tok = tok.trim();
                if (tok.isEmpty()) continue;
                try {
                    ids.add(Integer.parseInt(tok));
                } catch (NumberFormatException ignore) {}
            }
        }

        boolean ok = librerieDao.upsertLibrary(userid, nome, ids);
        out.write(ok ? "OK SAVE_LIBRARY\n" : "ERR SAVE_LIBRARY errore DB\n");
        out.flush();
    }

    // ==== VALUTAZIONI ====

    private void handleAddReview(String payload, BufferedWriter out) throws IOException, SQLException {
        String[] parts = payload.split(";", 9);
        if (parts.length < 8) {
            out.write("ERR ADD_REVIEW formato non valido\n");
            out.flush();
            return;
        }

        String userid = parts[0].trim();
        int bookId, stile, contenuto, gradevolezza, originalita, edizione, votoFinale;

        try {
            bookId       = Integer.parseInt(parts[1].trim());
            stile        = Integer.parseInt(parts[2].trim());
            contenuto    = Integer.parseInt(parts[3].trim());
            gradevolezza = Integer.parseInt(parts[4].trim());
            originalita  = Integer.parseInt(parts[5].trim());
            edizione     = Integer.parseInt(parts[6].trim());
            votoFinale   = Integer.parseInt(parts[7].trim());
        } catch (NumberFormatException nfe) {
            out.write("ERR ADD_REVIEW valori numerici non validi\n");
            out.flush();
            return;
        }

        String commento = parts.length == 9 ? parts[8] : "";
        if (commento.length() > 256) {
            out.write("ERR ADD_REVIEW commento troppo lungo\n");
            out.flush();
            return;
        }

        boolean ok = valDao.insertReview(userid, bookId, stile, contenuto,
                gradevolezza, originalita, edizione, votoFinale, commento);

        out.write(ok ? "OK ADD_REVIEW\n" : "ERR ADD_REVIEW\n");
        out.flush();
    }

    private void handleGetReviewStats(String payload, BufferedWriter out) throws IOException, SQLException {
        int bookId;
        try {
            bookId = Integer.parseInt(payload.trim());
        } catch (NumberFormatException nfe) {
            out.write("ERR GET_REVIEW_STATS id non valido\n");
            out.flush();
            return;
        }

        ValutazioniDao.ReviewStats rs = valDao.computeStats(bookId);
        if (rs.count == 0) {
            out.write("OK REVIEW_STATS 0\n");
            out.write("END\n");
            out.flush();
            return;
        }

        String header = String.format(Locale.US,
                "OK REVIEW_STATS %d;%.4f;%.4f;%.4f;%.4f;%.4f;%.4f",
                rs.count,
                rs.mediaStile,
                rs.mediaContenuto,
                rs.mediaGradevolezza,
                rs.mediaOriginalita,
                rs.mediaEdizione,
                rs.mediaVotoFinale);

        out.write(header + "\n");

        StringBuilder dist = new StringBuilder("DIST;");
        boolean first = true;
        for (Map.Entry<Integer,Integer> e : rs.distribuzione.entrySet()) {
            if (!first) dist.append(",");
            first = false;
            dist.append(e.getKey()).append(":").append(e.getValue());
        }
        out.write(dist.toString() + "\n");
        out.write("END\n");
        out.flush();
    }

    // ==== SUGGERIMENTI ====

    private void handleAddSuggestion(String payload, BufferedWriter out) throws IOException, SQLException {
        String[] parts = payload.split(";", 3);
        if (parts.length < 3) {
            out.write("ERR ADD_SUGGESTION formato non valido\n");
            out.flush();
            return;
        }

        String userid = parts[0].trim();
        int bookId;
        try {
            bookId = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException nfe) {
            out.write("ERR ADD_SUGGESTION idLibro non valido\n");
            out.flush();
            return;
        }

        List<Integer> ids = new ArrayList<>();
        String idsPart = parts[2].trim();
        if (!idsPart.isEmpty()) {
            for (String tok : idsPart.split(",")) {
                tok = tok.trim();
                if (tok.isEmpty()) continue;
                try {
                    ids.add(Integer.parseInt(tok));
                } catch (NumberFormatException ignore) {}
            }
        }

        boolean ok = consDao.insertSuggestion(userid, bookId, ids);
        out.write(ok ? "OK ADD_SUGGESTION\n" : "ERR ADD_SUGGESTION\n");
        out.flush();
    }

    private void handleGetSuggestionsStats(String payload, BufferedWriter out) throws IOException, SQLException {
        int bookId;
        try {
            bookId = Integer.parseInt(payload.trim());
        } catch (NumberFormatException nfe) {
            out.write("ERR GET_SUGGESTIONS_STATS id non valido\n");
            out.flush();
            return;
        }

        Map<Integer,Integer> map = consDao.computeSuggestions(bookId);
        out.write("OK SUGGESTIONS " + map.size() + "\n");
        for (Map.Entry<Integer,Integer> e : map.entrySet()) {
            out.write("SUG;" + e.getKey() + ";" + e.getValue() + "\n");
        }
        out.write("END\n");
        out.flush();
    }

    // ==== LOGIN / REGISTER ====

    private void handleLogin(String payload, BufferedWriter out) throws IOException, SQLException {
        String[] parts = payload.split(";", 2);
        if (parts.length < 2) {
            out.write("ERR LOGIN formato non valido\n");
            out.flush();
            return;
        }

        String userid = parts[0].trim();
        String passwordHash = parts[1].trim();

        boolean ok = utentiDao.verificaLogin(userid, passwordHash);
        out.write(ok ? "OK LOGIN\n" : "ERR LOGIN\n");
        out.flush();
    }

    private void handleRegister(String payload, BufferedWriter out) throws IOException, SQLException {
        String[] parts = payload.split(";", 6);
        if (parts.length < 6) {
            out.write("ERR REGISTER dati insufficienti\n");
            out.flush();
            return;
        }

        String userid        = parts[0].trim();
        String passwordHash  = parts[1].trim();
        String nome          = parts[2].trim();
        String cognome       = parts[3].trim();
        String codiceFiscale = parts[4].trim();
        String email         = parts[5].trim();

        if (utentiDao.esisteUserid(userid)) {
            out.write("ERR REGISTER userid esistente\n");
            out.flush();
            return;
        }

        boolean ok = utentiDao.registraNuovoUtente(
                userid, passwordHash, nome, cognome, codiceFiscale, email);
        out.write(ok ? "OK REGISTER\n" : "ERR REGISTER errore DB\n");
        out.flush();
    }

    // ==== UTIL ====

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\n"," ").replace("\r"," ").replace(";", ",");
    }
}
