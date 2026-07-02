package clubradar.model;

import java.util.ArrayList;
import java.util.List;

// Zusammengeführt aus den früheren Klassen "Rating" und "RatingManager",
// von Claude Fable 5 überarbeitet (englische Namen, alles Bewertungs-
// bezogene in einer Klasse).

/**
 * Eine einzelne Bewertung: Autor, Sterne (1..5, 0 = keine Angabe) und
 * optionaler Text.
 *
 * Speicherformat einer Bewertung:   autor∞sterne∞text
 * Mehrere Bewertungen werden mit "¿" getrennt.
 */
public class Rating {

    private static final String RATING_SEPARATOR = "¿"; // zwischen zwei Bewertungen
    private static final String FIELD_SEPARATOR = "∞";  // zwischen Autor/Sterne/Text

    private final String author;
    private final int stars;   // 1..5, 0 = keine Sterne angegeben (alte Daten)
    private final String text;

    public Rating(String author, int stars, String text) {
        this.author = author;
        this.stars = stars;
        this.text = text;
    }

    public String getAuthor() { return author; }
    public int getStars() { return stars; }
    public String getText() { return text; }

    public String toStorageString() {
        return author + FIELD_SEPARATOR + stars + FIELD_SEPARATOR + text;
    }

    // --- Umwandlung Rohstring <-> Liste ------------------------------------

    /** Parst den Rohstring eines Clubs in eine Liste von Bewertungen. */
    public static List<Rating> parseList(String raw) {
        List<Rating> ratings = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return ratings;
        }

        String[] entries = raw.split(RATING_SEPARATOR + "\\s*");
        for (String entry : entries) {
            if (entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split(FIELD_SEPARATOR + "\\s*", 3);
            String author = parts[0].trim();
            int stars = 0;
            String text = "";
            if (parts.length == 3) {
                stars = parseStars(parts[1].trim());
                text = parts[2].trim();
            } else if (parts.length == 2) {
                // Altes Format: entweder "autor∞sterne" oder "autor∞text"
                Integer maybeStars = tryParseStars(parts[1].trim());
                if (maybeStars != null) {
                    stars = maybeStars;
                } else {
                    text = parts[1].trim();
                }
            }
            ratings.add(new Rating(author, stars, text));
        }
        return ratings;
    }

    /** Macht aus einer Liste wieder den Rohstring fürs Speichern. */
    public static String listToStorageString(List<Rating> ratings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ratings.size(); i++) {
            if (i > 0) {
                sb.append(RATING_SEPARATOR);
            }
            sb.append(ratings.get(i).toStorageString());
        }
        return sb.toString();
    }

    /**
     * Fügt eine Bewertung hinzu ODER ersetzt die vorhandene Bewertung
     * desselben Autors — maximal eine Bewertung pro Nutzer pro Club.
     */
    public static void addOrReplace(List<Rating> ratings, Rating newRating) {
        ratings.removeIf(r -> r.getAuthor().equals(newRating.getAuthor()));
        ratings.add(newRating);
    }

    // --- kleine Helfer für die Sterne ---------------------------------------

    private static int parseStars(String value) {
        Integer stars = tryParseStars(value);
        return stars != null ? stars : 0;
    }

    private static Integer tryParseStars(String value) {
        try {
            int stars = Integer.parseInt(value);
            if (stars >= 1 && stars <= 5) {
                return stars;
            }
        } catch (NumberFormatException ignored) {
            // kein gültiger Integer -> null
        }
        return null;
    }
}
