package github.hmasum18.satellight.viewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersistenceViewModel extends ViewModel{

    private String currentSatelliteName;

    @Inject
    public PersistenceViewModel() {
    }


}
