package com.tareksaidee.marvel101;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 */
public class CharSearchFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<ArrayList<Character>> {

    private static final String QUERY_URL = "https://gateway.marvel.com:443/v1/public/characters";
    private static int CHARACTER_LOADER_ID = 1;
    ArrayList<Character> characters;
    CharacterAdapter adapter;
    ListView listView;
    EditText charSearchBox;
    Button searchButton;
    CheckBox startsWithCheck;

    public CharSearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_char_search, container, false);
        listView = (ListView) rootView.findViewById(R.id.list);
        charSearchBox = (EditText) rootView.findViewById(R.id.char_search_box);
        searchButton = (Button) rootView.findViewById(R.id.start_search_button);
        startsWithCheck = (CheckBox) rootView.findViewById(R.id.starts_with_check);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLoaderManager().destroyLoader(CHARACTER_LOADER_ID);
                getLoaderManager().initLoader(CHARACTER_LOADER_ID, null, CharSearchFragment.this);
            }
        });
        return rootView;
    }


    @Override
    public android.support.v4.content.Loader<ArrayList<Character>> onCreateLoader(int id, Bundle args) {
        Uri baseUri = Uri.parse(QUERY_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        String timeStamp = Calendar.getInstance().getTime().toString();
        uriBuilder.appendQueryParameter("apikey", SECRET_KEYS.PUBLIC_KEY);
        uriBuilder.appendQueryParameter("limit", "50");
        uriBuilder.appendQueryParameter("ts", timeStamp);
        uriBuilder.appendQueryParameter("hash", QueryUtils.getMD5Hash(timeStamp));
        if (startsWithCheck.isChecked())
            uriBuilder.appendQueryParameter("nameStartsWith", charSearchBox.getText().toString());
        else
            uriBuilder.appendQueryParameter("name", charSearchBox.getText().toString());
        return new CharacterLoader(this.getContext(), uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<ArrayList<Character>> loader, ArrayList<Character> data) {
        characters = data;
        adapter = new CharacterAdapter(getContext(), characters);
        listView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<ArrayList<Character>> loader) {
        adapter.clear();
    }

    private static class CharacterLoader extends android.support.v4.content.AsyncTaskLoader<ArrayList<Character>> {

        private String mUrl;

        CharacterLoader(Context context, String url) {
            super(context);
            mUrl = url;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public ArrayList<Character> loadInBackground() {
            return QueryUtils.extractCharacters(NetworkUtils.getData(mUrl));
        }
    }
}
