package bookrecommender.repo;

import bookrecommender.model.Library;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class LibrerieRepository {
    private final Path file;

    public LibrerieRepository(Path file) {
        this.file = file;
    }

    // -------- API pubblica --------

    /**
     * Restituisce tutte le librerie salvate (di tutti gli utenti).
     * Viene usato solo internamente; la GUI usa listUserLibraries()
     * attraverso LibraryService che filtra per userid.
     */
    public List<Library> findAll() throws IOException {
        return loadAll();
    }

    /**
     * Restituisce SOLO le librerie dell'utente specificato.
     */
    public List<Library> findByUserid(String userid) throws IOException {
        List<Library> all = loadAll();
        List<Library> res = new ArrayList<>();
        for (Library l : all) {
            if (l.getUserid().equals(userid)) {
                res.add(l);
            }
        }
        return res;
    }

    /**
     * Inserisce o aggiorna una libreria.
     * La libreria è identificata in modo univoco da (userid, nome).
     */
    public void upsert(Library lib) throws IOException {
        List<Library> all = loadAll();
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            Library cur = all.get(i);
            if (cur.getUserid().equals(lib.getUserid())
                    && cur.getNome().equals(lib.getNome())) {
                all.set(i, lib);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(lib);
        }
        saveAll(all);
    }

    // -------- metodi di utilità su file --------

    private List<Library> loadAll() throws IOException {
        List<Library> res = new ArrayList<>();
        if (!Files.exists(file)) {
            return res;
        }

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // formato: userid;nomeLibreria;id1|id2|id3
                String[] parts = line.split(";", 3);
                if (parts.length < 2) continue;

                String userid = parts[0].trim();
                String nome   = parts[1].trim();
                Set<Integer> ids = new LinkedHashSet<>();

                if (parts.length == 3 && !parts[2].trim().isEmpty()) {
                    String[] rawIds = parts[2].split("\\|");
                    for (String s : rawIds) {
                        s = s.trim();
                        if (s.isEmpty()) continue;
                        try {
                            ids.add(Integer.parseInt(s));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                res.add(new Library(userid, nome, ids));
            }
        }

        return res;
    }

    private void saveAll(List<Library> libs) throws IOException {
        if (!Files.exists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            for (Library l : libs) {
                StringBuilder sb = new StringBuilder();
                sb.append(l.getUserid()).append(";");
                sb.append(l.getNome() == null ? "" : l.getNome()).append(";");
                sb.append(joinIds(l.getBookIds()));
                bw.write(sb.toString());
                bw.newLine();
            }
        }
    }

    private static String joinIds(Set<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer id : ids) {
            if (!first) sb.append("|");
            sb.append(id);
            first = false;
        }
        return sb.toString();
    }
}
