package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.model.Suggestion;
import bookrecommender.repo.ConsigliRepository;
import bookrecommender.repo.LibrerieRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class SuggestionService {
    private final ConsigliRepository consRepo;
    private final LibrerieRepository librRepo;

    public SuggestionService(Path consigliFile, Path librerieFile) {
        this.consRepo = new ConsigliRepository(consigliFile);
        this.librRepo = new LibrerieRepository(librerieFile);
    }

    public boolean inserisciSuggerimento(Suggestion s) throws IOException {
        // Max 3, no duplicati, niente self-suggestion, e tutti nella libreria dell’utente
        List<Integer> list = new ArrayList<>(s.getSuggeriti());
        // rimuovo duplicati preservando ordine
        LinkedHashSet<Integer> set = new LinkedHashSet<>(list);
        list = new ArrayList<>(set);
        if (list.size() > 3) list = list.subList(0,3);
        list.removeIf(id -> id == s.getBookId());
        if (list.isEmpty()) return false;

        // controlla appartenenza alla libreria dell’utente
        Set<Integer> owned = new HashSet<>();
        for (Library L : librRepo.findByUserid(s.getUserid())) owned.addAll(L.getBookIds());
        for (Integer id : list) if (!owned.contains(id)) return false;

        consRepo.append(new Suggestion(s.getUserid(), s.getBookId(), list));
        return true;
    }
}
