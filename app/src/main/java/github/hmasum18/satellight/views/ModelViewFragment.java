package github.hmasum18.satellight.views;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import github.hmasum18.satellight.R;


public class ModelViewFragment extends Fragment {

    private WebView webView;
    MapsActivity mapsActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        mapsActivity = (MapsActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_model_view, container, false);

        webView = rootView.findViewById(R.id.modelViewFrag_webView);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        webView.getSettings().setJavaScriptEnabled(true);

        mapsActivity.progressBar.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                mapsActivity.progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }
        });
        webView.loadUrl("https://satellight.netlify.app/");
    }
}