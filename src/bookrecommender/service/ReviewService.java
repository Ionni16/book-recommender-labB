package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.model.Review;
import bookrecommender.repo.LibrerieRepository;
import bookrecommender.repo.ValutazioniRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ReviewService {
    private final ValutazioniRepository valRepo;
    private final LibrerieRepository librRepo;

    public ReviewService(Path valutazioniFile, Path librerieFile) {
        this.valRepo = new ValutazioniRepository(valutazioniFile);
        this.librRepo = new LibrerieRepository(librerieFile);
    }

    public boolean inserisciValutazione(Review r) throws IOException {
        // Validazioni: range 1..5, commento ≤256, libro presente in una libreria dell’utente
        if (!range15(r.getStile()) || !range15(r.getContenuto()) || !range15(r.getGradevolezza()) ||
            !range15(r.getOriginalita()) || !range15(r.getEdizione())) return false;
        if (r.getCommento() != null && r.getCommento().length() > 256) return false;

        // non permettere più valutazioni dello stesso libro dallo stesso utente
        List<Review> esistenti = valRepo.findByBookId(r.getBookId());
        for (Review ex : esistenti) {
            if (ex.getUserid().equals(r.getUserid())) {
                return false;
            }
        }

        boolean ok = false;
        List<Library> libs = librRepo.findByUserid(r.getUserid());
        for (Library L : libs) {
            if (L.getBookIds().contains(r.getBookId())) {
                ok = true;
                break;
            }
        }
        if (!ok) return false;

        valRepo.append(r);
        return true;
    }

    private static boolean range15(int x) { return x >= 1 && x <= 5; }
}
