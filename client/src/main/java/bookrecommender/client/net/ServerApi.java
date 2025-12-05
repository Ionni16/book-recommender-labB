package bookrecommender.client.net;

import bookrecommender.model.Library;
import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.service.AggregationService;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ServerApi {

    public static class BookRow {
        public final int id;
        public final String titolo;
        public final String autori;
        public final Integer anno;

        public BookRow(int id, String titolo, String autori, Integer anno) {
            this.id = id;
            this.titolo = titolo;
            this.autori = autori;
            this.anno = anno;
        }
    }

    private final String host;
    private final int port;

    public ServerApi(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ========= RICERCA LIBRI =========

    public List<BookRow> searchByTitle(String query) throws IOException {
        return searchGeneric("SEARCH_TITLE:" + query);
    }

    public List<BookRow> searchByAuthor(String author) throws IOException {
        return searchGeneric("SEARCH_AUTHOR:" + author);
    }

    public List<BookRow> searchByAuthorAndYear(String author, int year) throws IOException {
        return searchGeneric("SEARCH_AUTHOR_YEAR:" + author + ";" + year);
    }

    private List<BookRow> searchGeneric(String command) throws IOException {
        List<BookRow> out = new ArrayList<>();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             Writer w = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            in.readLine(); // benvenuto

            w.write(command + "\n");
            w.flush();

            String line = in.readLine();
            if (line == null) throw new IOException("Nessuna risposta dal server");
            if (line.startsWith("ERR")) throw new IOException("Errore dal server: " + line);
            if (line.startsWith("OK")) line = in.readLine();

            while (line != null && !"END".equals(line)) {
                if (line.startsWith("BOOK;")) {
                    String[] parts = line.split(";", 5);
                    int id = Integer.parseInt(parts[1]);
                    String titolo = parts[2];
                    String autori = parts[3];
                    String annoStr = parts[4];
                    Integer anno = (annoStr == null || annoStr.isBlank())
                            ? null : Integer.parseInt(annoStr);
                    out.add(new BookRow(id, titolo, autori, anno));
                }
                line = in.readLine();
            }
        }

        return out;
    }

    // ========= COMANDI SEMPLICI =========

    private String sendSingleCommand(String command) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             Writer w = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            in.readLine(); // benvenuto

            w.write(command + "\n");
            w.flush();

            String line = in.readLine();
            if (line == null) throw new IOException("Nessuna risposta dal server");
            return line.trim();
        }
    }

    // ========= LOGIN / REGISTRAZIONE =========

    public boolean login(String userid, String passwordHash) throws IOException {
        String resp = sendSingleCommand("LOGIN:" + userid + ";" + passwordHash);
        return resp.startsWith("OK LOGIN");
    }

    public boolean registerUser(String userid,
                                String passwordHash,
                                String nome,
                                String cognome,
                                String codiceFiscale,
                                String email) throws IOException {

        String payload = String.join(";",
                userid, passwordHash, nome, cognome, codiceFiscale, email);
        String resp = sendSingleCommand("REGISTER:" + payload);
        return resp.startsWith("OK REGISTER");
    }

    // ========= LIBRERIE =========

    public List<Library> listLibraries(String userid) throws IOException {
        List<Library> result = new ArrayList<>();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             Writer w = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            in.readLine(); // benvenuto
            w.write("LIST_LIBRARIES:" + userid + "\n");
            w.flush();

            String line = in.readLine();
            if (line == null) throw new IOException("Nessuna risposta dal server");
            if (line.startsWith("ERR")) throw new IOException("Errore dal server: " + line);
            if (line.startsWith("OK")) line = in.readLine();

            while (line != null && !"END".equals(line)) {
                if (line.startsWith("LIB;")) {
                    String[] parts = line.split(";", 3);
                    String nome = parts.length > 1 ? parts[1] : "";
                    String idsPart = parts.length > 2 ? parts[2] : "";
                    Set<Integer> ids = new LinkedHashSet<>();
                    if (!idsPart.isEmpty()) {
                        for (String tok : idsPart.split(",")) {
                            tok = tok.trim();
                            if (tok.isEmpty()) continue;
                            try { ids.add(Integer.parseInt(tok)); } catch (NumberFormatException ignore) {}
                        }
                    }
                    result.add(new Library(userid, nome, ids));
                }
                line = in.readLine();
            }
        }

        return result;
    }

    public boolean saveLibrary(Library lib) throws IOException {
        String userid = lib.getUserid();
        String nome   = lib.getNome();
        String idsStr = lib.getBookIds().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        String payload = userid + ";" + nome + ";" + idsStr;
        String resp = sendSingleCommand("SAVE_LIBRARY:" + payload);
        return resp.startsWith("OK SAVE_LIBRARY");
    }

    // ========= VALUTAZIONI =========

    public boolean addReview(Review r) throws IOException {
        String comment = r.getCommento() == null ? "" :
                r.getCommento().replace(";", " ").replace("\n", " ").replace("\r", " ");

        String payload = String.join(";",
                r.getUserid(),
                String.valueOf(r.getBookId()),
                String.valueOf(r.getStile()),
                String.valueOf(r.getContenuto()),
                String.valueOf(r.getGradevolezza()),
                String.valueOf(r.getOriginalita()),
                String.valueOf(r.getEdizione()),
                String.valueOf(r.getVotoFinale()),
                comment
        );

        String resp = sendSingleCommand("ADD_REVIEW:" + payload);
        return resp.startsWith("OK ADD_REVIEW");
    }

    public AggregationService.ReviewStats getReviewStats(int bookId) throws IOException {
        AggregationService.ReviewStats s = new AggregationService.ReviewStats();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             Writer w = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            in.readLine(); // benvenuto
            w.write("GET_REVIEW_STATS:" + bookId + "\n");
            w.flush();

            String line = in.readLine();
            if (line == null) throw new IOException("Nessuna risposta dal server");
            if (line.startsWith("ERR")) throw new IOException("Errore dal server: " + line);

            if (line.startsWith("OK REVIEW_STATS 0")) {
                // nessuna valutazione
                String end = in.readLine(); // END
                return s;
            }

            // OK REVIEW_STATS count;mediaS;mediaC;mediaG;mediaO;mediaE;mediaVF
            String data = line.substring("OK REVIEW_STATS ".length()).trim();
            String[] parts = data.split(";");
            s.count             = Integer.parseInt(parts[0]);
            s.mediaStile        = Double.parseDouble(parts[1]);
            s.mediaContenuto    = Double.parseDouble(parts[2]);
            s.mediaGradevolezza = Double.parseDouble(parts[3]);
            s.mediaOriginalita  = Double.parseDouble(parts[4]);
            s.mediaEdizione     = Double.parseDouble(parts[5]);
            s.mediaVotoFinale   = Double.parseDouble(parts[6]);

            // riga distribuzione
            line = in.readLine();
            if (line != null && line.startsWith("DIST;")) {
                String rest = line.substring("DIST;".length());
                if (!rest.isBlank()) {
                    for (String tok : rest.split(",")) {
                        tok = tok.trim();
                        if (tok.isEmpty()) continue;
                        String[] kv = tok.split(":");
                        int voto = Integer.parseInt(kv[0]);
                        int cnt  = Integer.parseInt(kv[1]);
                        s.distribuzioneVoti.put(voto, cnt);
                    }
                }
            }

            // END
            in.readLine();
        }

        return s;
    }

    // ========= SUGGERIMENTI =========

    public boolean addSuggestion(Suggestion s) throws IOException {
        String idsStr = s.getSuggeriti().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        String payload = s.getUserid() + ";" + s.getBookId() + ";" + idsStr;
        String resp = sendSingleCommand("ADD_SUGGESTION:" + payload);
        return resp.startsWith("OK ADD_SUGGESTION");
    }

    public AggregationService.SuggestionsStats getSuggestionsStats(int bookId) throws IOException {
        AggregationService.SuggestionsStats stats = new AggregationService.SuggestionsStats();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             Writer w = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            in.readLine(); // benvenuto
            w.write("GET_SUGGESTIONS_STATS:" + bookId + "\n");
            w.flush();

            String line = in.readLine();
            if (line == null) throw new IOException("Nessuna risposta dal server");
            if (line.startsWith("ERR")) throw new IOException("Errore dal server: " + line);
            if (line.startsWith("OK")) line = in.readLine();

            while (line != null && !"END".equals(line)) {
                if (line.startsWith("SUG;")) {
                    String[] parts = line.split(";", 3);
                    int idSug = Integer.parseInt(parts[1]);
                    int count = Integer.parseInt(parts[2]);
                    stats.suggeritiCount.put(idSug, count);
                }
                line = in.readLine();
            }
        }

        return stats;
    }
}
