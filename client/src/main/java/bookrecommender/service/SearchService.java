package bookrecommender.service;

import bookrecommender.client.net.ServerApi;
import bookrecommender.model.Book;
import bookrecommender.repo.LibriRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Servizi di ricerca.
 * In Lab B la ricerca per titolo usa il server/DB,
 * le ricerche per autore restano locali (file) per semplicità.
 */
public class SearchService {
    private final LibriRepository libriRepo;
    private final ServerApi serverApi = new ServerApi("localhost", 5555);

    public SearchService(LibriRepository libriRepo) {
        this.libriRepo = libriRepo;
    }

    /**
     * Ricerca per titolo.
     * Prova prima via server+DB; se fallisce, usa la vecchia ricerca locale.
     */
    public List<Book> cercaLibroPerTitolo(String q) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty()) {
            // per query vuota: come prima, tutti i libri locali
            return libriRepo.all();
        }

        try {
            // tentativo via server
            List<ServerApi.BookRow> rows = serverApi.searchByTitle(query);
            if (!rows.isEmpty()) {
                List<Book> books = new ArrayList<>();
                for (ServerApi.BookRow r : rows) {
                    List<String> autori = (r.autori == null || r.autori.isBlank())
                            ? List.of()
                            : Arrays.asList(r.autori.split("\\s*,\\s*"));
                    books.add(new Book(
                            r.id,
                            r.titolo,
                            autori,
                            r.anno,
                            null,
                            null
                    ));
                }
                return books;
            }
        } catch (Exception e) {
            // se il server è giù o dà errore, logghiamo e facciamo fallback locale
            e.printStackTrace();
        }

        // fallback: vecchia ricerca locale del Lab A
        String needle = norm(query);
        return libriRepo.all().stream()
                .filter(b -> norm(b.getTitolo()).contains(needle))
                .collect(Collectors.toList());
    }

        public List<Book> cercaLibroPerAutore(String a) {
        String query = a == null ? "" : a.trim();
        if (query.isEmpty()) return libriRepo.all(); // o lista vuota, come preferisci

        try {
            List<ServerApi.BookRow> rows = serverApi.searchByAuthor(query);
            if (!rows.isEmpty()) {
                List<Book> books = new ArrayList<>();
                for (ServerApi.BookRow r : rows) {
                    List<String> autori = (r.autori == null || r.autori.isBlank())
                            ? List.of()
                            : Arrays.asList(r.autori.split("\\s*,\\s*"));
                    books.add(new Book(
                            r.id, r.titolo, autori, r.anno, null, null
                    ));
                }
                return books;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback locale
        String needle = norm(a);
        return libriRepo.all().stream()
                .filter(b -> b.getAutori().stream().anyMatch(x -> norm(x).contains(needle)))
                .collect(Collectors.toList());
    }

    public List<Book> cercaLibroPerAutoreEAnno(String a, int anno) {
        String query = a == null ? "" : a.trim();
        if (query.isEmpty()) return List.of();

        try {
            List<ServerApi.BookRow> rows = serverApi.searchByAuthorAndYear(query, anno);
            if (!rows.isEmpty()) {
                List<Book> books = new ArrayList<>();
                for (ServerApi.BookRow r : rows) {
                    List<String> autori = (r.autori == null || r.autori.isBlank())
                            ? List.of()
                            : Arrays.asList(r.autori.split("\\s*,\\s*"));
                    books.add(new Book(
                            r.id, r.titolo, autori, r.anno, null, null
                    ));
                }
                return books;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback locale
        String needle = norm(a);
        return libriRepo.all().stream()
                .filter(b -> (b.getAnno() != null && b.getAnno() == anno))
                .filter(b -> b.getAutori().stream().anyMatch(x -> norm(x).contains(needle)))
                .collect(Collectors.toList());
    }


    private static String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ITALIAN);
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // toglie accenti
        return t;
    }
}
