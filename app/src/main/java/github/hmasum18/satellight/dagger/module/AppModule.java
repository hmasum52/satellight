package github.hmasum18.satellight.dagger.module;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import github.hmasum18.satellight.view.App;

@Module
public class AppModule{
    private App app;

    public AppModule(App app) {
        this.app = app;
    }

    @Provides
    @Singleton
    App provideApp(){
        return app;
    }

    @Provides
    @Singleton
    Context provideContext(){return app.getApplicationContext();}

    @Provides
    @Singleton
    Application provideApplication(){
        return app;
    }
}
