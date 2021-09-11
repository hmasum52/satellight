package github.hmasum18.satellight.view.adapter;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.databinding.CardSatelliteBinding;
import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.view.OnSelectedSatelliteUpdateListener;

@Singleton
public class SatelliteListAdapter extends RecyclerView.Adapter<SatelliteListAdapter.Holder> {
    private static final String TAG = "SatelliteListAdapter";
    private List<Satellite> satelliteList = new ArrayList<>();
    private List<Satellite> filteredList = new ArrayList<>();
    private SearchView searchView;

    private Satellite selectedSatellite = null;
    private OnSelectedSatelliteUpdateListener selectedSatelliteUpdateListener;

    @Inject
    public SatelliteListAdapter() {
    }

    public void setSelectedSatellite(Satellite selectedSatellite) {
        this.selectedSatellite = selectedSatellite;
        if(selectedSatelliteUpdateListener!=null)
            selectedSatelliteUpdateListener.onSelectedSatelliteUpdate(selectedSatellite);
        super.notifyDataSetChanged();
    }

    public Satellite getSelectedSatellite() {
        return selectedSatellite;
    }

    public void setSelectedSatelliteUpdateListener(OnSelectedSatelliteUpdateListener selectedSatelliteUpdateListener) {
        this.selectedSatelliteUpdateListener = selectedSatelliteUpdateListener;
        if(selectedSatelliteUpdateListener!=null)
            selectedSatelliteUpdateListener.onSelectedSatelliteUpdate(selectedSatellite);
    }

    public void setSearchView(SearchView searchView) {
        this.searchView = searchView;

        // search for problem
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return update(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return update(newText);
            }
        });
    }

    private boolean update(String s) {
        filteredList = new ArrayList<>();
        for (Satellite sat : satelliteList) {
            if (isSubSequence(s.toLowerCase(), sat.getName().toLowerCase()
                    , s.length(), sat.getName().length()))
                filteredList.add(sat);
        }
        super.notifyDataSetChanged();
        return true;
    }

    public void setSatelliteList(List<Satellite> satelliteList) {
        this.satelliteList = satelliteList;
        this.filteredList = satelliteList;
        if (satelliteList.size() > 0 && selectedSatellite == null){
            setSelectedSatellite(satelliteList.get(0));
        }else
            super.notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_satellite, parent, false);
        return new Holder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {
        Satellite satellite = filteredList.get(position);

        Glide.with(holder.mVB.getRoot()).load(satellite.getIconUrl()).into(holder.mVB.satelliteLogo);
        holder.mVB.satelliteName.setText(satellite.getName());
        holder.mVB.country.setText(satellite.getCountryName());

        // background color
        String color = satellite.getId() == selectedSatellite.getId() ? "#E1E1E1" : "#ffffff";
        holder.mVB.getRoot().setBackgroundColor(Color.parseColor(color));

        holder.mVB.getRoot().setOnClickListener(v -> {
            setSelectedSatellite(satellite);
            Log.d(TAG, "onBindViewHolder: selected:"+satellite.getName());
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        CardSatelliteBinding mVB;

        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            mVB = CardSatelliteBinding.bind(itemView);
        }
    }

    // Returns true if str1[] is a subsequence of str2[]
    // m is length of str1 and n is length of str2
    static boolean isSubSequence(String str1, String str2,
                                 int m, int n) {
        // Base Cases
        if (m == 0)
            return true;
        if (n == 0)
            return false;

        // If last characters of two strings are matching
        if (str1.charAt(m - 1) == str2.charAt(n - 1))
            return isSubSequence(str1, str2, m - 1, n - 1);

        // If last characters are not matching
        return isSubSequence(str1, str2, m, n - 1);
    }
}
