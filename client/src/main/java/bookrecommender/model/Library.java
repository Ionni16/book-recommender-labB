package bookrecommender.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Library {
    private final String userid;
    private final String nome;      // nome libreria scelto dall'utente
    private final Set<Integer> bookIds; // idLibro presenti nella libreria

    public Library(String userid, String nome, Set<Integer> bookIds) {
        this.userid = userid;
        this.nome = nome;
        this.bookIds = bookIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(bookIds);
    }

    public String getUserid() { return userid; }
    public String getNome() { return nome; }
    public Set<Integer> getBookIds() { return new LinkedHashSet<>(bookIds); }

    // evita esplicitamente i duplicati
    public boolean addBook(int id) {
        if (bookIds.contains(id)) return false;
        return bookIds.add(id);
    }

    public boolean removeBook(int id) { return bookIds.remove(id); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Library)) return false;
        Library other = (Library)o;
        return Objects.equals(userid, other.userid) &&
               Objects.equals(nome, other.nome);
    }

    @Override
    public int hashCode() { return Objects.hash(userid, nome); }
}
