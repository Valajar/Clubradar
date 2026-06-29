package Login.forms;

import javax.swing.*;
import java.util.ArrayList;

public class Main {
    public static ArrayList<Layer8> users = new ArrayList<>();
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //LoginOptionScreen loginOptionScreen = new LoginOptionScreen();
            //loginOptionScreen.setVisible(true);
            ClubDatabase clubdatabase = new ClubDatabase();
            ClubMap mapframe = new ClubMap();
        });
    }
}