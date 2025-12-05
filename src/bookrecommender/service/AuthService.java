package bookrecommender.service;

import bookrecommender.model.User;
import bookrecommender.repo.UtentiRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

public class AuthService {
    private final UtentiRepository utentiRepo;
    private String currentUserid = null;

    public AuthService(Path utentiFile) {
        this.utentiRepo = new UtentiRepository(utentiFile);
    }

    public boolean registrazione(User u) throws IOException {
        if (u.getUserid() == null || u.getUserid().isBlank()) return false;
        if (utentiRepo.existsUserid(u.getUserid())) return false;
        utentiRepo.append(u);
        return true;
    }

    public boolean login(String userid, String passwordPlain) throws IOException {
        Optional<User> ou = utentiRepo.findByUserid(userid);
        if (ou.isEmpty()) return false;
        String hash = sha256(passwordPlain);
        if (!hash.equals(ou.get().getPasswordHash())) return false;
        currentUserid = userid;
        return true;
    }

    public void logout() { currentUserid = null; }
    public String getCurrentUserid() { return currentUserid; }

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((s==null?"":s).getBytes());
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
