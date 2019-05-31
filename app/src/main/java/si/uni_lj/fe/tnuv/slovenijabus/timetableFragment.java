package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")

public class timetableFragment extends Fragment implements DownloadCallback {

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request

    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request

    List<List<HashMap<String, String>>> listOfChildGroups;
    CustomExpandableListAdapter adapter;
    public ArrayList<Integer> alreadyDownloadedLines = new ArrayList<>();
    ExpandableListView lv;
    TextView msg;
    private static final String ARG_REQUEST_STRING = "req_str";
    private static final String ARG_INVALID_STATION = "invalid_station";
    private String request_string;
    private boolean invalid_station;

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
        if (!invalid_station) {
            getTimetablesFromAPI(request_string);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);
        msg = view.findViewById(R.id.fragment_message);
        lv = view.findViewById(R.id.show_all_list);
        if (invalid_station) {
            msg.setText(R.string.invalid_station_message);
            msg.setVisibility(View.VISIBLE);
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
            Toast.makeText(getActivity().getApplicationContext(), R.string.network_error_message, Toast.LENGTH_SHORT).show();
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
            Log.d("time_string", time_str);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = sdf.format(new Date());
            Log.d("time_string_now", now);
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

        if (getActivity() == null) {
            Log.d("fragment", "disaster avoided");
            return;
        }

        if (request.get("url").equals(API_voznired)) {

            if (result_string.equals("error")) {
                msg.setText(R.string.network_error_message);
                msg.setVisibility(View.VISIBLE);
                return;
            }

            if (result_string.length() < 2) {
                msg.setText(R.string.no_buses_message);
                msg.setVisibility(View.VISIBLE);
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
                Log.d("fragment_request_date", request_date.toString());
                Log.d("fragment_response_date", response_date.toString());
            } catch (Exception e) {
                Log.d("fragment_parse_excptn", "No worky worky");
                Log.d("fragment_request_date", request_string.split("=")[3]);
                Log.d("fragment_response_date", timetable.get(0).get("date"));
            }
            lv.setVisibility(View.VISIBLE);

            listOfChildGroups = new ArrayList<>();
            for (int i = 0; i < timetable.size(); i++) {
                listOfChildGroups.add(i, new ArrayList<HashMap<String, String>>());
            }

            String[] parentFromArray = {"entry_time", "exit_time", "duration", "price"};
            int[] parentToArray = {R.id.entry_time, R.id.exit_time, R.id.duration, R.id.price};

            String[] childFromArray = {"time", "station"};
            int[] childToArray = {R.id.dropdown_item_time, R.id.dropdown_item_station};

            String[] firstChildFromArry = {"company", "line"};
            int[] firstChildToArray = {R.id.dropdown_item_company, R.id.dropdown_item_line};


            adapter = new CustomExpandableListAdapter(getActivity().getApplicationContext(),
                    timetable, R.layout.show_all_list_item, parentFromArray, parentToArray,
                    listOfChildGroups, R.layout.dropdown_list_item, R.layout.dropdown_list_first_item,
                    childFromArray, childToArray, firstChildFromArry, firstChildToArray, first_index);
            lv.setAdapter(adapter);
            lv.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                    if (alreadyDownloadedLines.contains(groupPosition)) {
                        return false;
                    }
                    HashMap<String, String> group = (HashMap<String, String>) adapter.getGroup(groupPosition);
                    String req_data = group.get("line_data");
                    try {
                        ArrayList<HashMap<String, String>> line_data = getLineDataFromAPI2(req_data);
                        List childGroup = listOfChildGroups.get(groupPosition);
                        for (HashMap<String, String> hm : line_data) {
                            childGroup.add(hm);
                        }
                        alreadyDownloadedLines.add(groupPosition);
                        return false;
                    } catch (Exception e) {
                        return true;
                    }
                }
            });
            lv.setSelectedGroup(first_index);
            lv.smoothScrollToPosition(first_index);
            Log.d("first_index", Integer.toString(first_index));
        }

        if (request.get("url").equals(API_podatki_relacija) && false) { // trenutno disablan
            int groupPosition = Integer.parseInt(request.get("group"));
            if (result_string.equals("error")) {
                Toast.makeText(getActivity().getApplicationContext(), R.string.network_error_message, Toast.LENGTH_LONG).show();
                return;
            }

            List childGroup = listOfChildGroups.get(groupPosition);
            ArrayList<HashMap<String, String>> line_data = lineDataParser2(result_string);
            for (HashMap<String, String> hm : line_data) {
                childGroup.add(hm);
            }

            adapter.notifyDataSetChanged();
            alreadyDownloadedLines.add(groupPosition);
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

    public HashMap<String, Object> lineDataParser(String input) {
        String[] splitted = input.split("\n");
        HashMap<String, Object> output = new HashMap<>();

        String start = splitted[0].split("\\|")[1];
        output.put("start", start);

        String destination = splitted[splitted.length - 1].split("\\|")[1];
        output.put("end", destination);

        String startEnd = start + " - " + destination;
        output.put("start_end", startEnd);

        String company = splitted[0].split("\\|")[0];
        output.put("company", company);

        ArrayList<HashMap<String, String>> visitedStations = new ArrayList<>();

        for (int i = 1; i < splitted.length - 1; i++) {
            if (i == 1) {
                String[] s = splitted[i].split("\\|");
                s = Arrays.copyOfRange(s, 1, s.length);
                s[1] = s[1].substring(11, 16);
                HashMap<String, String> ss = new HashMap<>();
                ss.put("time", s[1]);
                ss.put("station", s[0]);
                if (s.length > 3) {
                    ss.put("wait", s[3]);
                }
                visitedStations.add(ss);
                continue;
            }
            String[] s = splitted[i].split("\\|");
            s[2] = s[2].substring(11, 16);
            HashMap<String, String> ss = new HashMap<>();
            ss.put("time", s[2]);
            ss.put("station", s[1]);
            if (s.length > 4) {
                ss.put("wait", s[4]);
            }
            visitedStations.add(ss);
        }
        output.put("visited_stations", visitedStations);
        return output;
    }

    public ArrayList<HashMap<String, String>> lineDataParser2(String input) {
        String[] splitted = input.split("\n");

        ArrayList<HashMap<String, String>> output = new ArrayList<>();

        String start = splitted[0].split("\\|")[1];
        String destination = splitted[splitted.length - 1].split("\\|")[1];
        String startEnd = start + " - " + destination;
        String company = splitted[0].split("\\|")[0];

        HashMap<String, String> first_item = new HashMap<>();
        first_item.put("company", company);
        first_item.put("line", startEnd);
        first_item.put("start", start);
        first_item.put("destination", destination);

        output.add(first_item);

        for (int i = 1; i < splitted.length - 1; i++) {
            if (i == 1) {
                String[] s = splitted[i].split("\\|");
                s = Arrays.copyOfRange(s, 1, s.length);
                s[1] = s[1].substring(11, 16);
                HashMap<String, String> ss = new HashMap<>();
                ss.put("time", s[1]);
                if (s.length > 3) {
                    ss.put("station", s[0] + " (" + s[3] + ")");
                } else {
                    ss.put("station", s[0]);
                }
                output.add(ss);
                continue;
            }
            String[] s = splitted[i].split("\\|");
            s[2] = s[2].substring(11, 16);
            HashMap<String, String> ss = new HashMap<>();
            ss.put("time", s[2]);
            if (s.length > 4) {
                ss.put("station", s[1] + " (" + s[4] + ")");
            } else {
                ss.put("station", s[1]);
            }
            output.add(ss);
        }
        return output;
    }

    public void getLineDataFromAPI(String data, int i) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_podatki_relacija);
        request.put("method", "POST");
        request.put("data", "flags=" + data);
        request.put("group", Integer.toString(i));
        makeHttpRequest(request);
    }

    public ArrayList<HashMap<String, String>> getLineDataFromAPI2(String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_podatki_relacija);
        request.put("method", "POST");
        request.put("data", "flags=" + data);
        HashMap<String, Object> result = makeHttpRequest2(request);
        String result_string = (String) result.get("response");
        int response_code = (int) result.get("response_code");

        if (result_string.equals("error") || response_code != 200) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.network_error_message, Toast.LENGTH_LONG).show();
            return null;
        }
        ArrayList<HashMap<String, String>> line_data = lineDataParser2(result_string);
        return line_data;
    }

    public HashMap<String, Object> makeHttpRequest2(HashMap<String, String> request) {
        NetworkInfo netInfo = getActiveNetworkInfo();
        HashMap<String, Object> result;
        if (netInfo != null && netInfo.isConnected()) {
            try {
                Object res = new DownloadAsyncTask(this).execute(request).get(1500, TimeUnit.MILLISECONDS);
                result = (HashMap<String, Object>) res;
                return result;
            } catch (Exception e) {
                return null;
            }
        } else {
            Toast.makeText(getActivity().getApplicationContext(), R.string.network_error_message, Toast.LENGTH_SHORT).show();
            result = new HashMap<>();
            result.put("response", "error");
            return result;
        }
    }
}
