package bookrecommender.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Book {
    private final int id;
    private final String titolo;
    private final List<String> autori;
    private final Integer anno;
    private final String editore;
    private final String categoria;

    public Book(int id, String titolo, List<String> autori, Integer anno, String editore, String categoria) {
        this.id = id;
        this.titolo = titolo == null ? "" : titolo;
        this.autori = autori == null ? new ArrayList<>() : new ArrayList<>(autori);
        this.anno = anno;
        this.editore = editore;
        this.categoria = categoria;
    }

    public int getId() { return id; }
    public String getTitolo() { return titolo; }
    public List<String> getAutori() { return Collections.unmodifiableList(autori); }
    public Integer getAnno() { return anno; }
    public String getEditore() { return editore; }
    public String getCategoria() { return categoria; }

    @Override public boolean equals(Object o) { return (o instanceof Book) && ((Book)o).id == id; }
    @Override public int hashCode() { return Objects.hash(id); }
}
