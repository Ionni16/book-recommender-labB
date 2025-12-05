package bookrecommender.service;

import bookrecommender.client.net.ServerApi;
import bookrecommender.model.User;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Versione Lab B di AuthService.
 * Non usa più UtentiRepository e file locali,
 * ma parla con il server tramite ServerApi.
 */
public class AuthService {

    private final ServerApi serverApi;
    private String currentUserid = null;

    /**
     * Manteniamo la stessa firma del costruttore del Lab A
     * per non dover cambiare BookRecommenderFX, ma il Path
     * viene ignorato: tutto passa da PostgreSQL via server.
     */
    public AuthService(Path utentiFile) {
        this.serverApi = new ServerApi("localhost", 5555);
    }

    /**
     * Registrazione di un nuovo utente.
     * Il campo passwordHash deve essere già valorizzato nel User.
     */
    public boolean registrazione(User u) throws Exception {
        return serverApi.registerUser(
                u.getUserid(),
                u.getPasswordHash(),
                u.getNome(),
                u.getCognome(),
                u.getCodiceFiscale(),
                u.getEmail()
        );
    }

    /**
     * Login: prende la password in chiaro, calcola sha256
     * e chiede al server di verificare.
     */
    public boolean login(String userid, String plainPassword) throws Exception {
        String hash = sha256(plainPassword);
        boolean ok = serverApi.login(userid, hash);
        if (ok) {
            currentUserid = userid;
        } else {
            currentUserid = null;
        }
        return ok;
    }

    public void logout() {
        currentUserid = null;
    }

    public String getCurrentUserid() {
        return currentUserid;
    }

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((s == null ? "" : s).getBytes());
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
