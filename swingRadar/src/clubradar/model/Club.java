package clubradar.model;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.List;

// Zusammengeführt aus den früheren Klassen "clubinfos" und "infoWaypoint",
// von Claude Fable 5 überarbeitet: statt eines String-Arrays ("beschreibung")
// gibt es jetzt benannte Felder für Adresse, Öffnungszeiten und Website.

/**
 * Ein Club auf der Karte. Erbt von DefaultWaypoint, damit er direkt als
 * Marker im JXMapViewer verwendet werden kann.
 */
public class Club extends DefaultWaypoint {

    private final String name;
    private String address;
    private String openingHours;
    private String website;

    /** Benutzername des Betreibers, der den Club verwaltet; null = niemand. */
    private String ownerUsername;

    /** Rohstring aller Bewertungen (Format siehe {@link Rating}). */
    private String ratingsRaw;

    public Club(GeoPosition position, String name, String address,
                String openingHours, String website, String ratingsRaw) {
        super(position);
        this.name = name;
        this.address = address;
        this.openingHours = openingHours;
        this.website = website;
        this.ratingsRaw = ratingsRaw;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getOpeningHours() { return openingHours; }
    public String getWebsite() { return website; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getRatingsRaw() { return ratingsRaw; }

    public void setAddress(String address) { this.address = address; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setWebsite(String website) { this.website = website; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public void setRatingsRaw(String ratingsRaw) { this.ratingsRaw = ratingsRaw; }

    public boolean hasWebsite() {
        return website != null && !website.isBlank();
    }

    public List<Rating> getRatings() {
        return Rating.parseList(ratingsRaw);
    }

    /** Durchschnitt aller Sternebewertungen (ohne 0-Sterne-Alteinträge), 0 wenn keine. */
    public double getAverageStars() {
        List<Rating> ratings = getRatings();
        int sum = 0;
        int count = 0;
        for (Rating rating : ratings) {
            if (rating.getStars() > 0) {
                sum += rating.getStars();
                count++;
            }
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    public int getRatingCount() {
        return getRatings().size();
    }
}
