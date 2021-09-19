package github.hmasum18.satellight.dagger.module;

import android.content.Context;
import android.util.Log;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import github.hmasum18.satellight.dagger.anotation.MainActivityScope;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer;
import gov.nasa.worldwind.layer.BlueMarbleLayer;

@Module
public abstract class WorldWindModule {
    private static final String TAG = "WorldWindModule";
    /**
     * Creates a new WorldWindow (GLSurfaceView) object.
     */
    @Provides
    @Singleton
    static WorldWindow provideWorldWindow(Context context){
        Log.d(TAG, "provideWorldWindow: creating world window");
        WorldWindow worldWindow = new WorldWindow(context);
        worldWindow.getLayers().addLayer(new BlueMarbleLayer());
        worldWindow.getLayers().addLayer(new BlueMarbleLandsatLayer());
        return worldWindow;
    }
}
