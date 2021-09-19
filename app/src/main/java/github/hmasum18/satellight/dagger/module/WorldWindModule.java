package github.hmasum18.satellight.dagger.module;

import android.content.Context;
import android.util.Log;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import github.hmasum18.satellight.R;
import github.hmasum18.satellight.dagger.anotation.MainActivityScope;
import github.hmasum18.satellight.experimental.AtmosphereLayer;
import github.hmasum18.satellight.view.screen.globe.CameraController;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layer.BackgroundLayer;
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer;
import gov.nasa.worldwind.layer.BlueMarbleLayer;
import gov.nasa.worldwind.render.ImageOptions;
import gov.nasa.worldwind.render.ImageSource;

@Module
public abstract class WorldWindModule {
    private static final String TAG = "WorldWindModule";
    /**
     * Creates a new WorldWindow (GLSurfaceView) object.
     */
    @Provides
    static WorldWindow provideWorldWindow(Context context){
        Log.d(TAG, "provideWorldWindow: creating world window");
        WorldWindow worldWindow = new WorldWindow(context);
        worldWindow.getLayers().addLayer(new BlueMarbleLayer());
        worldWindow.getLayers().addLayer(new BlueMarbleLandsatLayer());
        worldWindow.setWorldWindowController(new CameraController());
        return worldWindow;
    }
}
