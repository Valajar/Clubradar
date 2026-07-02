package clubradar;

import clubradar.data.ClubRepository;
import clubradar.ui.LoginFrame;

import javax.swing.*;

// Von Claude Fable 5 überarbeitet: neues Package "clubradar",
// Einstiegspunkt ist jetzt clubradar.Main (statt Login.forms.Main).

public class Main {

    /**
     * Lädt die Clubdaten in einem Hintergrund-Thread, damit das
     * Login-Fenster beim Start nicht durch die Netzwerkabfrage
     * (OSM/Overpass) einfriert. LoginFrame wartet mit join() auf diesen
     * Thread, bevor die Karte geöffnet wird.
     */
    public static final Thread clubLoader = new Thread(
            () -> ClubRepository.getInstance().loadClubs(51.0, 6.3, 51.4, 6.6));

    public static void main(String[] args) {
        clubLoader.start();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
