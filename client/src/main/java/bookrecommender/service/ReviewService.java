package bookrecommender.service;

import bookrecommender.client.net.ServerApi;
import bookrecommender.model.Review;

import java.io.IOException;
import java.nio.file.Path;

public class ReviewService {

    private final ServerApi serverApi;

    public ReviewService(Path valutazioniFile, Path librerieFile) {
        // i path non servono più in Lab B, ma manteniamo la firma del costruttore
        this.serverApi = new ServerApi("localhost", 5555);
    }

    public boolean inserisciValutazione(Review r) throws IOException {
        // controlli base lato client
        if (!range15(r.getStile()) ||
            !range15(r.getContenuto()) ||
            !range15(r.getGradevolezza()) ||
            !range15(r.getOriginalita()) ||
            !range15(r.getEdizione())) return false;

        if (r.getCommento() != null && r.getCommento().length() > 256) return false;

        // il resto (libro presente in libreria, duplicati, ecc.) lo controlla il server
        return serverApi.addReview(r);
    }

    private static boolean range15(int x) { return x >= 1 && x <= 5; }
}
