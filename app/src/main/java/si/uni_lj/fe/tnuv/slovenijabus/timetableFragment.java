package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@SuppressWarnings("unchecked")

public class timetableFragment extends Fragment implements DownloadCallback {

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request

    private RecyclerView.Adapter showAll_recycler_adapter;
    private RecyclerView.LayoutManager showAll_layoutManager;
    private RecyclerView showAll_rv;

    TextView msg, msg_fullscreen;
    private static final String ARG_REQUEST_STRING = "req_str";
    private static final String ARG_INVALID_STATION = "invalid_station";
    private String request_string;
    private boolean invalid_station;

    private ProgressBar progressBar;

    public timetableFragment() {
    }

    public static timetableFragment newInstance(String param, boolean invalid) {
        timetableFragment fragment = new timetableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_STRING, param);
        args.putBoolean(ARG_INVALID_STATION, invalid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            request_string = getArguments().getString(ARG_REQUEST_STRING);
            invalid_station = getArguments().getBoolean(ARG_INVALID_STATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_timetable, container, false);
        msg = view.findViewById(R.id.fragment_message);
        msg_fullscreen = view.findViewById(R.id.fragment_message_fullscreen);
        showAll_rv = view.findViewById(R.id.show_all_list);
        progressBar = view.findViewById(R.id.progressBar);
        if (!invalid_station) {
            getTimetablesFromAPI(request_string);
        }
        if (invalid_station) {
            showAll_rv.setVisibility(View.GONE);
            msg_fullscreen.setText(R.string.invalid_station_message);
            msg_fullscreen.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public void getTimetablesFromAPI(String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_voznired);
        request.put("method", "POST");
        request.put("data", data);

        makeHttpRequest(request);
    }

    public void makeHttpRequest(HashMap<String, String> request) {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask(this).execute(request);
        } else {
            showAll_rv.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            msg_fullscreen.setText(R.string.network_error_message);
            msg_fullscreen.setVisibility(View.VISIBLE);
        }
    }

    public HashMap<String, Object> timetableParser(String input) {
        String[] splitted = input.split("\n");
        ArrayList<HashMap<String, String>> outputTimetable = new ArrayList<>();
        boolean first = true;
        int first_index = 0;
        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];
            String[] separated = s.split("\\|");
            String time_str = separated[6].substring(0, separated[6].length() - 3);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = sdf.format(new Date());

            if (now.compareTo(time_str) < 1 && first) {
                first = false;
                first_index = i;
            }
            HashMap<String, String> timetable = new HashMap<>();
            timetable.put("entry_time", separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("exit_time", separated[7].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("date", separated[6].substring(0, 10));
            timetable.put("entry_time_long", separated[6]);
            timetable.put("exit_time_long", separated[7]);
            timetable.put("duration", separated[8]);
            timetable.put("price", separated[9].replace(".", ",") + " €");
            timetable.put("line_data", separated[13]);
            outputTimetable.add(timetable);
        }
        if (first) {
            first_index = outputTimetable.size();
        }

        HashMap<String, Object> output = new HashMap<>();
        output.put("timetable", outputTimetable);
        output.put("index", first_index);
        return output;
    }

    @Override
    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");
        String result_string = (String) result.get("response");

        progressBar.setVisibility(View.GONE);

        if (getActivity() == null) {
            //Log.d("fragment", "disaster avoided");
            return;
        }

        if (request.get("url").equals(API_voznired)) {

            if (result_string.equals("error")) {
                showAll_rv.setVisibility(View.GONE);
                msg_fullscreen.setText(R.string.network_error_message);
                msg_fullscreen.setVisibility(View.VISIBLE);
                return;
            }

            if (result_string.length() < 2) {
                showAll_rv.setVisibility(View.GONE);
                msg_fullscreen.setText(R.string.no_buses_message);
                msg_fullscreen.setVisibility(View.VISIBLE);
                return;
            }

            HashMap<String, Object> data = timetableParser(result_string);
            int first_index = (int) data.get("index");
            ArrayList<HashMap<String, String>> timetable = (ArrayList<HashMap<String, String>>) data.get("timetable");

            SimpleDateFormat sdf_request = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat sdf_response = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date request_date = sdf_request.parse(request_string.split("=")[3]);
                Date response_date = sdf_response.parse(timetable.get(0).get("date"));
                if (request_date.before(response_date)) {
                    msg.setText(getString(R.string.no_buses_on_this_day, sdf_request.format(response_date)));
                    msg.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
/*                Log.d("fragment_parse_excptn", "No worky worky");
                Log.d("fragment_request_date", request_string.split("=")[3]);
                Log.d("fragment_response_date", timetable.get(0).get("date"));*/
            }
            //showAll_rv.setVisibility(View.VISIBLE);

            String[] parentFromArray = {"entry_time", "exit_time", "duration", "price"};
            int[] parentToArray = {R.id.entry_time, R.id.exit_time, R.id.duration, R.id.price};

            String[] childFromArray = {"time", "station"};
            int[] childToArray = {R.id.dropdown_item_time, R.id.dropdown_item_station};

            String[] firstChildFromArry = {"company", "line"};
            int[] firstChildToArray = {R.id.dropdown_item_company, R.id.dropdown_item_line};


            showAll_recycler_adapter = new ShowAllRecyclerAdapter(getActivity().getApplicationContext(),
                    timetable, R.layout.show_all_list_item, parentFromArray, parentToArray,
                    R.layout.dropdown_list_item, R.layout.dropdown_list_first_item,
                    childFromArray, childToArray, firstChildFromArry, firstChildToArray, first_index, showAll_rv);

            showAll_layoutManager = new LinearLayoutManager(getActivity());

            showAll_rv.setItemViewCacheSize(30);
            showAll_rv.setLayoutManager(showAll_layoutManager);
            showAll_rv.setAdapter(showAll_recycler_adapter);
            if (first_index == timetable.size()) {
                showAll_rv.scrollToPosition(first_index - 1);
            } else {
                showAll_rv.scrollToPosition(first_index);
            }
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
    }

    @Override
    public void finishDownloading() {
    }
}
