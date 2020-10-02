package github.hmasum18.satellight.views.vr;

import androidx.core.util.Pair;

public class Satellite {
    private float horizontalAngle;
    private float verticalAngle;

    private static final float HFieldOfView = 100;
    private static final float VFieldOfView = 55;


    public Satellite(float horizontalAngle, float verticalAngle) {
        this.horizontalAngle = horizontalAngle;
        this.verticalAngle = verticalAngle;
    }

    public Pair<Float, Float> getPosition(float deviceHAngle, float deviceVAngle) {
        if (2*Math.abs(deviceHAngle-horizontalAngle) > HFieldOfView ||
                2*Math.abs(deviceVAngle-verticalAngle) > VFieldOfView ) return null;

        return new Pair<>((horizontalAngle-deviceHAngle)/HFieldOfView, (verticalAngle-deviceVAngle)/VFieldOfView);
    }
}
