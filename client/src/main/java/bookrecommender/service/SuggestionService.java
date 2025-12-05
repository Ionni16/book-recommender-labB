package bookrecommender.service;

import bookrecommender.client.net.ServerApi;
import bookrecommender.model.Suggestion;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class SuggestionService {

    private final ServerApi serverApi;

    public SuggestionService(Path consigliFile, Path librerieFile) {
        this.serverApi = new ServerApi("localhost", 5555);
    }

    public boolean inserisciSuggerimento(Suggestion s) throws IOException {
        // stessa logica del Lab A per pulire la lista

        LinkedHashSet<Integer> set = new LinkedHashSet<>(s.getSuggeriti());
        set.removeIf(id -> id == null || id <= 0 || id == s.getBookId());

        List<Integer> list = new ArrayList<>(set);
        if (list.size() > 3) list = list.subList(0, 3);
        if (list.isEmpty()) return false;

        Suggestion cleaned = new Suggestion(s.getUserid(), s.getBookId(), list);

        // controllo su appartenenza alle librerie e su id validi lo fa il server
        return serverApi.addSuggestion(cleaned);
    }
}
