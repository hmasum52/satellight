package github.hmasum18.satellight.service.model;

import java.util.ArrayList;

public class SatelliteData {
    private  String color;
    private String type;
    private String launchDate;
    private String missionDuration;
    private String launchMass;
    private boolean isGeoStationary;
    private String tleLine1;
    private String tleLine2;
    private String fullName;
    private String shortName;
    private String countryName;
    private String countryFlagLink;
    private String iconUrl;
    private ArrayList<String> realImages;
    private ArrayList<String> useCases;
    private String description;
    private ArrayList<TrajectoryData> trajectoryDataList;

    public SatelliteData() {

    }

    public SatelliteData(String color, String type, String launchDate, String missionDuration, String launchMass,
                         boolean isGeoStationary, String tleLine1, String tleLine2, String fullName, String shortName,
                         String countryName, String countryFlagLink, String iconUrl, ArrayList<String> realImages,
                         ArrayList<String> useCases, String description, ArrayList<TrajectoryData> trajectoryDataList) {
        this.color = color;
        this.type = type;
        this.launchDate = launchDate;
        this.missionDuration = missionDuration;
        this.launchMass = launchMass;
        this.isGeoStationary = isGeoStationary;
        this.tleLine1 = tleLine1;
        this.tleLine2 = tleLine2;
        this.fullName = fullName;
        this.shortName = shortName;
        this.countryName = countryName;
        this.countryFlagLink = countryFlagLink;
        this.iconUrl = iconUrl;
        this.realImages = realImages;
        this.useCases = useCases;
        this.description = description;
        this.trajectoryDataList = trajectoryDataList;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(String launchDate) {
        this.launchDate = launchDate;
    }

    public String getMissionDuration() {
        return missionDuration;
    }

    public void setMissionDuration(String missionDuration) {
        this.missionDuration = missionDuration;
    }

    public String getLaunchMass() {
        return launchMass;
    }

    public void setLaunchMass(String launchMass) {
        this.launchMass = launchMass;
    }

    public boolean isGeoStationary() {
        return isGeoStationary;
    }

    public void setGeoStationary(boolean geoStationary) {
        isGeoStationary = geoStationary;
    }

    public String getTleLine1() {
        return tleLine1;
    }

    public void setTleLine1(String tleLine1) {
        this.tleLine1 = tleLine1;
    }

    public String getTleLine2() {
        return tleLine2;
    }

    public void setTleLine2(String tleLine2) {
        this.tleLine2 = tleLine2;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryFlagLink() {
        return countryFlagLink;
    }

    public void setCountryFlagLink(String countryFlagLink) {
        this.countryFlagLink = countryFlagLink;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public ArrayList<String> getRealImages() {
        return realImages;
    }

    public void setRealImages(ArrayList<String> realImages) {
        this.realImages = realImages;
    }

    public ArrayList<String> getUseCases() {
        return useCases;
    }

    public void setUseCases(ArrayList<String> useCases) {
        this.useCases = useCases;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<TrajectoryData> getTrajectoryDataList() {
        return trajectoryDataList;
    }

    public void setTrajectoryDataList(ArrayList<TrajectoryData> trajectoryDataList) {
        this.trajectoryDataList = trajectoryDataList;
    }
}
