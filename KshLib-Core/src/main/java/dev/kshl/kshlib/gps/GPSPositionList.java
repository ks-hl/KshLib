package dev.kshl.kshlib.gps;

import java.util.ArrayList;
import java.util.List;

public class GPSPositionList extends ArrayList<GPSTimeAndCoordinate> {
    public GPSPositionList() {
    }

    public GPSPositionList(List<GPSTimeAndCoordinate> list) {
        addAll(list);
    }
}
