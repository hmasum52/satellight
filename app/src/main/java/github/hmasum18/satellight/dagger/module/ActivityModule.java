package github.hmasum18.satellight.dagger.module;

import dagger.Module;
import dagger.Provides;
import github.hmasum18.satellight.dagger.anotation.MainActivityScope;
import github.hmasum18.satellight.dagger.component.ActivityComponent;
import github.hmasum18.satellight.view.MainActivity;

@Module/*(subcomponents = {ActivityComponent.class})*/
public class ActivityModule {
    private final MainActivity mainActivity;

    public ActivityModule(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Provides
    MainActivity provideMainActivity(){
        return mainActivity;
    }
}
