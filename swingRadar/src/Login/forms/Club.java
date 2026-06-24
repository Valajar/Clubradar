package Login.forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class Club extends JPanel {
    private JPanel mainPanel;
    private JLabel clubName;
    private JTextArea oefnungszeiten;
    private JLabel adresse;
    private JLabel Sperator1;
    private JLabel Website;
    private JTextArea Bewertungen;
    private String[] beschreibung;

    public Club(){
        clubName.setText(" ");
        oefnungszeiten.setText(" ");
        oefnungszeiten.setEditable(false);
        oefnungszeiten.setOpaque(false);
        Bewertungen.setText(" ");
        Bewertungen.setEditable(false);
        Bewertungen.setOpaque(false);
        adresse.setText(" ");
        Sperator1.setText(" ");
        Website.setText(" ");
        System.out.println(clubName.getText());
        add(mainPanel, BorderLayout.CENTER);
    }

    private String oefnungszeitenFormat(String s){
        String[] tage = s.split(";\\s*");
        StringBuilder sb = new StringBuilder();
        for (String tag : tage) {
            sb.append("🕑 ").append(tag.trim()).append("\n");
        }
        return sb.toString().trim();
    }
    private String bewertungenFormat(String s){
        String[] bewertung = s.split("¿\\s*");
        StringBuilder sb = new StringBuilder();
        for (String b : bewertung) {
            String[] einzelteile = b.split("∞\\s*");
            sb.append("👤").append(einzelteile[0].trim()).append("\n").append(einzelteile[1].trim()).append("\n\n");
        }
        return sb.toString().trim();
    }

    public void zeigeWaypoint(infoWaypoint waypoint) {
        if(beschreibung == null) {
            beschreibung = waypoint.getBeschreibung();
        }if(beschreibung == null){
            System.out.println("Beschreibung fehlt");
        }
        Website.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Website.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(beschreibung[2]));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        beschreibung = waypoint.getBeschreibung();
        System.out.println("Anzeigen der Informationen");
        System.out.println(waypoint.getBeschreibung());
        clubName.setText(waypoint.getName());
        adresse.setText("📍"+beschreibung[0]);
        oefnungszeiten.setText(oefnungszeitenFormat(beschreibung[1]).replace(";", "\n"));
        Website.setText("🌐"+beschreibung[2].replace("http://", "").replace("https://", "").replace("/", ""));

        revalidate();
        repaint();
    }
}
