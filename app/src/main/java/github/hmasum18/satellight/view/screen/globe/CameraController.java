package github.hmasum18.satellight.view.screen.globe;

import android.util.Log;

import gov.nasa.worldwind.BasicWorldWindowController;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Camera;
import gov.nasa.worldwind.geom.Location;
import gov.nasa.worldwind.gesture.GestureRecognizer;
import gov.nasa.worldwind.gesture.PinchRecognizer;
import gov.nasa.worldwind.gesture.RotationRecognizer;
import gov.nasa.worldwind.util.WWMath;

/**
 * A custom WorldWindController that uses gestures to control the camera directly via the setAsCamera interface
 * instead of the default setAsLookAt interface.
 */
public class CameraController extends BasicWorldWindowController {
    private static final String TAG = "CameraController";

    protected Camera camera = new Camera();

    protected Camera beginCamera = new Camera();

    @Override
    protected void handlePinch(GestureRecognizer recognizer) {
        Log.d(TAG, "handlePinch: ");
        int state = recognizer.getState();
        float scale = ((PinchRecognizer) recognizer).getScale();
        Log.d(TAG, "handlePinch: scale: "+scale);

        if (state == WorldWind.BEGAN) {
            this.gestureDidBegin();
        } else if (state == WorldWind.CHANGED) {
            if (scale != 0) {
                // Apply the change in scale to the navigator, relative to when the gesture began.
                scale = ((scale - 1) * 0.08f) + 1; // dampen the scale factor
                double alt = this.camera.altitude/1000.0;
                Log.d(TAG, "handlePinch: alt before: "+alt);
                this.camera.altitude = this.camera.altitude + this.camera.altitude * (1-scale);
                alt = this.camera.altitude/1000.0;
                Log.d(TAG, "handlePinch: alt after: "+alt);

               // this.applyLimits(this.camera);

                getWorldWindow().getNavigator().setAltitude(alt);
                this.wwd.getNavigator().setAsCamera(this.wwd.getGlobe(), this.camera);

                this.wwd.requestRedraw();
            }
        } else if (state == WorldWind.ENDED || state == WorldWind.CANCELLED) {
            this.gestureDidEnd();
        }
    }

    @Override
    protected void gestureDidBegin() {
        Log.d(TAG, "gestureDidBegin: ");
        if (this.activeGestures++ == 0) {
            this.wwd.getNavigator().getAsCamera(this.wwd.getGlobe(), this.beginCamera);
            this.camera.set(this.beginCamera);
        }
    }

    protected void applyLimits(Camera camera) {
        Log.d(TAG, "applyLimits: ");
        double distanceToExtents = this.wwd.distanceToViewGlobeExtents();

        double minAltitude = 100;
        double maxAltitude = distanceToExtents;
        camera.altitude = WWMath.clamp(camera.altitude, minAltitude, maxAltitude);

        // Limit the tilt to between nadir and the horizon (roughly)
        double r = wwd.getGlobe().getRadiusAt(camera.latitude, camera.latitude);
        double maxTilt = Math.toDegrees(Math.asin(r / (r + camera.altitude)));
        double minTilt = 0;
        camera.tilt = WWMath.clamp(camera.tilt, minTilt, maxTilt);
    }
    @Override
    protected void handlePan(GestureRecognizer recognizer) {
        int state = recognizer.getState();
        float dx = recognizer.getTranslationX();
        float dy = recognizer.getTranslationY();

        if (state == WorldWind.BEGAN) {
            this.gestureDidBegin();
            this.lastX = 0;
            this.lastY = 0;
        } else if (state == WorldWind.CHANGED) {
            // Get the navigator's current position.
            double lat = this.camera.latitude;
            double lon = this.camera.longitude;
            double alt = this.camera.altitude;

            // Convert the translation from screen coordinates to degrees. Use the navigator's range as a metric for
            // converting screen pixels to meters, and use the globe's radius for converting from meters to arc degrees.
            double metersPerPixel = this.wwd.pixelSizeAtDistance(alt);
            double forwardMeters = (dy - this.lastY) * metersPerPixel;
            double sideMeters = -(dx - this.lastX) * metersPerPixel;
            this.lastX = dx;
            this.lastY = dy;

            double globeRadius = this.wwd.getGlobe().getRadiusAt(lat, lon);
            double forwardDegrees = Math.toDegrees(forwardMeters / globeRadius);
            double sideDegrees = Math.toDegrees(sideMeters / globeRadius);

            // Adjust the change in latitude and longitude based on the navigator's heading.
            double heading = this.camera.heading;
            double headingRadians = Math.toRadians(heading);
            double sinHeading = Math.sin(headingRadians);
            double cosHeading = Math.cos(headingRadians);
            lat += forwardDegrees * cosHeading - sideDegrees * sinHeading;
            lon += forwardDegrees * sinHeading + sideDegrees * cosHeading;

            // If the navigator has panned over either pole, compensate by adjusting the longitude and heading to move
            // the navigator to the appropriate spot on the other side of the pole.
            if (lat < -90 || lat > 90) {
                this.camera.latitude = Location.normalizeLatitude(lat);
                this.camera.longitude = Location.normalizeLongitude(lon + 180);
            } else if (lon < -180 || lon > 180) {
                this.camera.latitude = lat;
                this.camera.longitude = Location.normalizeLongitude(lon);
            } else {
                this.camera.latitude = lat;
                this.camera.longitude = lon;
            }
            //this.camera.heading = WWMath.normalizeAngle360(heading + sideDegrees * 1000);

            this.wwd.getNavigator().setAsCamera(this.wwd.getGlobe(), this.camera);
            this.wwd.requestRedraw();
        } else if (state == WorldWind.ENDED || state == WorldWind.CANCELLED) {
            this.gestureDidEnd();
        }
    }


    @Override
    protected void handleRotate(GestureRecognizer recognizer) {
        int state = recognizer.getState();
        float rotation = ((RotationRecognizer) recognizer).getRotation();

        if (state == WorldWind.BEGAN) {
            this.gestureDidBegin();
            this.lastRotation = 0;
        } else if (state == WorldWind.CHANGED) {

            // Apply the change in rotation to the navigator, relative to the navigator's current values.
            double headingDegrees = this.lastRotation - rotation;
            this.camera.heading = WWMath.normalizeAngle360(this.camera.heading + headingDegrees);
            this.lastRotation = rotation;

            this.wwd.getNavigator().setAsCamera(this.wwd.getGlobe(), this.camera);
            this.wwd.requestRedraw();
        } else if (state == WorldWind.ENDED || state == WorldWind.CANCELLED) {
            this.gestureDidEnd();
        }
    }

    @Override
    protected void handleTilt(GestureRecognizer recognizer) {
        int state = recognizer.getState();
        float dx = recognizer.getTranslationX();
        float dy = recognizer.getTranslationY();

        if (state == WorldWind.BEGAN) {
            this.gestureDidBegin();
            this.lastRotation = 0;
        } else if (state == WorldWind.CHANGED) {
            // Apply the change in tilt to the navigator, relative to when the gesture began.
            double headingDegrees = 180 * dx / this.wwd.getWidth();
            double tiltDegrees = -180 * dy / this.wwd.getHeight();

            this.camera.heading = WWMath.normalizeAngle360(this.beginCamera.heading + headingDegrees);
            this.camera.tilt = this.beginCamera.tilt + tiltDegrees;
            this.applyLimits(camera);

            this.wwd.getNavigator().setAsCamera(this.wwd.getGlobe(), this.camera);
            this.wwd.requestRedraw();
        } else if (state == WorldWind.ENDED || state == WorldWind.CANCELLED) {
            this.gestureDidEnd();
        }
    }
}