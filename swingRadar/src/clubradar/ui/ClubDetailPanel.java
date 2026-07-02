package clubradar.ui;

import clubradar.data.ClubEditsStore;
import clubradar.data.FavoritesStore;
import clubradar.data.RatingStore;
import clubradar.data.UserManager;
import clubradar.model.Club;
import clubradar.model.Rating;
import clubradar.model.Role;
import clubradar.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;
import java.util.Locale;

// Diese Klasse hieß früher "Club" (JPanel) und wurde von Claude Fable 5
// neu geschrieben: handgeschriebenes Layout statt IntelliJ-.form und
// rollenspezifische Aktionen:
// - Gast:      nur ansehen,
// - Benutzer:  bewerten + Favoriten,
// - Betreiber: Club beanspruchen und Infos pflegen (kann eigenen Club
//              nicht selbst bewerten),
// - Admin:     Bewertungen löschen, Beanspruchungen aufheben.

public class ClubDetailPanel extends JPanel {

    private final MapFrame mapFrame;

    private final JLabel nameLabel = new JLabel();
    private final JLabel ownerBadgeLabel = new JLabel();
    private final JLabel averageLabel = new JLabel();
    private final JLabel addressLabel = new JLabel();
    private final JTextArea openingHoursArea = new JTextArea();
    private final JLabel websiteLabel = new JLabel();
    private final JTextArea ratingsArea = new JTextArea();
    private final JPanel actionPanel = new JPanel();

    private Club currentClub;

    public ClubDetailPanel(MapFrame mapFrame) {
        this.mapFrame = mapFrame;

        setLayout(new BorderLayout());

        // --- Zurück-Button oben -------------------------------------------------
        JButton backButton = new JButton("← Zurück zur Liste");
        backButton.addActionListener(e -> mapFrame.showList());
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(backButton);
        add(topBar, BorderLayout.NORTH);

        // --- Club-Infos in der Mitte ---------------------------------------------
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 20f));
        infoPanel.add(nameLabel);

        ownerBadgeLabel.setForeground(new Color(0, 120, 0));
        infoPanel.add(ownerBadgeLabel);
        infoPanel.add(averageLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(addressLabel);
        infoPanel.add(Box.createVerticalStrut(8));

        openingHoursArea.setEditable(false);
        openingHoursArea.setOpaque(false);
        infoPanel.add(openingHoursArea);
        infoPanel.add(Box.createVerticalStrut(8));

        websiteLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openWebsite();
            }
        });
        infoPanel.add(websiteLabel);
        infoPanel.add(Box.createVerticalStrut(12));

        JLabel ratingsTitle = new JLabel("Bewertungen");
        ratingsTitle.setFont(ratingsTitle.getFont().deriveFont(Font.BOLD));
        infoPanel.add(ratingsTitle);

        ratingsArea.setEditable(false);
        ratingsArea.setOpaque(false);
        ratingsArea.setLineWrap(true);
        ratingsArea.setWrapStyleWord(true);
        infoPanel.add(ratingsArea);

        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // --- Aktionen unten (je nach Rolle) ---------------------------------------
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(actionPanel, BorderLayout.SOUTH);
    }

    /** Füllt das Panel mit den Daten eines Clubs. */
    public void showClub(Club club) {
        this.currentClub = club;
        refresh();
    }

    /** Baut Anzeige und Aktions-Buttons anhand des aktuellen Clubs neu auf. */
    private void refresh() {
        if (currentClub == null) {
            return;
        }

        nameLabel.setText(currentClub.getName());

        if (currentClub.getOwnerUsername() != null) {
            ownerBadgeLabel.setText("✔ Verwaltet von Betreiber: " + currentClub.getOwnerUsername());
        } else {
            ownerBadgeLabel.setText(" ");
        }

        double average = currentClub.getAverageStars();
        if (average > 0) {
            averageLabel.setText("★ " + String.format(Locale.GERMANY, "%.1f", average)
                    + " von 5 (" + currentClub.getRatingCount() + " Bewertungen)");
        } else {
            averageLabel.setText("Noch keine Sternebewertungen");
        }

        addressLabel.setText("📍 " + currentClub.getAddress());
        openingHoursArea.setText(formatOpeningHours(currentClub.getOpeningHours()));

        if (currentClub.hasWebsite()) {
            websiteLabel.setText("🌐 " + currentClub.getWebsite()
                    .replace("http://", "").replace("https://", "").replace("/", ""));
            websiteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            websiteLabel.setText(" ");
            websiteLabel.setCursor(Cursor.getDefaultCursor());
        }

        ratingsArea.setText(formatRatings(currentClub.getRatings()));

        rebuildActions();
        revalidate();
        repaint();
    }

    // --- Aktions-Buttons je nach Rolle ------------------------------------------

    private void rebuildActions() {
        actionPanel.removeAll();

        User user = UserManager.getInstance().getCurrentUser();
        Role role = user == null ? Role.GUEST : user.getRole();
        boolean ownsThisClub = user != null
                && user.getUsername().equals(currentClub.getOwnerUsername());

        // Bewerten: alle angemeldeten Nutzer — außer der Betreiber beim eigenen Club.
        if (role != Role.GUEST) {
            JButton rateButton = new JButton("Bewertung abgeben");
            if (ownsThisClub) {
                rateButton.setEnabled(false);
                rateButton.setToolTipText("Den eigenen Club kannst du nicht bewerten.");
            } else {
                rateButton.addActionListener(e -> showRatingDialog(user));
            }
            actionPanel.add(rateButton);
        } else {
            JButton rateButton = new JButton("Bewertung abgeben");
            rateButton.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "Als Gast kannst du nur stöbern.\n"
                            + "Melde dich an oder registriere dich, um zu bewerten.",
                    "Nur für angemeldete Nutzer", JOptionPane.INFORMATION_MESSAGE));
            actionPanel.add(rateButton);
        }

        // Favoriten: alle angemeldeten Nutzer.
        if (role != Role.GUEST) {
            boolean isFavorite = FavoritesStore.getInstance()
                    .isFavorite(user.getUsername(), currentClub.getName());
            JButton favoriteButton = new JButton(isFavorite ? "❤ Favorit entfernen" : "♡ Als Favorit merken");
            favoriteButton.addActionListener(e -> {
                FavoritesStore.getInstance().toggleFavorite(user.getUsername(), currentClub.getName());
                mapFrame.refreshList();
                refresh();
            });
            actionPanel.add(favoriteButton);
        }

        // Betreiber: Club beanspruchen bzw. Infos bearbeiten.
        if (role == Role.OWNER) {
            if (currentClub.getOwnerUsername() == null) {
                JButton claimButton = new JButton("Diesen Club verwalten");
                claimButton.addActionListener(e -> claimClub(user));
                actionPanel.add(claimButton);
            } else if (ownsThisClub) {
                JButton editButton = new JButton("Club-Infos bearbeiten");
                editButton.addActionListener(e -> showEditDialog());
                actionPanel.add(editButton);
            }
        }

        // Admin: Bewertungen löschen und Beanspruchungen aufheben.
        if (role == Role.ADMIN) {
            if (!currentClub.getRatings().isEmpty()) {
                JButton deleteRatingButton = new JButton("Bewertung löschen…");
                deleteRatingButton.addActionListener(e -> showDeleteRatingDialog());
                actionPanel.add(deleteRatingButton);
            }
            if (currentClub.getOwnerUsername() != null) {
                JButton removeClaimButton = new JButton("Beanspruchung aufheben");
                removeClaimButton.addActionListener(e -> removeClaim());
                actionPanel.add(removeClaimButton);
            }
        }

        actionPanel.revalidate();
        actionPanel.repaint();
    }

    // --- Benutzer: Bewertung abgeben ------------------------------------------

    private void showRatingDialog(User user) {
        JComboBox<Integer> starsBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        starsBox.setSelectedItem(5);
        JTextArea textArea = new JTextArea(4, 20);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        JPanel starsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        starsRow.add(new JLabel("Sterne (1-5):"));
        starsRow.add(starsBox);
        inputPanel.add(starsRow);
        inputPanel.add(new JLabel("Bewertung (optional):"));
        inputPanel.add(new JScrollPane(textArea));

        int result = JOptionPane.showConfirmDialog(
                this, inputPanel, "Bewertung für " + currentClub.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        int stars = (Integer) starsBox.getSelectedItem();
        String text = textArea.getText().trim();

        // Maximal eine Bewertung pro Nutzer: vorhandene wird ersetzt.
        List<Rating> ratings = currentClub.getRatings();
        Rating.addOrReplace(ratings, new Rating(user.getUsername(), stars, text));
        saveRatings(ratings);
    }

    // --- Admin: Bewertung löschen ----------------------------------------------

    private void showDeleteRatingDialog() {
        List<Rating> ratings = currentClub.getRatings();
        String[] authors = ratings.stream().map(Rating::getAuthor).toArray(String[]::new);

        String chosen = (String) JOptionPane.showInputDialog(this,
                "Welche Bewertung soll gelöscht werden?", "Bewertung löschen",
                JOptionPane.QUESTION_MESSAGE, null, authors, authors[0]);
        if (chosen == null) {
            return;
        }

        ratings.removeIf(r -> r.getAuthor().equals(chosen));
        saveRatings(ratings);
    }

    private void saveRatings(List<Rating> ratings) {
        String raw = Rating.listToStorageString(ratings);
        currentClub.setRatingsRaw(raw);
        RatingStore.getInstance().setRatings(currentClub.getName(), raw);
        mapFrame.refreshList();
        refresh();
    }

    // --- Betreiber: beanspruchen und bearbeiten -----------------------------------

    private void claimClub(User user) {
        int result = JOptionPane.showConfirmDialog(this,
                "Möchtest du \"" + currentClub.getName() + "\" als Betreiber verwalten?\n"
                        + "Du kannst danach Adresse, Öffnungszeiten und Website pflegen.",
                "Club verwalten", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        currentClub.setOwnerUsername(user.getUsername());
        ClubEditsStore.getInstance().claimClub(currentClub.getName(), user.getUsername());
        refresh();
    }

    private void showEditDialog() {
        JTextField addressField = new JTextField(currentClub.getAddress(), 25);
        JTextArea hoursArea = new JTextArea(currentClub.getOpeningHours(), 3, 25);
        hoursArea.setLineWrap(true);
        JTextField websiteField = new JTextField(currentClub.getWebsite(), 25);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(new JLabel("Adresse:"));
        inputPanel.add(addressField);
        inputPanel.add(new JLabel("Öffnungszeiten (mit \";\" trennen):"));
        inputPanel.add(new JScrollPane(hoursArea));
        inputPanel.add(new JLabel("Website:"));
        inputPanel.add(websiteField);

        int result = JOptionPane.showConfirmDialog(
                this, inputPanel, "Infos bearbeiten: " + currentClub.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        currentClub.setAddress(addressField.getText().trim());
        currentClub.setOpeningHours(hoursArea.getText().trim());
        currentClub.setWebsite(websiteField.getText().trim());
        ClubEditsStore.getInstance().saveDetails(currentClub.getName(),
                currentClub.getAddress(), currentClub.getOpeningHours(), currentClub.getWebsite());
        refresh();
    }

    private void removeClaim() {
        int result = JOptionPane.showConfirmDialog(this,
                "Beanspruchung von \"" + currentClub.getOwnerUsername() + "\" wirklich aufheben?\n"
                        + "Die vom Betreiber gepflegten Infos gelten dann nicht mehr.",
                "Beanspruchung aufheben", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        currentClub.setOwnerUsername(null);
        ClubEditsStore.getInstance().removeClaim(currentClub.getName());
        refresh();
    }

    // --- Anzeige-Helfer -------------------------------------------------------

    private void openWebsite() {
        if (currentClub == null || !currentClub.hasWebsite()) {
            return;
        }
        String url = currentClub.getWebsite();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Die Website konnte nicht geöffnet werden:\n" + url,
                    "Fehler", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String formatOpeningHours(String hours) {
        if (hours == null || hours.isBlank()) {
            return "Öffnungszeiten unbekannt";
        }
        StringBuilder sb = new StringBuilder();
        for (String day : hours.split(";\\s*")) {
            if (!day.isBlank()) {
                sb.append("🕑 ").append(day.trim()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String formatRatings(List<Rating> ratings) {
        if (ratings.isEmpty()) {
            return "Noch keine Bewertungen vorhanden";
        }
        StringBuilder sb = new StringBuilder();
        for (Rating rating : ratings) {
            sb.append("👤 ").append(rating.getAuthor()).append("\n");
            if (rating.getStars() > 0) {
                sb.append(starsToString(rating.getStars())).append("\n");
            }
            if (!rating.getText().isEmpty()) {
                sb.append(rating.getText()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String starsToString(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= stars ? "★" : "☆");
        }
        return sb.toString();
    }
}
