package clubradar.ui;

import clubradar.data.ClubRepository;
import clubradar.data.UserManager;
import clubradar.model.Club;
import clubradar.model.User;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

// Diese Klasse hieß früher "ClubMap" und wurde von Claude Fable 5
// umbenannt und überarbeitet: Kopfzeile mit angemeldetem Nutzer und
// Abmelden-Button, Navigation zwischen Liste und Detailansicht läuft
// jetzt zentral über diese Klasse.

public class MapFrame extends JFrame {

    private final JXMapViewer mapViewer = new JXMapViewer();
    private final JSplitPane splitPane;
    private final ClubListPanel listPanel;
    private final ClubDetailPanel detailPanel;

    public MapFrame() {
        setTitle("Clubradar – Karte");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // --- Karte -----------------------------------------------------------
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(4);
        mapViewer.setTileFactory(tileFactory);
        mapViewer.setZoom(7);
        mapViewer.setAddressLocation(new GeoPosition(51.195457, 6.428547));

        Set<Waypoint> waypoints = new HashSet<>(ClubRepository.getInstance().getClubs());
        WaypointPainter<Waypoint> painter = new WaypointPainter<>();
        painter.setWaypoints(waypoints);
        mapViewer.setOverlayPainter(painter);

        PanMouseInputListener panListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener((MouseListener) panListener);
        mapViewer.addMouseMotionListener((MouseMotionListener) panListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));
        mapViewer.setFocusable(true);

        // Klick auf einen Marker öffnet die Detailansicht des Clubs.
        // (Treffer-Erkennung ursprünglich mit Cascade ai SWE-1.6 erstellt)
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Club club : ClubRepository.getInstance().getClubs()) {
                    Point2D clubPoint = mapViewer.getTileFactory()
                            .geoToPixel(club.getPosition(), mapViewer.getZoom());
                    Point2D centerPoint = mapViewer.getTileFactory()
                            .geoToPixel(mapViewer.getCenterPosition(), mapViewer.getZoom());

                    int dx = (int) (clubPoint.getX() - centerPoint.getX()) + mapViewer.getWidth() / 2;
                    int dy = (int) (clubPoint.getY() - centerPoint.getY()) + mapViewer.getHeight() / 2;

                    if (Math.abs(e.getX() - dx) < 15 && Math.abs(e.getY() - dy) < 15) {
                        showClub(club);
                        break;
                    }
                }
            }
        });

        // --- Linke Seite: Liste / Detail --------------------------------------
        detailPanel = new ClubDetailPanel(this);
        listPanel = new ClubListPanel(this);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, mapViewer);
        splitPane.setDividerLocation(340);
        splitPane.setResizeWeight(0.0);
        add(splitPane, BorderLayout.CENTER);

        // --- Kopfzeile: wer ist angemeldet + Abmelden --------------------------
        User user = UserManager.getInstance().getCurrentUser();
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        String userText = user == null
                ? "Nicht angemeldet"
                : "Angemeldet als: " + user.getUsername() + " (" + user.getRole().getLabel() + ")";
        topBar.add(new JLabel(userText), BorderLayout.WEST);

        JButton logoutButton = new JButton("Abmelden");
        logoutButton.addActionListener(e -> {
            UserManager.getInstance().setCurrentUser(null);
            new LoginFrame().setVisible(true);
            dispose();
        });
        topBar.add(logoutButton, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // --- Fußzeile: Zoom-Buttons -------------------------------------------
        JButton zoomIn = new JButton("+");
        JButton zoomOut = new JButton("-");
        zoomIn.addActionListener(e -> {
            if (mapViewer.getZoom() > 1) mapViewer.setZoom(mapViewer.getZoom() - 1);
        });
        zoomOut.addActionListener(e -> {
            if (mapViewer.getZoom() < 17) mapViewer.setZoom(mapViewer.getZoom() + 1);
        });
        JPanel zoomPanel = new JPanel();
        zoomPanel.add(zoomIn);
        zoomPanel.add(zoomOut);
        add(zoomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    /** Zeigt die Detailansicht eines Clubs links und zentriert die Karte auf ihn. */
    public void showClub(Club club) {
        detailPanel.showClub(club);
        int divider = splitPane.getDividerLocation();
        splitPane.setLeftComponent(detailPanel);
        splitPane.setDividerLocation(divider);
        splitPane.revalidate();
        splitPane.repaint();
        mapViewer.setAddressLocation(club.getPosition());
        if (mapViewer.getZoom() > 4) {
            mapViewer.setZoom(4);
        }
    }

    /** Wechselt zurück zur Clubliste. */
    public void showList() {
        int divider = splitPane.getDividerLocation();
        listPanel.rebuild();
        splitPane.setLeftComponent(listPanel);
        splitPane.setDividerLocation(divider);
        splitPane.revalidate();
        splitPane.repaint();
    }

    /** Baut die Liste neu auf (z.B. nach neuer Bewertung oder Favoriten-Änderung). */
    public void refreshList() {
        listPanel.rebuild();
    }
}
