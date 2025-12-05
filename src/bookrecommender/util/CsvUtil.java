package bookrecommender.util;

import java.util.ArrayList;
import java.util.List;

public class CsvUtil {
    public static String[] splitSemicolonRow(String line) {
        return line.split(";", -1);
    }
    public static List<String> splitAuthorsPipe(String field) {
        List<String> out = new ArrayList<>();
        if (field == null || field.isEmpty()) return out;
        for (String p : field.split("\\|")) {
            String s = p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
