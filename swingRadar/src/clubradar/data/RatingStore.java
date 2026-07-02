package clubradar.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

// Von Claude Fable 5 überarbeitet: JSON-Helfer nach JsonUtil ausgelagert.
// Speicherformat (ratings.json) ist unverändert.

/**
 * Persistente Speicherung der Club-Bewertungen, Schlüssel ist der Club-Name.
 */
public class RatingStore {

    private static RatingStore instance;

    private final Map<String, String> ratingsByClub = new LinkedHashMap<>();
    private final File ratingFile = new File("ratings.json");

    private RatingStore() {
        load();
    }

    public static RatingStore getInstance() {
        if (instance == null) {
            instance = new RatingStore();
        }
        return instance;
    }

    /** Rohstring der Bewertungen eines Clubs oder null, wenn nichts gespeichert ist. */
    public String getRatings(String clubName) {
        return ratingsByClub.get(clubName);
    }

    public void setRatings(String clubName, String rawRatings) {
        ratingsByClub.put(clubName, rawRatings);
        save();
    }

    // --- Persistenz ---------------------------------------------------------

    private void load() {
        if (!ratingFile.exists()) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(ratingFile.toPath()), StandardCharsets.UTF_8);
            for (String object : JsonUtil.splitObjects(json)) {
                String club = JsonUtil.extractString(object, "club");
                String ratings = JsonUtil.extractString(object, "ratings");
                if (club != null && ratings != null) {
                    ratingsByClub.put(club, ratings);
                }
            }
        } catch (IOException e) {
            System.out.println("ratings.json konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    private void save() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        int i = 0;
        for (Map.Entry<String, String> entry : ratingsByClub.entrySet()) {
            json.append("  {\n");
            json.append("    \"club\": \"").append(JsonUtil.escape(entry.getKey())).append("\",\n");
            json.append("    \"ratings\": \"").append(JsonUtil.escape(entry.getValue())).append("\"\n");
            json.append("  }");
            if (i < ratingsByClub.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            i++;
        }
        json.append("]\n");

        try {
            Files.write(ratingFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("ratings.json konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
