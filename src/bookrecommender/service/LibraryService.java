package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.repo.LibrerieRepository;
import bookrecommender.repo.LibriRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class LibraryService {
    private final LibrerieRepository librerieRepo;
    private final LibriRepository libriRepo; // per validare idLibro esistenti

    public LibraryService(Path librerieFile, LibriRepository libriRepo) {
        this.librerieRepo = new LibrerieRepository(librerieFile);
        this.libriRepo = libriRepo;
    }

    /**
     * Restituisce SOLO le librerie dell'utente indicato.
     */
    public List<Library> listUserLibraries(String userid) throws IOException {
        return librerieRepo.findByUserid(userid);
    }

    /**
     * Crea o aggiorna una libreria dell'utente.
     * - Verifica che gli ID libro esistano nel dataset
     * - Nessun ID duplicato
     * - Libreria identificata da (userid, nome)
     */
    public boolean saveLibrary(Library lib) throws IOException {
        // Valida che i bookIds esistano nel dataset libri
        Set<Integer> valid = new LinkedHashSet<>();
        Set<Integer> allIds = new HashSet<>();
        libriRepo.all().forEach(b -> allIds.add(b.getId()));

        for (Integer id : lib.getBookIds()) {
            if (allIds.contains(id)) {
                valid.add(id);
            }
        }

        Library clean = new Library(lib.getUserid(), lib.getNome(), valid);
        librerieRepo.upsert(clean);
        return true;
    }
}
