package bookrecommender.repo;

import bookrecommender.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class UtentiRepository {
    private final Path file;

    public UtentiRepository(Path file) {
        this.file = file;
    }

    public Optional<User> findByUserid(String userid) throws IOException {
        // Se il file non esiste ancora, non ci sono utenti
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                String[] c = line.split(";", -1);
                if (c.length < 6) continue;
                if (userid.equals(c[0])) {
                    return Optional.of(new User(c[0], c[1], c[2], c[3], c[4], c[5]));
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsUserid(String userid) throws IOException {
        return findByUserid(userid).isPresent();
    }

    public void append(User u) throws IOException {
        boolean exists = Files.exists(file);
        try (BufferedWriter bw = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            if (!exists) {
                bw.write("userid;passwordHash;nome;cognome;codiceFiscale;email\n");
            }
            bw.write(String.join(";", Arrays.asList(
                    u.getUserid(),
                    u.getPasswordHash(),
                    u.getNome(),
                    u.getCognome(),
                    u.getCodiceFiscale(),
                    u.getEmail()
            )));
            bw.write("\n");
        }
    }
}
