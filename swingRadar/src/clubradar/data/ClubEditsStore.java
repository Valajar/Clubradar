package clubradar.data;

import clubradar.model.Club;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

// Diese Klasse wurde von Claude Fable 5 erstellt.

/**
 * Persistente Betreiber-Daten pro Club (clubedits.json):
 * - welcher Betreiber den Club "beansprucht" hat,
 * - von ihm gepflegte Adresse, Öffnungszeiten und Website.
 *
 * Die Clubdaten selbst kommen bei jedem Start frisch aus der OSM-API.
 * Damit Betreiber-Änderungen einen Neustart überleben, werden sie hier
 * separat gespeichert und in {@link #applyTo(Club)} über die API-Daten
 * "drübergelegt".
 */
public class ClubEditsStore {

    /** Ein gespeicherter Eintrag für einen Club. */
    public static class ClubEdit {
        public String owner;        // Benutzername des Betreibers
        public String address;      // null = nicht überschrieben
        public String openingHours; // null = nicht überschrieben
        public String website;      // null = nicht überschrieben
    }

    private static ClubEditsStore instance;

    private final Map<String, ClubEdit> editsByClub = new LinkedHashMap<>();
    private final File editsFile = new File("clubedits.json");

    private ClubEditsStore() {
        load();
    }

    public static ClubEditsStore getInstance() {
        if (instance == null) {
            instance = new ClubEditsStore();
        }
        return instance;
    }

    /** Überträgt gespeicherte Betreiber-Daten auf einen frisch geladenen Club. */
    public void applyTo(Club club) {
        ClubEdit edit = editsByClub.get(club.getName());
        if (edit == null) {
            return;
        }
        if (edit.owner != null && !edit.owner.isBlank()) {
            club.setOwnerUsername(edit.owner);
        }
        if (edit.address != null && !edit.address.isBlank()) {
            club.setAddress(edit.address);
        }
        if (edit.openingHours != null && !edit.openingHours.isBlank()) {
            club.setOpeningHours(edit.openingHours);
        }
        if (edit.website != null) {
            club.setWebsite(edit.website);
        }
    }

    /** Ein Betreiber beansprucht einen Club für sich. */
    public void claimClub(String clubName, String ownerUsername) {
        ClubEdit edit = editsByClub.computeIfAbsent(clubName, k -> new ClubEdit());
        edit.owner = ownerUsername;
        save();
    }

    /** Admin: Beanspruchung (und damit die Betreiber-Änderungen) entfernen. */
    public void removeClaim(String clubName) {
        editsByClub.remove(clubName);
        save();
    }

    /** Speichert die vom Betreiber bearbeiteten Club-Infos. */
    public void saveDetails(String clubName, String address, String openingHours, String website) {
        ClubEdit edit = editsByClub.computeIfAbsent(clubName, k -> new ClubEdit());
        edit.address = address;
        edit.openingHours = openingHours;
        edit.website = website;
        save();
    }

    // --- Persistenz ---------------------------------------------------------

    private void load() {
        if (!editsFile.exists()) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(editsFile.toPath()), StandardCharsets.UTF_8);
            for (String object : JsonUtil.splitObjects(json)) {
                String club = JsonUtil.extractString(object, "club");
                if (club == null) {
                    continue;
                }
                ClubEdit edit = new ClubEdit();
                edit.owner = JsonUtil.extractString(object, "owner");
                edit.address = JsonUtil.extractString(object, "address");
                edit.openingHours = JsonUtil.extractString(object, "openingHours");
                edit.website = JsonUtil.extractString(object, "website");
                editsByClub.put(club, edit);
            }
        } catch (IOException e) {
            System.out.println("clubedits.json konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    private void save() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        int i = 0;
        for (Map.Entry<String, ClubEdit> entry : editsByClub.entrySet()) {
            ClubEdit edit = entry.getValue();
            json.append("  {\n");
            json.append("    \"club\": \"").append(JsonUtil.escape(entry.getKey())).append("\",\n");
            json.append("    \"owner\": \"").append(JsonUtil.escape(edit.owner)).append("\",\n");
            json.append("    \"address\": \"").append(JsonUtil.escape(edit.address)).append("\",\n");
            json.append("    \"openingHours\": \"").append(JsonUtil.escape(edit.openingHours)).append("\",\n");
            json.append("    \"website\": \"").append(JsonUtil.escape(edit.website)).append("\"\n");
            json.append("  }");
            if (i < editsByClub.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            i++;
        }
        json.append("]\n");

        try {
            Files.write(editsFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("clubedits.json konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
