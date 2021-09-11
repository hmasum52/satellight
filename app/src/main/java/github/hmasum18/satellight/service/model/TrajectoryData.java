package github.hmasum18.satellight.service.model;

public class TrajectoryData {
    private String shortName;
    private double lat;
    private double lng;
    private double elevation;
    private double azimuth;
    private double range;
    private double height;
    private double velocity;
    private long timestamp;

    //for SSC web service
    public TrajectoryData(String shortName, double lat, double lng, double height, long timestamp) {
        this.shortName = shortName;
        this.lat = lat;
        this.lng = lng;
        this.height = height;
        this.timestamp = timestamp;
    }

    public TrajectoryData(String shortName, double lat, double lng, double elevation, double azimuth
            , double range, double height, double velocity, long timestamp) {
        this.shortName = shortName;
        this.lat = lat;
        this.lng = lng;
        this.elevation = elevation;
        this.azimuth = azimuth;
        this.range = range;
        this.height = height;
        this.velocity = velocity;
        this.timestamp = timestamp;
    }

    public String getShortName() {
        return shortName;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getElevation() {
        return elevation;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getRange() {
        return range;
    }

    public double getHeight() {
        return height;
    }

    public double getVelocity() {
        return velocity;
    }

    public long getTimestamp() {
        return timestamp;
    }
}