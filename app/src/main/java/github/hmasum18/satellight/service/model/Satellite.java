package github.hmasum18.satellight.service.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.neosensory.tlepredictionengine.Tle;

import java.lang.reflect.Type;
import java.util.List;

@Entity(tableName = "satellite_data")
public class Satellite {

    @PrimaryKey
    @SerializedName("id")
    @Expose
    private int id;
    @SerializedName("color")
    @Expose
    private String color;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("launch_date")
    @Expose
    private String launchDate;
    @SerializedName("mission_duration")
    @Expose
    private String missionDuration;
    @SerializedName("launch_mass")
    @Expose
    private String launchMass;
    @SerializedName("isGeoStationary")
    @Expose
    private boolean isGeoStationary;
    @SerializedName("tle_line1")
    @Expose
    private String tleLine1;
    @SerializedName("tle_line2")
    @Expose
    private String tleLine2;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("sat_name")
    @Expose
    private String satName;
    @SerializedName("country_name")
    @Expose
    private String countryName;
    @SerializedName("country_flag")
    @Expose
    private String countryFlag;
    @SerializedName("icon_url")
    @Expose
    private String iconUrl;

    @Ignore // ignore by room
    @SerializedName("real_images")
    @Expose
    private List<String> realImages;

    @ColumnInfo(name = "realImages")
    private String realImageString;

    @Ignore // ignore by room
    @SerializedName("use_cases")
    @Expose
    private List<String> useCases;

    @ColumnInfo(name = "useCases")
    private String useCasesString;

    @SerializedName("description")
    @Expose
    private String description;

    /**
     * No args constructor for use in serialization
     *
     */
    public Satellite() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public boolean isIsGeoStationary() {
        return isGeoStationary;
    }

    public void setIsGeoStationary(boolean isGeoStationary) {
        this.isGeoStationary = isGeoStationary;
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

    public Tle extractTle(){
        return new Tle(tleLine1, tleLine2);
    }

    public void setTleLine2(String tleLine2) {
        this.tleLine2 = tleLine2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSatName() {
        return satName;
    }

    public void setSatName(String satName) {
        this.satName = satName;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryFlag() {
        return countryFlag;
    }

    public void setCountryFlag(String countryFlag) {
        this.countryFlag = countryFlag;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public List<String> getRealImages() {
        return realImages;
    }

    public void setRealImages(List<String> realImages) {
        this.realImages = realImages;
    }

    public void setRealImageString(String realImageString) {
        Type type = new TypeToken<List<String>>(){}.getType();
        realImages = new Gson().fromJson(realImageString, type);
        this.realImageString = realImageString;
    }

    public String getRealImageString() {
        return new Gson().toJson(realImages);
    }

    public List<String> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<String> useCases) {
        this.useCases = useCases;
    }

    public void setUseCasesString(String useCasesString) {
        Type type = new TypeToken<List<String>>(){}.getType();
        this.useCases = new Gson().fromJson(useCasesString, type);
        this.useCasesString = useCasesString;
    }

    public String getUseCasesString() {
        return new Gson().toJson(useCases);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}