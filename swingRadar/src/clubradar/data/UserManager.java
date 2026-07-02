package clubradar.data;

import clubradar.model.Role;
import clubradar.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// Von Claude Fable 5 überarbeitet: englische Namen, Role-Enum,
// JSON-Helfer nach JsonUtil ausgelagert. Speicherformat (users.json)
// ist unverändert, alte Dateien bleiben lesbar.

/**
 * Zentrales User-Management:
 * - hält die Liste aller bekannten Nutzer im Speicher,
 * - lädt sie beim Start aus users.json,
 * - speichert sie zurück, wenn ein neuer Nutzer registriert wird.
 */
public class UserManager {

    private static UserManager instance;

    private final List<User> users = new ArrayList<>();
    private User currentUser;
    private final File userFile = new File("users.json");

    private UserManager() {
        loadUsers();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public User findUserByName(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    /** Registriert einen Nutzer; gibt null zurück, wenn der Name vergeben ist. */
    public User registerUser(String username, String password, boolean stayLoggedIn, Role role) {
        if (findUserByName(username) != null) {
            return null;
        }
        User user = new User(username, password, stayLoggedIn, role);
        users.add(user);
        saveUsers();
        return user;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /** true, wenn ein "richtiger" Nutzer angemeldet ist (kein Gast). */
    public boolean isLoggedInUser() {
        return currentUser != null && currentUser.getRole() != Role.GUEST;
    }

    // --- Persistenz ---------------------------------------------------------

    private void loadUsers() {
        if (!userFile.exists()) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(userFile.toPath()), StandardCharsets.UTF_8);
            for (String object : JsonUtil.splitObjects(json)) {
                String username = JsonUtil.extractString(object, "username");
                String passwordHash = JsonUtil.extractString(object, "passwordHash");
                String roleLabel = JsonUtil.extractString(object, "role");
                boolean stayLoggedIn = JsonUtil.extractBoolean(object, "stayLoggedIn");

                if (username == null || passwordHash == null) {
                    continue;
                }
                users.add(User.fromStoredData(username, passwordHash, stayLoggedIn, Role.fromLabel(roleLabel)));
            }
        } catch (IOException e) {
            System.out.println("users.json konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    private void saveUsers() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            json.append("  {\n");
            json.append("    \"username\": \"").append(JsonUtil.escape(user.getUsername())).append("\",\n");
            // Es wird immer nur der Hash gespeichert, nie das Klartext-Passwort.
            json.append("    \"passwordHash\": \"").append(JsonUtil.escape(user.getPasswordHash())).append("\",\n");
            json.append("    \"stayLoggedIn\": ").append(user.isStayLoggedIn()).append(",\n");
            json.append("    \"role\": \"").append(JsonUtil.escape(user.getRole().getLabel())).append("\"\n");
            json.append("  }");
            if (i < users.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]\n");

        try {
            Files.write(userFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("users.json konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
