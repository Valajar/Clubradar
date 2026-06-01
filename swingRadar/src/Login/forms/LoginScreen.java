package Login.forms;

import javax.swing.*;

public class LoginScreen extends JFrame {
    private JPanel panel1;
    private JTextField textField1;
    private JPasswordField passwordField1;
    private JCheckBox angemeldetBleibenCheckBox;
    private JButton anmeldenButton;
    private JButton regestrierenButton;
    private JLabel incorrectPassword;
    private JLabel Userdoesnotexist;
    private boolean angemeldetBleiben = false;
    private String username;
    private String password;

    public LoginScreen() {
        setTitle("Anmelden");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(panel1);
        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);

        anmeldenButton.addActionListener(e -> {
            username = textField1.getText();
            password = new String(passwordField1.getPassword());
            angemeldetBleiben = angemeldetBleibenCheckBox.isSelected();
            Layer8 user = Database.getDatabase().sucheNachName(username);
            Userdoesnotexist.setText("");
            incorrectPassword.setText("");
            if(user == null){
                Userdoesnotexist.setText("Benutzer existiert nicht");
                return;
            }
            if(!user.checkPassword(password) || password.isEmpty()){
                incorrectPassword.setText("Falsches Passwort");
                Userdoesnotexist.setText("Benutzername existiert nicht");
                return;
            }
            System.out.println("Passwort: " + user.checkPassword(password));
            System.out.println("Benutzername: " + username);
            System.out.println("Passwort: " + password);
            System.out.println("Angemeldet bleiben: " + angemeldetBleiben);
        });
        regestrierenButton.addActionListener(e -> {
            username = textField1.getText();
            password = new String(passwordField1.getPassword());
            angemeldetBleiben = angemeldetBleibenCheckBox.isSelected();
            incorrectPassword.setText("");
            if (password.isEmpty()){
                incorrectPassword.setText("Gib ein Passwort an!");
                return;
            }
            System.out.println("Benutzername: " + username);
            System.out.println("Passwort: " + password);
            System.out.println("Angemeldet bleiben: " + angemeldetBleiben);
            Layer8 user = Database.getDatabase().sucheNachName(username);
            if(user == null){
                user = new Layer8(username, password, angemeldetBleiben, "Betreiber");
                Database.getDatabase().hinzufuegen(user);
            }
            System.out.println("Passwort: " + user.checkPassword(password));
        });
    }
}
