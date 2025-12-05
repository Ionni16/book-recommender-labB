package bookrecommender.repo;

import bookrecommender.model.Suggestion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ConsigliRepository {
    private final Path file;
    public ConsigliRepository(Path file) { this.file = file; }

    public List<Suggestion> findByBookId(int bookId) throws IOException {
        List<Suggestion> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                String[] c = line.split(";", -1);
                if (c.length < 5) continue;
                int bId;
                try { bId = Integer.parseInt(c[1]); } catch (NumberFormatException e) { continue; }
                if (bId != bookId) continue;

                List<Integer> sug = new ArrayList<>();
                for (int i = 2; i <= 4; i++) {
                    if (!c[i].isEmpty()) {
                        try { sug.add(Integer.parseInt(c[i])); } catch (NumberFormatException ignored) {}
                    }
                }
                out.add(new Suggestion(c[0], bId, sug));
            }
        }
        return out;
    }

    public void append(Suggestion s) throws IOException {
        boolean exists = Files.exists(file);
        List<Integer> list = s.getSuggeriti();
        int id1 = list.size() > 0 ? list.get(0) : 0;
        int id2 = list.size() > 1 ? list.get(1) : 0;
        int id3 = list.size() > 2 ? list.get(2) : 0;

        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (!exists) {
                bw.write("userid;idLibro;idSuggerito1;idSuggerito2;idSuggerito3\n");
            }
            bw.write(s.getUserid() + ";" + s.getBookId() + ";" +
                    (id1==0?"":id1) + ";" + (id2==0?"":id2) + ";" + (id3==0?"":id3) + "\n");
        }
    }
}
