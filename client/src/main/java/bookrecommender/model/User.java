package bookrecommender.model;

import java.util.Objects;

public class User {
    private final String userid;
    private final String passwordHash; // SHA-256 
    private final String nome;
    private final String cognome;
    private final String codiceFiscale;
    private final String email;

    public User(String userid, String passwordHash, String nome, String cognome, String codiceFiscale, String email) {
        this.userid = userid;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.cognome = cognome;
        this.codiceFiscale = codiceFiscale;
        this.email = email;
    }

    public String getUserid() { return userid; }
    public String getPasswordHash() { return passwordHash; }
    public String getNome() { return nome; }
    public String getCognome() { return cognome; }
    public String getCodiceFiscale() { return codiceFiscale; }
    public String getEmail() { return email; }

    @Override public boolean equals(Object o) { return (o instanceof User) && Objects.equals(userid, ((User)o).userid); }
    @Override public int hashCode() { return Objects.hash(userid); }
}
