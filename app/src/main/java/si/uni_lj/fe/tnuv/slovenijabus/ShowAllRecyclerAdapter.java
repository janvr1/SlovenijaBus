package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ShowAllRecyclerAdapter extends RecyclerView.Adapter<ShowAllRecyclerAdapter.showAllViewHolder> implements DownloadCallback {
    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request
    private ArrayList<HashMap<String, String>> mGroupData;
    private int mGroupLayout, mChildLayout, mFirstChildLayout, mIndex;
    private String[] mGroupFrom, mFirstChildFrom, mChildFrom;
    private int[] mGroupTo, mChildTo, mFirstChildTo;
    private LayoutInflater mInflater;
    private Context mContext;
    private int expired_color, defaultChildTextColor, timetableTextColor;
    private List<LinearLayout> mChildLayouts = new ArrayList<>();
    private RecyclerView recyclerView;

    public ShowAllRecyclerAdapter(Context context,
                                  ArrayList<HashMap<String, String>> groupData, int groupLayout,
                                  String[] groupFrom, int[] groupTo,
                                  int childLayout, int firstChildLayout, String[] childFrom,
                                  int[] childTo, String[] firstChildFrom, int[] firstChildTo, int index, RecyclerView rv) {
        mGroupData = groupData;
        mGroupLayout = groupLayout;
        mGroupFrom = groupFrom;
        mGroupTo = groupTo;
        mChildLayout = childLayout;
        mFirstChildLayout = firstChildLayout;
        mChildFrom = childFrom;
        mChildTo = childTo;
        mFirstChildFrom = firstChildFrom;
        mFirstChildTo = firstChildTo;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        recyclerView = rv;
        mContext = context;
        mIndex = index;
        expired_color = mContext.getColor(R.color.expiredColor);
        timetableTextColor = mContext.getColor(R.color.timeTableTextColor);
        defaultChildTextColor = mContext.getColor(android.R.color.secondary_text_light);


        recyclerView.getItemAnimator().setChangeDuration(500);
        recyclerView.getItemAnimator().setMoveDuration(350);
        recyclerView.getItemAnimator().setAddDuration(350);
        recyclerView.getItemAnimator().setRemoveDuration(350);

        for (int i = 0; i < mGroupData.size(); i++) {
            mChildLayouts.add(null);
        }
    }

    public showAllViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(mGroupLayout,
                viewGroup, false);
        return new showAllViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final showAllViewHolder viewHolder, final int i) {
        HashMap<String, String> group = mGroupData.get(i);

        LinearLayout childLayout = mChildLayouts.get(i);
        ConstraintLayout groupLayout = viewHolder.groupLayout;
        if (i < mIndex) {
            setAllTextColor(groupLayout, expired_color);
            viewHolder.indicator.setImageTintList(ColorStateList.valueOf(expired_color));
        } else {
            setTextColors(groupLayout, viewHolder.defaultGroupTextColors);
            viewHolder.indicator.setImageTintList(ColorStateList.valueOf(timetableTextColor));
        }

        bindData(groupLayout, group, mGroupFrom, mGroupTo);

        if (childLayout == null && viewHolder.parentLayout.findViewById(R.id.childLinearLayout) != null) {
            viewHolder.parentLayout.removeView(viewHolder.parentLayout.findViewById(R.id.childLinearLayout));
        }

        if (viewHolder.parentLayout.findViewById(R.id.childLinearLayout) != childLayout &&
                childLayout != null) {
            viewHolder.parentLayout.removeView(viewHolder.parentLayout.findViewById(R.id.childLinearLayout));
            if (childLayout.getParent() != null) {
                ((ViewGroup) childLayout.getParent()).removeView(childLayout);
            }
            viewHolder.parentLayout.addView(childLayout);
        }

    }

    private void bindData(View view, HashMap<String, String> data, String[] fromArray, int[] toArray) {
        for (int i = 0; i < fromArray.length; i++) {
            TextView tv = view.findViewById(toArray[i]);
            tv.setText(data.get(fromArray[i]));
        }
    }

    @Override
    public int getItemCount() {
        return mGroupData.size();
    }

    @Override
    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");
        String result_string = (String) result.get("response");

        if (request.get("url").equals(API_podatki_relacija)) { // trenutno disablan
            int groupPosition = Integer.parseInt(request.get("group"));

            if (result_string.equals("error")) {
                Toast.makeText(mContext.getApplicationContext(), R.string.network_error_message, Toast.LENGTH_LONG).show();
                return;
            }

            ArrayList<HashMap<String, String>> line_data = lineDataParser2(result_string);

            LinearLayout childLinearLayout = new LinearLayout(mContext);
            childLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            childLinearLayout.setOrientation(LinearLayout.VERTICAL);
            childLinearLayout.setId(R.id.childLinearLayout);
            childLinearLayout.setPadding(0, 0, 0, 32);
            childLinearLayout.setVisibility(View.GONE);

            for (int i = 0; i < line_data.size(); i++) {
                ConstraintLayout child;
                HashMap<String, String> hm = line_data.get(i);
                if (i == 0) {
                    child = (ConstraintLayout) newChildView(true);
                    bindData(child, hm, mFirstChildFrom, mFirstChildTo);
                } else {
                    child = (ConstraintLayout) newChildView(false);
                    bindData(child, hm, mChildFrom, mChildTo);
                }
                if (groupPosition < mIndex) {
                    setAllTextColor(child, expired_color);
                } else {
                    setAllTextColor(child, defaultChildTextColor);
                }
                childLinearLayout.addView(child);
            }
            mChildLayouts.set(groupPosition, childLinearLayout);
            showAllViewHolder viewHolder = (showAllViewHolder) recyclerView.findViewHolderForLayoutPosition(groupPosition);
            viewHolder.parentLayout.addView(mChildLayouts.get(groupPosition));

            mChildLayouts.get(groupPosition).setVisibility(View.VISIBLE);
            //viewHolder.indicator.setImageResource(R.drawable.arrow_down_black);
            viewHolder.indicator.animate().rotationBy(90);
            recyclerView.scrollToPosition(groupPosition);
            notifyItemChanged(groupPosition, mChildLayouts.get(groupPosition));
        }
    }

    public View newChildView(boolean isFirstChild) {
        return mInflater.inflate((isFirstChild) ? mFirstChildLayout : mChildLayout, null);
    }

    public void getLineDataFromAPI(String data, int index) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_podatki_relacija);
        request.put("method", "POST");
        request.put("data", "flags=" + data);
        request.put("group", Integer.toString(index));

        makeHttpRequest(request);
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


    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {

    }

    @Override
    public void finishDownloading() {

    }

    public class showAllViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout groupLayout;
        LinearLayout parentLayout;
        List<Integer> defaultGroupTextColors;
        ImageView indicator;

        public showAllViewHolder(View view) {
            super(view);
            groupLayout = view.findViewById(R.id.show_all_group_viewgroup);
            parentLayout = view.findViewById(R.id.show_all_parentLinearLayout);
            defaultGroupTextColors = getTextColors(groupLayout);
            indicator = view.findViewById(R.id.expanded_indicator);

            groupLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = getLayoutPosition();
                    if (mChildLayouts.get(i) == null) {
                        String request_data = mGroupData.get(i).get("line_data");
                        getLineDataFromAPI(request_data, i);
                    } else {
                        if (mChildLayouts.get(i).getVisibility() == View.GONE) {
                            mChildLayouts.get(i).setVisibility(View.VISIBLE);

                            //indicator.setImageResource(R.drawable.arrow_down_black);
                            indicator.animate().rotationBy(90).setDuration(350);
                            recyclerView.scrollToPosition(i);
                            notifyItemChanged(i, mChildLayouts.get(i));
                            Log.d("showallrecycler", "view set to visible");
                        } else {
                            mChildLayouts.get(i).setVisibility(View.GONE);
                            //indicator.setImageResource(R.drawable.arrow_right_black);
                            indicator.animate().rotationBy(-90).setDuration(350);
                            notifyItemChanged(i, mChildLayouts.get(i));
                        }
                    }

                }
            });
        }
    }

    public void makeHttpRequest(HashMap<String, String> request) {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask(this).execute(request);
        } else {
            Toast.makeText(mContext.getApplicationContext(), R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void setAllTextColor(ViewGroup vg, int color) {
        int child_num = vg.getChildCount();
        for (int i = 0; i < child_num; i++) {
            View v = vg.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(color);
            }
        }
    }

    private List<Integer> getTextColors(ViewGroup vg) {
        int child_num = vg.getChildCount();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < child_num; i++) {
            View v = vg.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                colors.add(tv.getCurrentTextColor());
            }
        }
        return colors;
    }

    private void setTextColors(ViewGroup vg, List<Integer> clrs) {
        int child_num = vg.getChildCount();
        for (int i = 0, j = 0; i < child_num; i++) {
            View v = vg.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(clrs.get(j));
                j++;
            }
        }
    }
}
