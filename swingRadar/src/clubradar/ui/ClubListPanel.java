package clubradar.ui;

import clubradar.data.ClubRepository;
import clubradar.data.FavoritesStore;
import clubradar.data.UserManager;
import clubradar.model.Club;
import clubradar.model.User;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Diese Klasse hieß früher "ClubOverview" und wurde von Claude Fable 5
// neu geschrieben: Suchfeld, Sortierung (Name / beste Bewertung /
// Favoriten zuerst) und Anzeige der Durchschnittsbewertung pro Club.

public class ClubListPanel extends JPanel {

    private static final String SORT_BY_NAME = "Name";
    private static final String SORT_BY_RATING = "Beste Bewertung";
    private static final String SORT_FAVORITES_FIRST = "Favoriten zuerst";

    private final MapFrame mapFrame;
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> sortComboBox;
    private final JPanel clubListPanel = new JPanel();

    public ClubListPanel(MapFrame mapFrame) {
        this.mapFrame = mapFrame;

        setLayout(new BorderLayout());

        // --- Suchfeld + Sortierung oben ---------------------------------------
        JPanel controls = new JPanel(new GridLayout(0, 1, 4, 4));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel searchRow = new JPanel(new BorderLayout(4, 0));
        searchRow.add(new JLabel("🔍"), BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        controls.add(searchRow);

        List<String> sortOptions = new ArrayList<>();
        sortOptions.add(SORT_BY_NAME);
        sortOptions.add(SORT_BY_RATING);
        if (UserManager.getInstance().isLoggedInUser()) {
            sortOptions.add(SORT_FAVORITES_FIRST);
        }
        sortComboBox = new JComboBox<>(sortOptions.toArray(new String[0]));

        JPanel sortRow = new JPanel(new BorderLayout(4, 0));
        sortRow.add(new JLabel("Sortieren:"), BorderLayout.WEST);
        sortRow.add(sortComboBox, BorderLayout.CENTER);
        controls.add(sortRow);

        add(controls, BorderLayout.NORTH);

        // --- Liste -------------------------------------------------------------
        clubListPanel.setLayout(new BoxLayout(clubListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(clubListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Bei jeder Eingabe im Suchfeld und jedem Sortierwechsel neu aufbauen.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { rebuild(); }
            public void removeUpdate(DocumentEvent e) { rebuild(); }
            public void changedUpdate(DocumentEvent e) { rebuild(); }
        });
        sortComboBox.addActionListener(e -> rebuild());

        rebuild();
    }

    /** Baut die Liste anhand von Suchbegriff und Sortierung neu auf. */
    public void rebuild() {
        clubListPanel.removeAll();

        String search = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String sortMode = (String) sortComboBox.getSelectedItem();
        User user = UserManager.getInstance().getCurrentUser();

        List<Club> clubs = new ArrayList<>(ClubRepository.getInstance().getClubs());

        if (!search.isEmpty()) {
            clubs.removeIf(club -> !club.getName().toLowerCase(Locale.ROOT).contains(search));
        }

        if (SORT_BY_RATING.equals(sortMode)) {
            clubs.sort(Comparator.comparingDouble(Club::getAverageStars).reversed()
                    .thenComparing(Club::getName, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_FAVORITES_FIRST.equals(sortMode) && user != null) {
            clubs.sort(Comparator
                    .comparing((Club club) -> !FavoritesStore.getInstance()
                            .isFavorite(user.getUsername(), club.getName()))
                    .thenComparing(Club::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            clubs.sort(Comparator.comparing(Club::getName, String.CASE_INSENSITIVE_ORDER));
        }

        for (Club club : clubs) {
            clubListPanel.add(createClubRow(club, user));
        }
        if (clubs.isEmpty()) {
            JLabel empty = new JLabel("Keine Clubs gefunden");
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            clubListPanel.add(empty);
        }

        clubListPanel.revalidate();
        clubListPanel.repaint();
    }

    /** Eine Zeile der Liste: Favoriten-Herz, Sterne-Schnitt und Clubname. */
    private JComponent createClubRow(Club club, User user) {
        StringBuilder text = new StringBuilder("<html>");
        if (user != null && FavoritesStore.getInstance().isFavorite(user.getUsername(), club.getName())) {
            text.append("❤ ");
        }
        double average = club.getAverageStars();
        if (average > 0) {
            text.append("★ ").append(String.format(Locale.GERMANY, "%.1f", average))
                .append(" (").append(club.getRatingCount()).append(")  ");
        }
        text.append(club.getName()).append("</html>");

        JLabel label = new JLabel(text.toString());
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mapFrame.showClub(club);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(Color.BLUE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Color.BLACK);
            }
        });
        return label;
    }
}
