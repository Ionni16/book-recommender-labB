package bookrecommender.repo;

import bookrecommender.model.Book;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class LibriRepository {

    private final Path file;      // Libri.dati
    private final Path csvFile;   // BooksDatasetClean.csv (dataset originale)
    private final List<Book> books = new ArrayList<>();

    private int nextId = 1;

    public LibriRepository(Path file) {
        this.file = file;
        Path dir = file.getParent();
        if (dir == null) dir = Paths.get(".");
        // il CSV sta nella stessa cartella di Libri.dati
        this.csvFile = dir.resolve("BooksDatasetClean.csv");
    }

    /** Restituisce tutti i libri (lista immodificabile). */
    public List<Book> all() {
        return Collections.unmodifiableList(books);
    }

    /** Ritorna un libro per id o null se non trovato. */
    public Book findById(int id) {
        return books.stream().filter(b -> b.getId() == id).findFirst().orElse(null);
    }

    /** Numero di libri caricati. */
    public int size() {
        return books.size();
    }

    /** Salva l'elenco corrente di libri in Libri.dati. */
    public void save() throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("idLibro;Titolo;Autori;Anno;Editore;Categoria");
            w.newLine();
            for (Book b : books) {
                String autori = String.join("|", b.getAutori());
                String annoStr = (b.getAnno() == null) ? "" : b.getAnno().toString();
                String editore = b.getEditore() == null ? "" : b.getEditore();
                String categoria = b.getCategoria() == null ? "" : b.getCategoria();
                w.write(b.getId() + ";" + b.getTitolo() + ";" + autori + ";" +
                        annoStr + ";" + editore + ";" + categoria);
                w.newLine();
            }
        }
    }

    /**
     * Carica i libri:
     * - se Libri.dati esiste, lo legge
     * - altrimenti, se esiste BooksDatasetClean.csv, lo usa per generare Libri.dati e popolare i libri
     */
    public void load() throws IOException {
        books.clear();
        nextId = 1;

        if (Files.exists(file)) {
            loadFromLibri();
        } else if (Files.exists(csvFile)) {
            System.out.println("Libri.dati non trovato, genero da BooksDatasetClean.csv...");
            buildFromCsv();
            save();           // scrive Libri.dati per le prossime esecuzioni
        } else {
            throw new FileNotFoundException(
                    "Non trovo né " + file.toAbsolutePath() +
                    " né " + csvFile.toAbsolutePath());
        }
    }

    //  METODI PRIVATI 

    /** Legge Libri.dati nel formato: idLibro;Titolo;Autori;Anno;Editore;Categoria */
    private void loadFromLibri() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = r.readLine(); // header
            if (line == null) return;

            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 3) continue;

                int id;
                try {
                    id = Integer.parseInt(c[0].trim());
                } catch (NumberFormatException ex) {
                    id = nextId;
                }

                String titolo = c[1].trim();
                String autoriRaw = c[2].trim();
                Integer anno = null;
                if (c.length > 3 && !c[3].trim().isEmpty()) {
                    try {
                        anno = Integer.parseInt(c[3].trim());
                    } catch (NumberFormatException ignored) {}
                }
                String editore = c.length > 4 ? c[4].trim() : null;
                String categoria = c.length > 5 ? c[5].trim() : null;

                List<String> autori = normalizeAutori(autoriRaw);

                books.add(new Book(id, titolo, autori, anno, editore, categoria));
                nextId = Math.max(nextId, id + 1);
            }
        }
    }

    /**
     * Costruisce la lista di libri a partire dal CSV originale BooksDatasetClean.csv.
     * CSV atteso con colonne:
     * 0: Title
     * 1: Authors
     * 2: Description
     * 3: Category
     * 4: Publisher
     * 5: Price Starting With ($)
     * 6: Publish Date (Month)
     * 7: Publish Date (Year)
     */
    private void buildFromCsv() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line = br.readLine(); // header
            if (line == null) return;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                List<String> cols = parseCSV(line);
                if (cols.size() < 8) continue;

                String title     = cols.get(0).trim();
                String authors   = cols.get(1).trim();
                String category  = cols.get(3).trim();
                String publisher = cols.get(4).trim();
                String yearStr   = cols.get(7).trim();

                Integer year = null;
                try {
                    if (!yearStr.isEmpty()) {
                        year = Integer.parseInt(yearStr);
                    }
                } catch (NumberFormatException ignored) {}

                List<String> autori = normalizeAutori(authors);

                Book b = new Book(nextId++, title, autori, year,
                        publisher.isEmpty() ? null : publisher,
                        category.isEmpty() ? null : category);

                books.add(b);
            }
        }
    }

    /** parsing CSV grezzo che gestisce virgolette e virgole nei campi */
    private static List<String> parseCSV(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }

    /** Normalizza la stringa autori in una lista (split su virgole, punto e virgola e '|'). */
    private List<String> normalizeAutori(String raw) {
        if (raw == null) return Collections.emptyList();
        String s = raw.trim();
        // rimuovo eventuale prefisso "By "
        if (s.toLowerCase().startsWith("by ")) s = s.substring(3);
        // split su virgola, ';' o '|'
        String[] parts = s.split("\\s*[;,|]\\s*");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }
}
