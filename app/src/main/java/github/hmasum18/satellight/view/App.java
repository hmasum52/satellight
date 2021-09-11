package github.hmasum18.satellight.view;

import android.app.Application;

import github.hmasum18.satellight.dagger.component.AppComponent;


import github.hmasum18.satellight.dagger.component.DaggerAppComponent;
import github.hmasum18.satellight.dagger.module.AppModule;

public class App extends Application {

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}
