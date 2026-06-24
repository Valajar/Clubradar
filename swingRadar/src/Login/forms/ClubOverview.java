package Login.forms;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class ClubOverview extends JPanel {
    private JPanel mainPanel;
    private JLabel Ueberschrift;
    private JLabel Club1;
    private infoWaypoint clubWaypoint;
    private Club clubanzeige1;
    private JSplitPane splitPane;

    public ClubOverview(Club club, JSplitPane splitPane, JXMapViewer mapViewer) {
        this.clubanzeige1 = club;
        this.splitPane = splitPane;
        add(mainPanel, BorderLayout.CENTER);
        Club1.setText("Projekt 42"); //clubinfo.name
        clubWaypoint = new infoWaypoint(new GeoPosition(51.195457, 6.428547), "Projekt 42", new String[]{"Waldhausener Str. 40-42, 41061 Mönchengladbach-Nord", "Freitag,23:00–05:00;Samstag,23:00–05:00;", "http://projekt42.info/"});
        Club1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Club1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clubanzeige1.zeigeWaypoint(clubWaypoint);
                splitPane.setLeftComponent(clubanzeige1);
                splitPane.revalidate();
                splitPane.repaint();
                mapViewer.setAddressLocation(clubWaypoint.getPosition());
                mapViewer.setZoom(2);
            }
        });
    }
}
