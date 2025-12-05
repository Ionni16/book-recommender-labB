package bookrecommender.repo;

import bookrecommender.model.Review;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ValutazioniRepository {
    private final Path file;
    public ValutazioniRepository(Path file) { this.file = file; }

    public List<Review> findByBookId(int bookId) throws IOException {
        List<Review> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                String[] c = line.split(";", -1);
                if (c.length < 9) continue;
                int bId;
                try { bId = Integer.parseInt(c[1]); } catch (NumberFormatException e) { continue; }
                if (bId != bookId) continue;

                try {
                    int stile = Integer.parseInt(c[2]);
                    int contenuto = Integer.parseInt(c[3]);
                    int gradevolezza = Integer.parseInt(c[4]);
                    int originalita = Integer.parseInt(c[5]);
                    int edizione = Integer.parseInt(c[6]);
                    int votoFinale = Integer.parseInt(c[7]);
                    String commento = c[8];
                    out.add(new Review(c[0], bId, stile, contenuto, gradevolezza, originalita, edizione, votoFinale, commento));
                } catch (NumberFormatException ignored) {}
            }
        }
        return out;
    }

    public void append(Review r) throws IOException {
        boolean exists = Files.exists(file);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (!exists) {
                bw.write("userid;idLibro;stile;contenuto;gradevolezza;originalita;edizione;votoFinale;commento\n");
            }
            bw.write(String.join(";", Arrays.asList(
                r.getUserid(),
                Integer.toString(r.getBookId()),
                Integer.toString(r.getStile()),
                Integer.toString(r.getContenuto()),
                Integer.toString(r.getGradevolezza()),
                Integer.toString(r.getOriginalita()),
                Integer.toString(r.getEdizione()),
                Integer.toString(r.getVotoFinale()),
                r.getCommento() == null ? "" : r.getCommento().replace("\n"," ").trim()
            )));
            bw.write("\n");
        }
    }
}
