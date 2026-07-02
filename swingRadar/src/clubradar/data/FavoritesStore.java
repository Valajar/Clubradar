package clubradar.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Diese Klasse wurde von Claude Fable 5 erstellt.

/**
 * Persistente Favoritenlisten: pro Benutzername die Menge der Clubs,
 * die er als Favorit markiert hat (favorites.json).
 *
 * Format: [{ "user": "...", "clubs": "Club A¿Club B" }, ...]
 * Die Clubnamen werden mit "¿" verbunden — dasselbe Trennzeichen,
 * das auch die Bewertungen benutzen.
 */
public class FavoritesStore {

    private static final String CLUB_SEPARATOR = "¿";

    private static FavoritesStore instance;

    private final Map<String, Set<String>> favoritesByUser = new LinkedHashMap<>();
    private final File favoritesFile = new File("favorites.json");

    private FavoritesStore() {
        load();
    }

    public static FavoritesStore getInstance() {
        if (instance == null) {
            instance = new FavoritesStore();
        }
        return instance;
    }

    public boolean isFavorite(String username, String clubName) {
        Set<String> clubs = favoritesByUser.get(username);
        return clubs != null && clubs.contains(clubName);
    }

    /** Schaltet den Favoriten-Status um und gibt den neuen Status zurück. */
    public boolean toggleFavorite(String username, String clubName) {
        Set<String> clubs = favoritesByUser.computeIfAbsent(username, k -> new LinkedHashSet<>());
        boolean nowFavorite;
        if (clubs.contains(clubName)) {
            clubs.remove(clubName);
            nowFavorite = false;
        } else {
            clubs.add(clubName);
            nowFavorite = true;
        }
        save();
        return nowFavorite;
    }

    // --- Persistenz ---------------------------------------------------------

    private void load() {
        if (!favoritesFile.exists()) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(favoritesFile.toPath()), StandardCharsets.UTF_8);
            for (String object : JsonUtil.splitObjects(json)) {
                String user = JsonUtil.extractString(object, "user");
                String clubs = JsonUtil.extractString(object, "clubs");
                if (user == null || clubs == null) {
                    continue;
                }
                Set<String> clubSet = new LinkedHashSet<>();
                for (String club : clubs.split(CLUB_SEPARATOR)) {
                    if (!club.isBlank()) {
                        clubSet.add(club);
                    }
                }
                favoritesByUser.put(user, clubSet);
            }
        } catch (IOException e) {
            System.out.println("favorites.json konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    private void save() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        int i = 0;
        for (Map.Entry<String, Set<String>> entry : favoritesByUser.entrySet()) {
            json.append("  {\n");
            json.append("    \"user\": \"").append(JsonUtil.escape(entry.getKey())).append("\",\n");
            json.append("    \"clubs\": \"").append(JsonUtil.escape(String.join(CLUB_SEPARATOR, entry.getValue()))).append("\"\n");
            json.append("  }");
            if (i < favoritesByUser.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            i++;
        }
        json.append("]\n");

        try {
            Files.write(favoritesFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("favorites.json konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
