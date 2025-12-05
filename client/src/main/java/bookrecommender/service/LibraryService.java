package bookrecommender.service;

import bookrecommender.client.net.ServerApi;
import bookrecommender.model.Library;
import bookrecommender.repo.LibriRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Versione Lab B di LibraryService.
 * Le librerie non sono più salvate su file, ma nel database PostgreSQL
 * tramite il server BookRecommender.
 */
public class LibraryService {
    private final LibriRepository libriRepo; // ancora usato solo per validare gli id
    private final ServerApi serverApi;

    public LibraryService(Path librerieFile, LibriRepository libriRepo) {
        this.libriRepo = libriRepo;
        this.serverApi = new ServerApi("localhost", 5555);
    }

    /**
     * Restituisce tutte le librerie personali dell'utente.
     */
    public List<Library> listUserLibraries(String userid) throws IOException {
        try {
            return serverApi.listLibraries(userid);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Errore nel caricamento delle librerie da server", e);
        }
    }

    /**
     * Salva (crea o aggiorna) una libreria dell'utente.
     * Vengono considerati solo gli idLibro presenti nel dataset libri.
     */
    public boolean saveLibrary(Library lib) throws IOException {
        // Valida che i bookIds esistano nel dataset libri (come nel Lab A)
        Set<Integer> valid = new LinkedHashSet<>();
        if (libriRepo != null) {
            Set<Integer> allIds = new HashSet<>();
            libriRepo.all().forEach(b -> allIds.add(b.getId()));
            for (Integer id : lib.getBookIds()) {
                if (allIds.contains(id)) {
                    valid.add(id);
                }
            }
        } else {
            valid.addAll(lib.getBookIds());
        }

        Library clean = new Library(lib.getUserid(), lib.getNome(), valid);

        try {
            return serverApi.saveLibrary(clean);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Errore nel salvataggio della libreria sul server", e);
        }
    }
}
