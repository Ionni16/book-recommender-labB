package bookrecommender.model;

public class Review {
    private final String userid;
    private final int bookId;
    private final int stile, contenuto, gradevolezza, originalita, edizione; // 1..5
    private final int votoFinale; // round(media 5 criteri)
    private final String commento; // ≤256

    public Review(String userid, int bookId, int stile, int contenuto, int gradevolezza, int originalita, int edizione, int votoFinale, String commento) {
        this.userid = userid;
        this.bookId = bookId;
        this.stile = stile;
        this.contenuto = contenuto;
        this.gradevolezza = gradevolezza;
        this.originalita = originalita;
        this.edizione = edizione;
        this.votoFinale = votoFinale;
        this.commento = commento;
    }

    public String getUserid() { return userid; }
    public int getBookId() { return bookId; }
    public int getStile() { return stile; }
    public int getContenuto() { return contenuto; }
    public int getGradevolezza() { return gradevolezza; }
    public int getOriginalita() { return originalita; }
    public int getEdizione() { return edizione; }
    public int getVotoFinale() { return votoFinale; }
    public String getCommento() { return commento; }

    public static int calcolaVotoFinale(int stile, int contenuto, int gradevolezza, int originalita, int edizione) {
        double media = (stile + contenuto + gradevolezza + originalita + edizione) / 5.0;
        return (int)Math.round(media);
    }
}
