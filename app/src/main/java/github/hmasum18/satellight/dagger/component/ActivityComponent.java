package github.hmasum18.satellight.dagger.component;

import dagger.BindsInstance;
import dagger.Subcomponent;
import github.hmasum18.satellight.dagger.anotation.MainActivityScope;
import github.hmasum18.satellight.dagger.module.ActivityModule;
import github.hmasum18.satellight.view.MainActivity;
import github.hmasum18.satellight.view.screen.GlobeFragment;
import github.hmasum18.satellight.view.screen.googlemap.GoogleMapFragment;

@MainActivityScope
@Subcomponent(modules = {ActivityModule.class})
public interface ActivityComponent{
    void inject(MainActivity mainActivity);
    void inject(GoogleMapFragment googleMapFragment);
    void inject(GlobeFragment globeFragment);

    @Subcomponent.Builder
    interface Builder{
        ActivityComponent build();
        Builder activityModule(ActivityModule activityModule);
    }
}
