package github.hmasum18.satellight.service.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import github.hmasum18.satellight.service.model.Satellite;


@Dao
public interface SatelliteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Satellite... satellites);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<Satellite> satelliteList);

    @Query("SELECT * FROM satellite_data")
    LiveData<List<Satellite>> getAllSatelliteData();
}
