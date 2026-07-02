package clubradar.data;

import clubradar.model.Club;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Diese Klasse hieß früher "ClubDatabase" und wurde von Claude Fable 5
// umbenannt und überarbeitet (englische Namen, Singleton statt statischer
// Konstruktor-Seiteneffekte, Club-Objekte statt String-Arrays).
// Die API-Aufruf- und JSON-Parse-Methoden stammen inhaltlich aus der alten
// Klasse (Teile davon mit Cascade ai SWE-1.6 erstellt, siehe Git-Historie).

/**
 * Lädt und hält die Liste aller Clubs.
 *
 * Quellen:
 * - manuell gepflegte Datei swingRadar/data/Clubs.txt,
 * - OSM Overpass API (Standard),
 * - optional HERE / Google Places (per Flag).
 *
 * Nach dem Laden werden Duplikate entfernt und gespeicherte Bewertungen
 * (RatingStore) sowie Betreiber-Änderungen (ClubEditsStore) angewendet.
 */
public class ClubRepository {

    private static ClubRepository instance;

    private final List<Club> clubs = new ArrayList<>();

    private final File localClubFile = new File("swingRadar/data/Clubs.txt");

    // Eigene API-Keys hier eintragen, wenn HERE/Google genutzt werden sollen.
    // (Hinweis von Claude Fable 5: Keys liegen im Klartext in der Git-Historie
    // und sollten in der jeweiligen Konsole gesperrt/erneuert werden.)
    private static final String GOOGLE_API_KEY = "AIzaSyCB1u0DkWFBy7Y56oQRKnyb5KNPpQzlx_g";
    private static final String HERE_API_KEY = "LBC9Rmlvvs5QorKjbbRse9K8G8Wcwvx2kEe8zdVjCHc";

    /* APIs aktivieren oder deaktivieren
       Für Tests bitte nur OSM aktivieren */
    private static final boolean USE_HERE_API = false;
    private static final boolean USE_GOOGLE_API = false;
    private static final boolean USE_OSM_API = true;

    private ClubRepository() {
    }

    public static synchronized ClubRepository getInstance() {
        if (instance == null) {
            instance = new ClubRepository();
        }
        return instance;
    }

    public List<Club> getClubs() {
        return clubs;
    }

    /** Lädt alle Clubs im angegebenen Kartenausschnitt (Bounding-Box). */
    public void loadClubs(double minLat, double minLon, double maxLat, double maxLon) {
        loadLocalClubs();

        if (USE_HERE_API) {
            System.out.println("HERE API");
            fetchFromHere(minLat, minLon, maxLat, maxLon);
        }
        if (USE_GOOGLE_API) {
            System.out.println("Google Places API");
            fetchFromGoogle(minLat, minLon, maxLat, maxLon);
        }
        if (USE_OSM_API) {
            System.out.println("OSM");
            fetchFromOsm(minLat, minLon, maxLat, maxLon);
        }

        removeDuplicates();
        applyStoredData();
        System.out.println("Anzahl an Clubs: " + clubs.size());
    }

    /**
     * Liest die manuell gepflegte Clubdatei.
     * Zeilenformat: lat¿lon¿name¡adresse¿öffnungszeiten¿website¡bewertungen
     */
    private void loadLocalClubs() {
        if (!localClubFile.exists()) {
            return;
        }
        System.out.println("Lese lokale Clubdatei: " + localClubFile.getPath());
        try (Scanner reader = new Scanner(localClubFile, StandardCharsets.UTF_8)) {
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (line.isBlank()) {
                    continue;
                }
                String[] sections = line.split("¡\\s*");
                String[] general = sections[0].split("¿\\s*");
                String[] details = sections[1].split("¿\\s*");
                String ratings = sections.length > 2 ? sections[2] : "";

                double lat = Double.parseDouble(general[0]);
                double lon = Double.parseDouble(general[1]);
                String name = general[2];
                String address = details.length > 0 ? details[0] : "Adresse unbekannt";
                String hours = details.length > 1 ? details[1] : "Öffnungszeiten unbekannt";
                String website = details.length > 2 ? details[2] : "";

                clubs.add(new Club(new GeoPosition(lat, lon), name, address, hours, website, ratings));
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Lesen der Clubdatei: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Wendet gespeicherte Bewertungen und Betreiber-Änderungen auf alle Clubs an. */
    private void applyStoredData() {
        for (Club club : clubs) {
            String persistedRatings = RatingStore.getInstance().getRatings(club.getName());
            if (persistedRatings != null) {
                club.setRatingsRaw(persistedRatings);
            }
            ClubEditsStore.getInstance().applyTo(club);
        }
    }

    private void removeDuplicates() {
        List<Club> deduplicated = new ArrayList<>();
        List<String> seenNames = new ArrayList<>();
        int duplicateCount = 0;

        for (Club club : clubs) {
            String normalizedName = normalizeName(club.getName());
            if (!seenNames.contains(normalizedName)) {
                deduplicated.add(club);
                seenNames.add(normalizedName);
            } else {
                duplicateCount++;
            }
        }

        System.out.println("Entfernte Dopplungen: " + duplicateCount);
        clubs.clear();
        clubs.addAll(deduplicated);
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("[^a-z0-9]", "");
    }

    // --- HERE API ------------------------------------------------------------

    private void fetchFromHere(double minLat, double minLon, double maxLat, double maxLon) {
        try {
            String urlString = "https://discover.search.hereapi.com/v1/discover" +
                    "?at=" + ((minLat + maxLat) / 2) + "," + ((minLon + maxLon) / 2) +
                    "&q=nightclub" +
                    "&in=bbox:" + minLat + "," + minLon + "," + maxLat + "," + maxLon +
                    "&apiKey=" + HERE_API_KEY +
                    "&limit=100";

            String response = httpGet(urlString);
            if (response == null) {
                return;
            }
            parseHereResponse(response);

        } catch (Exception e) {
            System.out.println("HERE API Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseHereResponse(String jsonResponse) {
        String itemsKey = "\"items\":";
        int itemsStart = jsonResponse.indexOf(itemsKey);
        if (itemsStart == -1) {
            System.out.println("HERE API: Antwort ist leer");
            return;
        }

        String itemsSection = jsonResponse.substring(itemsStart + itemsKey.length());
        int arrayEnd = findMatchingBracket(itemsSection, 0);
        if (arrayEnd == -1) {
            System.out.println("HERE API: Ungültiges Format");
            return;
        }

        String items = itemsSection.substring(0, arrayEnd + 1);

        int index = 0;
        int clubCount = 0;
        while (index < items.length()) {
            int itemStart = items.indexOf("{", index);
            if (itemStart == -1) break;

            int itemEnd = findMatchingBracket(items, itemStart);
            if (itemEnd == -1) break;

            String item = items.substring(itemStart, itemEnd + 1);

            String name = extractJsonValue(item, "title");
            String lat = extractJsonValue(item, "lat");
            String lon = extractJsonValue(item, "lon");
            String address = extractJsonValue(item, "address");

            if (name != null && lat != null && lon != null) {
                clubs.add(new Club(
                        new GeoPosition(Double.parseDouble(lat), Double.parseDouble(lon)),
                        name,
                        address != null ? address : "Adresse unbekannt",
                        "Öffnungszeiten unbekannt",
                        "",
                        ""));
                clubCount++;
            }
            index = itemEnd + 1;
        }
        System.out.println("HERE API: " + clubCount + " Clubs geladen");
    }

    // --- Google Places API -----------------------------------------------------
    // (ursprünglich mit Cascade ai SWE-1.6 erstellt)

    private void fetchFromGoogle(double minLat, double minLon, double maxLat, double maxLon) {
        try {
            String location = ((minLat + maxLat) / 2) + "," + ((minLon + maxLon) / 2);
            String radius = String.valueOf(searchRadius(minLat, minLon, maxLat, maxLon));

            String urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=" + location +
                    "&radius=" + radius +
                    "&type=night_club" +
                    "&key=" + GOOGLE_API_KEY;

            String response = httpGet(urlString);
            if (response == null) {
                return;
            }
            parseGoogleResponse(response);

        } catch (Exception e) {
            System.out.println("Google API Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseGoogleResponse(String jsonResponse) {
        String status = extractJsonValue(jsonResponse, "status");
        if (status != null && !status.equals("OK")) {
            System.out.println("Google API Fehler: " + extractJsonValue(jsonResponse, "error_message"));
            return;
        }

        String resultsSection = extractSection(jsonResponse, "results");
        if (resultsSection == null) {
            System.out.println("Google API: Keine Antwort");
            return;
        }

        int index = 0;
        int clubCount = 0;
        while (index < resultsSection.length()) {
            int resultStart = resultsSection.indexOf("{", index);
            if (resultStart == -1) break;

            int resultEnd = findMatchingBracket(resultsSection, resultStart);
            if (resultEnd == -1) break;

            String result = resultsSection.substring(resultStart, resultEnd + 1);

            String name = extractJsonValue(result, "name");
            String vicinity = extractJsonValue(result, "vicinity");

            String geometrySection = extractSection(result, "geometry");
            String lat = null;
            String lon = null;
            if (geometrySection != null) {
                String locationSection = extractSection(geometrySection, "location");
                if (locationSection != null) {
                    lat = extractJsonValue(locationSection, "lat");
                    lon = extractJsonValue(locationSection, "lng");
                }
            }

            if (name != null && lat != null && lon != null) {
                clubs.add(new Club(
                        new GeoPosition(Double.parseDouble(lat), Double.parseDouble(lon)),
                        name,
                        vicinity != null ? vicinity : "Adresse unbekannt",
                        "Öffnungszeiten unbekannt",
                        "",
                        ""));
                clubCount++;
            }
            index = resultEnd + 1;
        }
        System.out.println("Google API: " + clubCount + " Clubs geladen");
    }

    private double searchRadius(double minLat, double minLon, double maxLat, double maxLon) {
        double latDiff = maxLat - minLat;
        double lonDiff = maxLon - minLon;
        double radius = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111000 / 2;
        return Math.min(radius, 50000);
    }

    // --- OSM Overpass API ------------------------------------------------------

    private void fetchFromOsm(double minLat, double minLon, double maxLat, double maxLon) {
        try {
            String bbox = minLat + "," + minLon + "," + maxLat + "," + maxLon;
            String query = "[out:json][timeout:25];" +
                    "nwr[\"amenity\"~\"bar|nightclub|pub\"](" + bbox + ");" +
                    "out center;";

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String response = httpGet("https://overpass-api.de/api/interpreter?data=" + encodedQuery);
            if (response == null) {
                return;
            }
            parseOsmResponse(response);

        } catch (Exception e) {
            System.out.println("OSM API Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseOsmResponse(String jsonResponse) {
        int elementsStart = jsonResponse.indexOf("\"elements\":");
        if (elementsStart == -1) {
            System.out.println("OSM API: Geladene Daten sind leer");
            return;
        }

        String elementsSection = jsonResponse.substring(elementsStart + 11);
        int arrayEnd = findMatchingBracket(elementsSection, 0);
        if (arrayEnd == -1) {
            System.out.println("OSM API: Ungültiges Format");
            return;
        }

        String elements = elementsSection.substring(0, arrayEnd + 1);

        int index = 0;
        int clubCount = 0;
        while (index < elements.length()) {
            int elementStart = elements.indexOf("{", index);
            if (elementStart == -1) break;

            int elementEnd = findMatchingBracket(elements, elementStart);
            if (elementEnd == -1) break;

            String element = elements.substring(elementStart, elementEnd + 1);

            String tagsSection = extractTagsSection(element);

            String name = extractJsonValue(tagsSection, "name");
            String lat = extractJsonValue(element, "lat");
            String lon = extractJsonValue(element, "lon");
            String website = extractJsonValue(tagsSection, "website");
            String openingHours = extractJsonValue(tagsSection, "opening_hours");
            String street = extractJsonValue(tagsSection, "addr:street");
            String city = extractJsonValue(tagsSection, "addr:city");
            String houseNumber = extractJsonValue(tagsSection, "addr:housenumber");

            if (name != null && lat != null && lon != null) {
                String address = "";
                if (street != null) {
                    address = street;
                    if (houseNumber != null) {
                        address += " " + houseNumber;
                    }
                    if (city != null) {
                        address += ", " + city;
                    }
                }

                clubs.add(new Club(
                        new GeoPosition(Double.parseDouble(lat), Double.parseDouble(lon)),
                        name,
                        address.isEmpty() ? "Adresse unbekannt" : address,
                        openingHours != null ? openingHours : "Öffnungszeiten unbekannt",
                        website != null ? website : "",
                        ""));
                clubCount++;
            }
            index = elementEnd + 1;
        }
        System.out.println("OSM API: " + clubCount + " Clubs geladen");
    }

    // --- HTTP- und JSON-Helfer ---------------------------------------------------
    // (JSON-Parsing ursprünglich mit Cascade ai SWE-1.6 erstellt)

    /** Führt einen GET-Request aus und gibt den Body zurück (null bei Fehler). */
    private String httpGet(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("HTTP-Fehler " + conn.getResponseCode() + " bei " + urlString);
                return null;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            return response.toString();

        } catch (Exception e) {
            System.out.println("HTTP-Fehler: " + e.getMessage());
            return null;
        }
    }

    private String extractSection(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStart = skipWhitespace(json, keyIndex + searchKey.length());
        if (valueStart < json.length() && json.charAt(valueStart) == ':') {
            valueStart = skipWhitespace(json, valueStart + 1);
        }
        if (valueStart >= json.length()) return null;

        char c = json.charAt(valueStart);
        if (c == '{' || c == '[') {
            int sectionEnd = findMatchingBracket(json, valueStart);
            if (sectionEnd == -1) return null;
            return json.substring(valueStart, sectionEnd + 1);
        }
        return null;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStart = skipWhitespace(json, keyIndex + searchKey.length());
        if (valueStart < json.length() && json.charAt(valueStart) == ':') {
            valueStart = skipWhitespace(json, valueStart + 1);
        }
        if (valueStart >= json.length()) return null;

        char c = json.charAt(valueStart);
        if (c == '"') {
            int valueEnd = json.indexOf("\"", valueStart + 1);
            if (valueEnd == -1) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() &&
                    (Character.isLetterOrDigit(json.charAt(valueEnd)) ||
                     json.charAt(valueEnd) == '.' ||
                     json.charAt(valueEnd) == '-' ||
                     json.charAt(valueEnd) == '+')) {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        }
    }

    private String extractTagsSection(String element) {
        int tagsStart = element.indexOf("\"tags\":");
        if (tagsStart == -1) return element;

        int valueStart = skipWhitespace(element, tagsStart + 7);
        if (valueStart >= element.length() || element.charAt(valueStart) != '{') {
            return element;
        }

        int tagsEnd = findMatchingBracket(element, valueStart);
        if (tagsEnd == -1) return element;

        return element.substring(valueStart, tagsEnd + 1);
    }

    private int skipWhitespace(String s, int index) {
        while (index < s.length() && Character.isWhitespace(s.charAt(index))) {
            index++;
        }
        return index;
    }

    private int findMatchingBracket(String s, int start) {
        int bracketCount = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '{') bracketCount++;
            else if (c == ']' || c == '}') {
                bracketCount--;
                if (bracketCount == 0) return i;
            }
        }
        return -1;
    }
}
