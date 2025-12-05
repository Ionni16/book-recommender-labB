package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.repo.ConsigliRepository;
import bookrecommender.repo.ValutazioniRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class AggregationService {
    private final ValutazioniRepository valRepo;
    private final ConsigliRepository consRepo;

    public AggregationService(Path valutazioniFile, Path consigliFile) {
        this.valRepo = new ValutazioniRepository(valutazioniFile);
        this.consRepo = new ConsigliRepository(consigliFile);
    }

    public static class ReviewStats {
        public int count;
        public Map<Integer,Integer> distribuzioneVoti = new TreeMap<>(); // votoFinale  count
        public double mediaStile, mediaContenuto, mediaGradevolezza, mediaOriginalita, mediaEdizione;
        public double mediaVotoFinale; //  NUOVO: media totale (voto finale)
    }

    public static class SuggestionsStats {
        // idLibro suggerito - numero di utenti che l'hanno suggerito
        public Map<Integer,Integer> suggeritiCount = new LinkedHashMap<>();
    }

    public ReviewStats getReviewStats(int bookId) throws IOException {
        List<Review> reviews = valRepo.findByBookId(bookId);
        ReviewStats s = new ReviewStats();
        s.count = reviews.size();
        if (s.count == 0) return s;

        int sumS=0, sumC=0, sumG=0, sumO=0, sumE=0, sumVF=0;
        for (Review r : reviews) {
            s.distribuzioneVoti.merge(r.getVotoFinale(), 1, Integer::sum);
            sumS += r.getStile();
            sumC += r.getContenuto();
            sumG += r.getGradevolezza();
            sumO += r.getOriginalita();
            sumE += r.getEdizione();
            sumVF += r.getVotoFinale();     //  sommo il voto finale
        }
        s.mediaStile        = sumS / (double)s.count;
        s.mediaContenuto    = sumC / (double)s.count;
        s.mediaGradevolezza = sumG / (double)s.count;
        s.mediaOriginalita  = sumO / (double)s.count;
        s.mediaEdizione     = sumE / (double)s.count;
        s.mediaVotoFinale   = sumVF / (double)s.count; //  media totale
        return s;
    }

    public SuggestionsStats getSuggestionsStats(int bookId) throws IOException {
        List<Suggestion> sugs = consRepo.findByBookId(bookId);
        SuggestionsStats s = new SuggestionsStats();
        for (Suggestion sg : sugs) {
            for (Integer sug : sg.getSuggeriti()) {
                s.suggeritiCount.merge(sug, 1, Integer::sum);
            }
        }
        // Ordina per conteggio desc
        s.suggeritiCount = s.suggeritiCount.entrySet().stream()
            .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(LinkedHashMap::new,
                     (m,e) -> m.put(e.getKey(), e.getValue()),
                     LinkedHashMap::putAll);
        return s;
    }
}
