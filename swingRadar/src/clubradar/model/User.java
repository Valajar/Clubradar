package clubradar.model;

import clubradar.data.PasswordHasher;

// Diese Klasse hieß früher "Layer8" und wurde von Claude Fable 5
// umbenannt und überarbeitet (englische Namen, Role-Enum statt String).

public class User {
    private final String username;
    private String passwordHash;
    private final boolean stayLoggedIn;
    private final Role role;

    public User(String username, String password, boolean stayLoggedIn, Role role) {
        this.username = username;
        this.passwordHash = PasswordHasher.hash(password);
        this.stayLoggedIn = stayLoggedIn;
        this.role = role;
    }

    /** Baut einen User aus bereits gespeicherten Daten (users.json) wieder auf. */
    public static User fromStoredData(String username, String passwordHash, boolean stayLoggedIn, Role role) {
        User user = new User(username, "", stayLoggedIn, role);
        user.passwordHash = passwordHash;
        return user;
    }

    /** Ein Gast ohne Konto — wird nirgendwo gespeichert. */
    public static User guest() {
        return new User("Gast", "", false, Role.GUEST);
    }

    public boolean checkPassword(String password) {
        return PasswordHasher.hash(password).equals(this.passwordHash);
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isStayLoggedIn() { return stayLoggedIn; }
    public Role getRole() { return role; }
}
