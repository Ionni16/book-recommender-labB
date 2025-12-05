package bookrecommender.service;

import bookrecommender.model.Book;
import bookrecommender.repo.LibriRepository;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SearchService {
    private final LibriRepository libriRepo;
    public SearchService(LibriRepository libriRepo) { this.libriRepo = libriRepo; }

    public List<Book> cercaLibroPerTitolo(String q) {
        String needle = norm(q);
        if (needle.isEmpty()) return List.of();
        return libriRepo.all().stream()
                .filter(b -> norm(b.getTitolo()).contains(needle))
                .collect(Collectors.toList());
    }

    public List<Book> cercaLibroPerAutore(String a) {
        String needle = norm(a);
        if (needle.isEmpty()) return List.of();
        return libriRepo.all().stream()
                .filter(b -> b.getAutori().stream().anyMatch(x -> norm(x).contains(needle)))
                .collect(Collectors.toList());
    }

    public List<Book> cercaLibroPerAutoreEAnno(String a, int anno) {
        String needle = norm(a);
        return libriRepo.all().stream()
                .filter(b -> (b.getAnno() != null && b.getAnno() == anno))
                .filter(b -> b.getAutori().stream().anyMatch(x -> norm(x).contains(needle)))
                .collect(Collectors.toList());
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ITALIAN);
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // toglie accenti
        return t;
    }
}
