package github.hmasum18.satellight.service.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import github.hmasum18.satellight.service.model.Satellite;

@Database(entities = {Satellite.class},exportSchema = false, version = 1)
public abstract class SatelliteDatabase extends RoomDatabase {
    public abstract SatelliteDao satelliteDao(); // room will implement this
}
