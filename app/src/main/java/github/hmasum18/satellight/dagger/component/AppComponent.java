package github.hmasum18.satellight.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import github.hmasum18.satellight.dagger.module.AppModule;
import github.hmasum18.satellight.dagger.module.network.NetworkModule;
import github.hmasum18.satellight.dagger.module.RoomModule;
import github.hmasum18.satellight.view.MainActivity;
import github.hmasum18.satellight.view.fragment.GoogleMapFragment;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class, NetworkModule.class})
public interface AppComponent{
    void inject(MainActivity mainActivity);
    void inject(GoogleMapFragment googleMapFragment);
}
