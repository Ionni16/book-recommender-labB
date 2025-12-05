package bookrecommender;

import bookrecommender.model.*;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class BookRecommender {
    /**
     * Autori: Ionut Puiu -753296- Sede: VA
     *         
     */
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Path dataDir = Path.of("data");

        // Repo libri (read-only)
        LibriRepository libriRepo = new LibriRepository(dataDir.resolve("Libri.dati"));
        try {
            libriRepo.load();
        } catch (Exception e) {
            System.err.println("Errore nel caricamento di Libri.dati: " + e.getMessage());
            return;
        }
        System.out.println("=== Book Recommender (Lab A) ===");
        System.out.println("Libri caricati: " + libriRepo.size());

        // Servizi
        SearchService search = new SearchService(libriRepo);
        AuthService auth = new AuthService(dataDir.resolve("UtentiRegistrati.dati"));
        LibraryService libraryService = new LibraryService(dataDir.resolve("Librerie.dati"), libriRepo);
        ReviewService reviewService = new ReviewService(dataDir.resolve("ValutazioniLibri.dati"), dataDir.resolve("Librerie.dati"));
        SuggestionService suggestionService = new SuggestionService(dataDir.resolve("ConsigliLibri.dati"), dataDir.resolve("Librerie.dati"));
        AggregationService agg = new AggregationService(dataDir.resolve("ValutazioniLibri.dati"), dataDir.resolve("ConsigliLibri.dati"));

        while (true) {
            System.out.println();
            System.out.println("Menu:");
            System.out.println("1) Cerca per titolo");
            System.out.println("2) Cerca per autore");
            System.out.println("3) Cerca per autore e anno");
            System.out.println("4) Registrazione");
            System.out.println("5) Login");
            System.out.println("6) Crea/Aggiorna Libreria (richiede login)");
            System.out.println("7) Inserisci Valutazione (richiede login)");
            System.out.println("8) Inserisci Suggerimenti (richiede login)");
            System.out.println("9) Visualizza Dettagli Libro (aggregati)");
            System.out.println("L) Logout");
            System.out.println("0) Esci");
            System.out.print("Scelta: ");
            String choice = in.nextLine().trim();

            switch (choice) {
                case "1": doSearchByTitle(in, search); break;
                case "2": doSearchByAuthor(in, search); break;
                case "3": doSearchByAuthorYear(in, search); break;
                case "4": doRegister(in, auth); break;
                case "5": doLogin(in, auth); break;
                case "6": doLibrary(in, auth, libraryService, libriRepo); break;
                case "7": doReview(in, auth, reviewService, libriRepo); break;
                case "8": doSuggest(in, auth, suggestionService, libriRepo); break;
                case "9": doVisualizza(in, search, agg, libriRepo); break;
                case "L": case "l": auth.logout(); System.out.println("Logout eseguito."); break;
                case "0": return;
                default: System.out.println("Scelta non valida.");
            }
        }
    }

    // === Cerca ===
    private static void doSearchByTitle(Scanner in, SearchService search) {
        System.out.print("Titolo (sottostringa): ");
        printBooks(search.cercaLibroPerTitolo(in.nextLine()));
    }
    private static void doSearchByAuthor(Scanner in, SearchService search) {
        System.out.print("Autore (sottostringa): ");
        printBooks(search.cercaLibroPerAutore(in.nextLine()));
    }
    private static void doSearchByAuthorYear(Scanner in, SearchService search) {
        System.out.print("Autore: ");
        String a = in.nextLine();
        System.out.print("Anno (es. 1999): ");
        try {
            int anno = Integer.parseInt(in.nextLine().trim());
            printBooks(search.cercaLibroPerAutoreEAnno(a, anno));
        } catch (NumberFormatException e) {
            System.out.println("Anno non valido.");
        }
    }

    // === Registrazione/Login ===
    private static void doRegister(Scanner in, AuthService auth) {
        System.out.println("=== Registrazione ===");
        System.out.print("Userid: "); String userid = in.nextLine().trim();
        System.out.print("Password: "); String pass = in.nextLine();
        System.out.print("Nome: "); String nome = in.nextLine().trim();
        System.out.print("Cognome: "); String cognome = in.nextLine().trim();
        System.out.print("Codice Fiscale: "); String cf = in.nextLine().trim();
        System.out.print("Email: "); String email = in.nextLine().trim();

        String hash = AuthService.sha256(pass);
        User u = new User(userid, hash, nome, cognome, cf, email);
        try {
            boolean ok = auth.registrazione(u);
            System.out.println(ok ? "Registrazione completata." : "Registrazione fallita (userid già esistente?).");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }
    private static void doLogin(Scanner in, AuthService auth) {
        System.out.println("=== Login ===");
        System.out.print("Userid: "); String userid = in.nextLine().trim();
        System.out.print("Password: "); String pass = in.nextLine();
        try {
            boolean ok = auth.login(userid, pass);
            System.out.println(ok ? "Login OK." : "Credenziali errate.");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Librerie ===
    private static void doLibrary(Scanner in, AuthService auth, LibraryService libraryService, LibriRepository libriRepo) {
        String me = auth.getCurrentUserid();
        if (me == null) { System.out.println("Devi fare login."); return; }

        try {
            System.out.println("=== Librerie di " + me + " ===");
            var libs = libraryService.listUserLibraries(me);
            if (libs.isEmpty()) System.out.println("(nessuna libreria)");
            else libs.forEach(L -> System.out.println("- " + L.getNome() + " -> " + L.getBookIds().size() + " libri"));

            System.out.print("Nome libreria da creare/aggiornare: ");
            String nome = in.nextLine().trim();

            // Inserisci lista di idLibro separati da virgola
            System.out.print("Inserisci idLibro separati da virgola (es: 10,25,31): ");
            String line = in.nextLine().trim();
            Set<Integer> ids = parseIdSet(line);

            Library lib = new Library(me, nome, ids);
            boolean ok = libraryService.saveLibrary(lib);
            System.out.println(ok ? "Libreria salvata." : "Errore nel salvataggio.");

        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Valutazioni ===
    private static void doReview(Scanner in, AuthService auth, ReviewService reviewService, LibriRepository libriRepo) {
        String me = auth.getCurrentUserid();
        if (me == null) { System.out.println("Devi fare login."); return; }

        try {
            System.out.print("idLibro da valutare: ");
            int id = Integer.parseInt(in.nextLine().trim());
            int stile = askInt(in, "Stile (1..5): ", 1, 5);
            int contenuto = askInt(in, "Contenuto (1..5): ", 1, 5);
            int gradev = askInt(in, "Gradevolezza (1..5): ", 1, 5);
            int orig = askInt(in, "Originalità (1..5): ", 1, 5);
            int ed = askInt(in, "Edizione (1..5): ", 1, 5);
            System.out.print("Commento (max 256, opzionale): ");
            String comm = in.nextLine();
            if (comm != null && comm.length() > 256) { System.out.println("Commento troppo lungo."); return; }

            int votoFinale = Review.calcolaVotoFinale(stile, contenuto, gradev, orig, ed);
            Review r = new Review(me, id, stile, contenuto, gradev, orig, ed, votoFinale, comm);
            boolean ok = reviewService.inserisciValutazione(r);
            System.out.println(ok ? "Valutazione registrata." : "Impossibile registrare (controlla che il libro sia nella tua libreria).");

        } catch (NumberFormatException nfe) {
            System.out.println("idLibro non valido.");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Suggerimenti ===
    private static void doSuggest(Scanner in, AuthService auth, SuggestionService suggestionService, LibriRepository libriRepo) {
        String me = auth.getCurrentUserid();
        if (me == null) { System.out.println("Devi fare login."); return; }

        try {
            System.out.print("idLibro di riferimento: ");
            int id = Integer.parseInt(in.nextLine().trim());
            System.out.print("Suggerisci fino a 3 idLibro (es: 101,202,303): ");
            List<Integer> list = parseIdList(in.nextLine().trim());
            Suggestion s = new Suggestion(me, id, list);
            boolean ok = suggestionService.inserisciSuggerimento(s);
            System.out.println(ok ? "Suggerimento registrato." : "Impossibile registrare (max 3, no duplicati/self, e libri nella tua libreria).");

        } catch (NumberFormatException nfe) {
            System.out.println("id non valido.");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Visualizza Dettagli ===
    private static void doVisualizza(Scanner in, SearchService search, AggregationService agg, LibriRepository libriRepo) {
        System.out.print("Cerca titolo per scegliere il libro: ");
        var results = search.cercaLibroPerTitolo(in.nextLine());
        if (results.isEmpty()) { System.out.println("Nessun risultato."); return; }
        printBooks(results);
        System.out.print("Inserisci idLibro da visualizzare: ");
        try {
            int id = Integer.parseInt(in.nextLine().trim());
            var opt = libriRepo.all().stream().filter(b -> b.getId()==id).findFirst();
            if (opt.isEmpty()) { System.out.println("idLibro non trovato."); return; }
            Book b = opt.get();
            System.out.println("\n=== Dettagli Libro ===");
            System.out.println("[" + b.getId() + "] " + b.getTitolo());
            System.out.println("Autori: " + String.join(", ", b.getAutori()));
            System.out.println("Anno: " + (b.getAnno()==null?"":b.getAnno()));
            System.out.println("Editore: " + (b.getEditore()==null?"":b.getEditore()));
            System.out.println("Categoria: " + (b.getCategoria()==null?"":b.getCategoria()));

            var rs = agg.getReviewStats(b.getId());
            System.out.println("\n-- Valutazioni --");
            System.out.println("Numero valutazioni: " + rs.count);
            if (rs.count > 0) {
                System.out.printf(Locale.ROOT, "Medie -> Stile: %.2f, Contenuto: %.2f, Gradevolezza: %.2f, Originalità: %.2f, Edizione: %.2f%n",
                        rs.mediaStile, rs.mediaContenuto, rs.mediaGradevolezza, rs.mediaOriginalita, rs.mediaEdizione);
                System.out.println("Distribuzione voto finale: " + rs.distribuzioneVoti);
            }

            var ss = agg.getSuggestionsStats(b.getId());
            System.out.println("\n-- Suggerimenti (idLibro -> conteggio utenti) --");
            if (ss.suggeritiCount.isEmpty()) System.out.println("(nessuno)");
            else {
                String line = ss.suggeritiCount.entrySet().stream()
                        .map(e -> e.getKey() + "->" + e.getValue())
                        .collect(Collectors.joining(", "));
                System.out.println(line);
            }

        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Utils stampa e parsing ===
    private static void printBooks(List<Book> results) {
        if (results.isEmpty()) { System.out.println("Nessun risultato."); return; }
        System.out.println("Trovati " + results.size() + " risultati:");
        for (int i = 0; i < Math.min(results.size(), 20); i++) {
            Book b = results.get(i);
            System.out.printf("- [%d] %s (%s) | Autori: %s%n",
                    b.getId(),
                    b.getTitolo(),
                    b.getAnno() == null ? "" : b.getAnno().toString(),
                    String.join(", ", b.getAutori()));
        }
        if (results.size() > 20) System.out.println("... (mostrati i primi 20)");
    }
    private static Set<Integer> parseIdSet(String csv) {
        Set<Integer> out = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) return out;
        for (String p : csv.split(",")) {
            try { out.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
        }
        return out;
    }
    private static List<Integer> parseIdList(String csv) {
        List<Integer> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String p : csv.split(",")) {
            try { out.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
        }
        return out;
    }
    private static int askInt(Scanner in, String prompt, int min, int max) {
        System.out.print(prompt);
        try {
            int x = Integer.parseInt(in.nextLine().trim());
            if (x < min || x > max) throw new NumberFormatException();
            return x;
        } catch (NumberFormatException e) {
            System.out.println("Valore non valido. Annullato.");
            throw e;
        }
    }
}
