package bookrecommender.service;

import bookrecommender.client.net.ServerApi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class AggregationService {

    private final ServerApi serverApi;

    public AggregationService(Path valutazioniFile, Path consigliFile) {
        this.serverApi = new ServerApi("localhost", 5555);
    }

    // === DTO usati dalla GUI (stessa interfaccia di prima) ===

    public static class ReviewStats {
        public int count;
        public Map<Integer,Integer> distribuzioneVoti = new TreeMap<>();
        public double mediaStile, mediaContenuto, mediaGradevolezza,
                mediaOriginalita, mediaEdizione;
        public double mediaVotoFinale;
    }

    public static class SuggestionsStats {
        public Map<Integer,Integer> suggeritiCount = new LinkedHashMap<>();
    }

    public ReviewStats getReviewStats(int bookId) throws IOException {
        return serverApi.getReviewStats(bookId);
    }

    public SuggestionsStats getSuggestionsStats(int bookId) throws IOException {
        return serverApi.getSuggestionsStats(bookId);
    }
}
