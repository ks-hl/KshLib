package dev.kshl.kshlib.gps;

public enum Direction {
    N, S, E, W;

    public CoordinateType getCoordinateType() {
        return switch (this) {
            case N, S -> CoordinateType.LATITUDE;
            case E, W -> CoordinateType.LONGITUDE;
        };
    }
}
