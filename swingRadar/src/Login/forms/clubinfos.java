package Login.forms;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

public class clubinfos {
    public String[] beschreibung;
    public infoWaypoint clubinfo;

    clubinfos(String n, double x, double y,String[] beschreibung){
        this.beschreibung = beschreibung;
        clubinfo = new infoWaypoint(new GeoPosition(x, y), n, beschreibung);
    }
    public infoWaypoint getClubinfo() {
        return clubinfo;
    }
}
