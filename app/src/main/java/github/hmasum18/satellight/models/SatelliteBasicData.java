package github.hmasum18.satellight.models;

public class SatelliteBasicData {
    private String id;
    private double lat;
    private double lng;
    private double height;
    private long timestamp;


    public SatelliteBasicData(String id, double lat, double lng, double height, long timestamp) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.height = height;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getHeight() {
        return height;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SatelliteBasicData{" +
                "id='" + id + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", height=" + height +
                ", timestamp=" + timestamp +
                '}';
    }
}
