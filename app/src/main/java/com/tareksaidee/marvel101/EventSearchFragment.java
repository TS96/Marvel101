package com.tareksaidee.marvel101;


import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import static android.view.View.GONE;


/**
 * A simple {@link Fragment} subclass.
 */
public class EventSearchFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Pair<ArrayList<Event>, Integer>> {

    private static final String QUERY_URL = "https://gateway.marvel.com:443/v1/public/events";
    private static int EVENTS_LOADER_ID = 5;
    private final int LIMIT = 15;
    ArrayList<Event> events;
    EventAdapter adapter;
    ListView listView;
    EditText eventSearchBox;
    Button searchButton;
    CheckBox startsWithCheck;
    TextView emptyView;
    ProgressBar progressBar;
    Button nextPageButton;
    Button previousPageButton;
    private int offset = 0;
    private int total;
    private boolean artificialClick = false;

    public EventSearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_event_search, container, false);
        listView = (ListView) rootView.findViewById(R.id.list);
        eventSearchBox = (EditText) rootView.findViewById(R.id.event_search_box);
        searchButton = (Button) rootView.findViewById(R.id.start_search_button);
        emptyView = (TextView) rootView.findViewById(R.id.empty_view);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        Typeface face = Typeface.createFromAsset(getActivity().getAssets(), "fonts/comicsfont.TTF");
        eventSearchBox.setTypeface(face);
        RelativeLayout footerLayout = (RelativeLayout) inflater.inflate(R.layout.listview_footer, null);
        listView.addFooterView(footerLayout);
        nextPageButton = (Button) footerLayout.findViewById(R.id.next_page_button);
        previousPageButton = (Button) footerLayout.findViewById(R.id.previous_page_button);
        nextPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offset += LIMIT;
                artificialClick = true;
                searchButton.performClick();
            }
        });
        previousPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offset -= LIMIT;
                artificialClick = true;
                searchButton.performClick();
            }
        });
        progressBar.setVisibility(GONE);
        emptyView.setVisibility(GONE);
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        startsWithCheck = (CheckBox) rootView.findViewById(R.id.starts_with_check);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!artificialClick) {
                    offset = 0;
                }
                if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                    getLoaderManager().destroyLoader(EVENTS_LOADER_ID);
                    getLoaderManager().initLoader(EVENTS_LOADER_ID, null, EventSearchFragment.this);
                    emptyView.setText("");
                    listView.setEmptyView(emptyView);
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(GONE);
                    emptyView.setText("No Internet Connection");
                }
                //close keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(eventSearchBox.getWindowToken(), 0);
                artificialClick = false;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateLayout(view, adapter.getItem(position));
            }
        });
        return rootView;
    }

    @Override
    public Loader<Pair<ArrayList<Event>, Integer>> onCreateLoader(int id, Bundle args) {
        Uri baseUri = Uri.parse(QUERY_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        String timeStamp = Calendar.getInstance().getTime().toString();
        uriBuilder.appendQueryParameter("apikey", SECRET_KEYS.PUBLIC_KEY);
        uriBuilder.appendQueryParameter("limit", LIMIT + "");
        uriBuilder.appendQueryParameter("offset", offset + "");
        uriBuilder.appendQueryParameter("ts", timeStamp);
        uriBuilder.appendQueryParameter("hash", QueryUtils.getMD5Hash(timeStamp));
        if (startsWithCheck.isChecked())
            uriBuilder.appendQueryParameter("nameStartsWith", eventSearchBox.getText().toString());
        else
            uriBuilder.appendQueryParameter("name", eventSearchBox.getText().toString());
        return new EventSearchFragment.EventsLoader(this.getContext(), uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<Pair<ArrayList<Event>, Integer>> loader, Pair<ArrayList<Event>, Integer> data) {
        events = data.first;
        total = data.second;
        adapter = new EventAdapter(getContext(), events);
        listView.setAdapter(adapter);
        emptyView.setText("No Events Found");
        progressBar.setVisibility(GONE);
        if (offset + LIMIT >= total)
            nextPageButton.setVisibility(GONE);
        else
            nextPageButton.setVisibility(View.VISIBLE);
        if (offset == 0)
            previousPageButton.setVisibility(GONE);
        else
            previousPageButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Pair<ArrayList<Event>, Integer>> loader) {
        adapter.clear();
    }

    private static class EventsLoader extends android.support.v4.content.AsyncTaskLoader<Pair<ArrayList<Event>, Integer>> {

        private String mUrl;

        EventsLoader(Context context, String url) {
            super(context);
            mUrl = url;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public Pair<ArrayList<Event>, Integer> loadInBackground() {
            return QueryUtils.extractEvents(NetworkUtils.getData(mUrl));
        }
    }

    private void updateLayout(View tempView, Event temp) {
        TextView descrp = ((TextView) tempView.findViewById(R.id.event_description));
        Button goToDetails = (Button) tempView.findViewById(R.id.open_details_button);
        LinearLayout expContainer = (LinearLayout) tempView.findViewById(R.id.expandable_views_container);
        if (!temp.wasClicked()) {
            descrp.setMaxLines(20);
            expContainer.setVisibility(View.VISIBLE);
            if (temp.getDetailsURL() == null)
                goToDetails.setVisibility(View.GONE);
            temp.gotClicked();
        } else {
            descrp.setMaxLines(3);
            expContainer.setVisibility(GONE);
            temp.unClicked();
        }
    }
}
