package clubradar.data;

import java.util.ArrayList;
import java.util.List;

// --------------------------------------
// Von Claude Fable 5 erstellt: Die kleinen JSON-Helfer standen vorher
// doppelt in UserManager und RatingStore — jetzt einmal zentral hier.
// Sie müssen nur das Format verstehen, das die Stores selbst schreiben.
// --------------------------------------

public class JsonUtil {

    /** Zerlegt ein Top-Level-Array in den Text der einzelnen "{ ... }"-Objekte. */
    public static List<String> splitObjects(String json) {
        List<String> objects = new ArrayList<>();
        int i = 0;
        while (true) {
            int start = json.indexOf('{', i);
            if (start == -1) break;

            // Klammern mitzählen, damit auch verschachtelte "{...}" funktionieren.
            int depth = 0;
            int end = -1;
            for (int j = start; j < json.length(); j++) {
                char c = json.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { end = j; break; }
                }
            }
            if (end == -1) break;

            objects.add(json.substring(start, end + 1));
            i = end + 1;
        }
        return objects;
    }

    /** Liest den String-Wert von "key" aus einem einzelnen JSON-Objekt. */
    public static String extractString(String object, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = object.indexOf(search);
        if (keyIndex == -1) return null;

        int valueStart = object.indexOf('"', keyIndex + search.length());
        if (valueStart == -1) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = valueStart + 1; i < object.length(); i++) {
            char c = object.charAt(i);
            if (c == '\\' && i + 1 < object.length()) {
                char next = object.charAt(++i); // Escape auflösen
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default:  sb.append(next); // deckt \" und \\ ab
                }
            } else if (c == '"') {
                break; // schließendes Anführungszeichen -> Wert komplett
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Liest den Boolean-Wert von "key" aus einem einzelnen JSON-Objekt. */
    public static boolean extractBoolean(String object, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = object.indexOf(search);
        if (keyIndex == -1) return false;

        int colon = object.indexOf(':', keyIndex + search.length());
        if (colon == -1) return false;

        return object.substring(colon + 1).trim().startsWith("true");
    }

    /** Escaped die Zeichen, die einen JSON-String kaputt machen würden. */
    public static String escape(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
