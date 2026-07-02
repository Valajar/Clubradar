package clubradar.model;

// Diese Klasse wurde von Claude Fable 5 erstellt.

/**
 * Die vier Benutzerrollen der Anwendung.
 *
 * Jede Rolle hat ein deutsches Label, das in der Oberfläche angezeigt und
 * in users.json gespeichert wird (dadurch bleiben alte Speicherdateien,
 * die "Benutzer"/"Betreiber"/... enthalten, weiter lesbar).
 */
public enum Role {
    USER("Benutzer"),
    GUEST("Gast"),
    OWNER("Betreiber"),
    ADMIN("Admin");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Findet die Rolle zu einem gespeicherten Label; unbekannt -> USER. */
    public static Role fromLabel(String label) {
        for (Role role : values()) {
            if (role.label.equalsIgnoreCase(label)) {
                return role;
            }
        }
        return USER;
    }
}
