package clubradar.ui;

import clubradar.Main;
import clubradar.data.UserManager;
import clubradar.model.Role;
import clubradar.model.User;

import javax.swing.*;
import java.awt.*;

// Diese Klasse wurde von Claude Fable 5 erstellt. Sie ersetzt die beiden
// alten Fenster LoginOptionScreen + LoginScreen (IntelliJ-.form-basiert)
// durch EIN handgeschriebenes Fenster:
// - Anmelden braucht keine Rollenauswahl mehr (die Rolle steht in users.json),
// - die Rollen-Combobox gilt nur für die Registrierung,
// - "Als Gast fortfahren" öffnet die Karte ohne Konto (nur lesen).

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JCheckBox stayLoggedInCheckBox = new JCheckBox("Angemeldet bleiben");
    private final JComboBox<Role> roleComboBox = new JComboBox<>(
            new Role[]{Role.USER, Role.OWNER, Role.ADMIN});
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame() {
        setTitle("Clubradar – Anmelden");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Clubradar");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        content.add(title, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 4, 16, 4);
        content.add(new JLabel("Finde Clubs, Bars und Kneipen in deiner Nähe."), gbc);
        gbc.insets = new Insets(4, 4, 4, 4);

        gbc.gridy++;
        content.add(new JLabel("Benutzername"), gbc);
        gbc.gridy++;
        content.add(usernameField, gbc);

        gbc.gridy++;
        content.add(new JLabel("Passwort"), gbc);
        gbc.gridy++;
        content.add(passwordField, gbc);

        gbc.gridy++;
        content.add(stayLoggedInCheckBox, gbc);

        gbc.gridy++;
        errorLabel.setForeground(new Color(180, 30, 30));
        content.add(errorLabel, gbc);

        gbc.gridy++;
        JButton loginButton = new JButton("Anmelden");
        content.add(loginButton, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 4, 4, 4);
        content.add(new JSeparator(), gbc);
        gbc.insets = new Insets(4, 4, 4, 4);

        gbc.gridy++;
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rolePanel.add(new JLabel("Neues Konto als:  "));
        roleComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Role) {
                    setText(((Role) value).getLabel());
                }
                return this;
            }
        });
        rolePanel.add(roleComboBox);
        content.add(rolePanel, gbc);

        gbc.gridy++;
        JButton registerButton = new JButton("Registrieren");
        content.add(registerButton, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 4, 4, 4);
        JButton guestButton = new JButton("Als Gast fortfahren (nur ansehen)");
        content.add(guestButton, gbc);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());
        guestButton.addActionListener(e -> continueAsGuest());
        getRootPane().setDefaultButton(loginButton); // Enter-Taste = Anmelden
    }

    private void login() {
        errorLabel.setText(" ");
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        User user = UserManager.getInstance().findUserByName(username);
        if (user == null) {
            errorLabel.setText("Benutzer existiert nicht");
            return;
        }
        if (password.isEmpty() || !user.checkPassword(password)) {
            errorLabel.setText("Falsches Passwort");
            return;
        }

        UserManager.getInstance().setCurrentUser(user);
        openMap();
    }

    private void register() {
        errorLabel.setText(" ");
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Role role = (Role) roleComboBox.getSelectedItem();

        if (username.isEmpty()) {
            errorLabel.setText("Gib einen Benutzernamen an!");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Gib ein Passwort an!");
            return;
        }

        User user = UserManager.getInstance().registerUser(
                username, password, stayLoggedInCheckBox.isSelected(), role);
        if (user == null) {
            errorLabel.setText("Benutzer existiert bereits");
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Konto \"" + username + "\" (" + role.getLabel() + ") wurde erstellt.\n"
                        + "Du wirst jetzt angemeldet.",
                "Registrierung erfolgreich", JOptionPane.INFORMATION_MESSAGE);

        UserManager.getInstance().setCurrentUser(user);
        openMap();
    }

    private void continueAsGuest() {
        UserManager.getInstance().setCurrentUser(User.guest());
        openMap();
    }

    /** Wartet ggf. auf die im Hintergrund ladenden Clubdaten und öffnet die Karte. */
    private void openMap() {
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Main.clubLoader.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        new MapFrame().setVisible(true);
        dispose();
    }
}
