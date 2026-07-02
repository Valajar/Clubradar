package clubradar.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Diese Klasse hieß früher "Hash" (erstellt von claude.ai Sonnet 4.6)
// und wurde von Claude Fable 5 umbenannt (englische Namen, UTF-8 explizit).

public class PasswordHasher {

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Fehler beim Erstellen des Hashes", e);
        }
    }
}
