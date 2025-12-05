package bookrecommender.model;

import java.util.ArrayList;
import java.util.List;

public class Suggestion {
    private final String userid;
    private final int bookId;          // libro di riferimento
    private final List<Integer> suggeriti; // fino a 3 idLibro suggeriti

    public Suggestion(String userid, int bookId, List<Integer> suggeriti) {
        this.userid = userid;
        this.bookId = bookId;
        this.suggeriti = suggeriti == null ? new ArrayList<>() : new ArrayList<>(suggeriti);
    }

    public String getUserid() { return userid; }
    public int getBookId() { return bookId; }
    public List<Integer> getSuggeriti() { return new ArrayList<>(suggeriti); }
}
