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
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class timetableFragment extends Fragment implements DownloadCallback {

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request

    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request

    List<List<HashMap<String, String>>> listOfChildGroups;
    SimpleExpandableListAdapter adapter;
    public ArrayList<Integer> alreadyDownloadedLines = new ArrayList<>();
    ExpandableListView lv;
    TextView msg;
    private static final String ARG_REQUEST_STRING = "req_str";
    private String request_string;
    ArrayList<HashMap<String, String>> timetable;

    public timetableFragment() {
    }

    public static timetableFragment newInstance(String param) {
        timetableFragment fragment = new timetableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_STRING, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            request_string = getArguments().getString(ARG_REQUEST_STRING);
        }
        getTimetablesFromAPI(request_string);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);
        msg = view.findViewById(R.id.fragment_message);
        lv = view.findViewById(R.id.show_all_list);
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

    public ArrayList<HashMap<String, String>> timetableParserAll(String input) {
        String[] splitted = input.split("\n");
        ArrayList<HashMap<String, String>> output = new ArrayList<>();

        for (String s : splitted) {
            String[] separated = s.split("\\|");
            HashMap<String, String> timetable = new HashMap<>();
            timetable.put("entry_time", separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("exit_time", separated[7].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("entry_time_long", separated[6]);
            timetable.put("exit_time_long", separated[7]);
            timetable.put("duration", separated[8]);
            timetable.put("price", separated[9]);
            timetable.put("line_data", separated[13]);
            output.add(timetable);
        }
        return output;
    }

    public ArrayList<HashMap<String, String>> timetableParserCurrent(String input) {
        String[] splitted = input.split("\n");
        ArrayList<HashMap<String, String>> output = new ArrayList<>();
        Boolean first = Boolean.TRUE;
        int first_index = 0;
        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];
            String[] separated = s.split("\\|");
            String time_str = separated[6].substring(0, separated[6].length() - 3);
            Log.d("time_string", time_str);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = sdf.format(new Date());
            Log.d("time_string_now", now);
            if (now.compareTo(time_str) < 1) {
                if (first) {
                    first = Boolean.FALSE;
                    first_index = i;
                }
                HashMap<String, String> timetable = new HashMap<>();
                timetable.put("entry_time", separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
                timetable.put("exit_time", separated[7].substring(11, 16).replaceFirst("^0+(?!$)", ""));
                timetable.put("entry_time_long", separated[6]);
                timetable.put("exit_time_long", separated[7]);
                timetable.put("duration", separated[8]);
                timetable.put("price", separated[9]);
                timetable.put("line_data", separated[13]);
                timetable.put("expired", "True");
                output.add(timetable);
            } else {
                HashMap<String, String> timetable = new HashMap<>();
                timetable.put("entry_time", separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
                timetable.put("exit_time", separated[7].substring(11, 16).replaceFirst("^0+(?!$)", ""));
                timetable.put("entry_time_long", separated[6]);
                timetable.put("exit_time_long", separated[7]);
                timetable.put("duration", separated[8]);
                timetable.put("price", separated[9]);
                timetable.put("line_data", separated[13]);
                timetable.put("expired", "False");
                output.add(timetable);
            }
        }
        HashMap<String, String> index = new HashMap<>();
        index.put("first_index", Integer.toString(first_index));
        output.add(index);
        return output;
    }


    @Override
    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");
        String result_string = (String) result.get("response");

        if (result_string.length() < 2) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.no_buses_message), Toast.LENGTH_LONG).show();
            return;
        }

        if (result_string.equals("error")) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.network_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        if (request.get("url").equals(API_voznired)) {
            timetable = timetableParserCurrent(result_string);
            int first_index = Integer.parseInt(timetable.get(timetable.size() - 1).get("first_index"));
            timetable.remove(timetable.size() - 1);

            if (timetable.isEmpty()) {
                msg.setText(R.string.no_buses);
                msg.setVisibility(View.VISIBLE);
            } else {
                lv.setVisibility(View.VISIBLE);
            }

            listOfChildGroups = new ArrayList<List<HashMap<String, String>>>();
            for (int i = 0; i < timetable.size(); i++) {
                listOfChildGroups.add(i, new ArrayList<HashMap<String, String>>());
            }

            String[] parentFromArray = {"entry_time", "exit_time", "duration"};
            int[] parentToArray = {R.id.entry_time, R.id.exit_time, R.id.duration};

            String[] childFromArray = {"label", "data"};
            int[] childToArray = {R.id.dropdown_item_label, R.id.dropdown_item_data};


            adapter = new SimpleExpandableListAdapter(getActivity().getApplicationContext(),
                    timetable, R.layout.show_all_list_item, parentFromArray, parentToArray,
                    listOfChildGroups, R.layout.dropdown_list_item, childFromArray, childToArray);
            lv.setAdapter(adapter);
            lv.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                @Override
                public void onGroupExpand(int groupPosition) {
                    if (alreadyDownloadedLines.contains(groupPosition)) {
                        return;
                    }
                    HashMap<String, String> group = (HashMap<String, String>) adapter.getGroup(groupPosition);
                    String req_data = group.get("line_data");
                    getLineDataFromAPI(req_data, groupPosition);
                    alreadyDownloadedLines.add(groupPosition);
                }
            });
            lv.setSelectedGroup(first_index);
            lv.smoothScrollToPosition(first_index);
        }

        if (request.get("url").equals(API_podatki_relacija)) {
            HashMap<String, Object> line_data = lineDataParser(result_string);

            int groupPosition = Integer.parseInt(request.get("group"));
            List childGroup = listOfChildGroups.get(groupPosition);

            HashMap<String, String> prevoznikmap = new HashMap<>();
            prevoznikmap.put("label", getString(R.string.prevoznik) + ":");
            prevoznikmap.put("data", (String) line_data.get("company"));
            childGroup.add(prevoznikmap);

            HashMap<String, String> relacijamap = new HashMap<>();
            relacijamap.put("label", getString(R.string.relacija) + ":");
            relacijamap.put("data", (String) line_data.get("start_end"));
            childGroup.add(relacijamap);

            for (HashMap<String, String> s :
                    (ArrayList<HashMap<String, String>>) line_data.get("visited_stations")) {
                HashMap<String, String> postaja = new HashMap<>();
                postaja.put("label", s.get("time"));
                if (s.containsKey("wait")) {
                    postaja.put("data", s.get("station") + " (" + s.get("wait") + ")");
                } else {
                    postaja.put("data", s.get("station"));
                }
                childGroup.add(postaja);
            }

            adapter.notifyDataSetChanged();
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

    public void getLineDataFromAPI(String data, int i) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_podatki_relacija);
        request.put("method", "POST");
        request.put("data", "flags=" + data);
        request.put("group", Integer.toString(i));
        makeHttpRequest(request);
    }

/*    public void resumeFragment() {
        lv.setAdapter(adapter);
        lv.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (alreadyDownloadedLines.contains(groupPosition)) {
                    return;
                }
                HashMap<String, String> group = (HashMap<String, String>) adapter.getGroup(groupPosition);
                String req_data = group.get("line_data");
                getLineDataFromAPI(req_data, groupPosition);
                alreadyDownloadedLines.add(groupPosition);
            }
        });
    }*/

}
