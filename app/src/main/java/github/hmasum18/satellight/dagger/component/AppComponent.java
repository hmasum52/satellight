package github.hmasum18.satellight.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import github.hmasum18.satellight.dagger.module.AppModule;
import github.hmasum18.satellight.dagger.module.WorldWindModule;
import github.hmasum18.satellight.dagger.module.NetworkModule;
import github.hmasum18.satellight.dagger.module.RoomModule;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class,
        NetworkModule.class, WorldWindModule.class})
public interface AppComponent{
    ActivityComponent.Builder activityComponentBuilder();
}
