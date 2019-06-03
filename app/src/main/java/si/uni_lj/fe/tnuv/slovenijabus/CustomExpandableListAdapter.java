package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {
    private Context mContext;

    private List<? extends Map<String, ?>> mGroupData;
    private int mGroupLayout;
    private String[] mGroupFrom;
    private int[] mGroupTo;

    private List<? extends List<? extends Map<String, ?>>> mChildData;
    private int mChildLayout;
    private int mFirstChildLayout;
    private String[] mChildFrom;
    private int[] mChildTo;
    private String[] mFirstChildFrom;
    private int[] mFirstChildTo;

    private int mIndex;
    private int expired_color;
    private int defaultChildTextColor;
    private List<Integer> defaultGroupTextColors;

    private LayoutInflater mInflater;

    public CustomExpandableListAdapter(Context context,
                                       List<? extends Map<String, ?>> groupData, int groupLayout,
                                       String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData,
                                       int childLayout, int firstChildLayout, String[] childFrom,
                                       int[] childTo, String[] firstChildFrom, int[] firstChildTo, int index) {
        mGroupData = groupData;
        mGroupLayout = groupLayout;
        mGroupFrom = groupFrom;
        mGroupTo = groupTo;

        mChildData = childData;
        mChildLayout = childLayout;
        mFirstChildLayout = firstChildLayout;
        mChildFrom = childFrom;
        mChildTo = childTo;
        mFirstChildFrom = firstChildFrom;
        mFirstChildTo = firstChildTo;

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mContext = context;
        mIndex = index;
        expired_color = mContext.getColor(R.color.expiredColor);
        defaultChildTextColor = mContext.getColor(android.R.color.secondary_text_light);
    }

    public Object getChild(int groupPosition, int childPosition) {
        return mChildData.get(groupPosition).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        ViewGroup vg;
        boolean firstChild;
        firstChild = childPosition == 0;
        if (convertView == null) {
            vg = (ViewGroup) newChildView(firstChild, parent);
        } else {
            vg = (ViewGroup) convertView;
            if (firstChild) {
                if (vg.getId() != R.id.dropdown_list_first_item) {
                    vg = (ViewGroup) newChildView(firstChild, parent);
                }

            } else {
                if (vg.getId() != R.id.dropdown_list_item) {
                    vg = (ViewGroup) newChildView(firstChild, parent);
                }
            }
        }
        //ViewGroup vg2 = (ViewGroup) vg.getChildAt(0);
        //defaultChildTextColor = ((TextView) vg.getChildAt(0)).getCurrentTextColor();
        if (groupPosition < mIndex) {
            setAllTextColor(vg, expired_color, -1);
        } else {
            setAllTextColor(vg, defaultChildTextColor, -1);
        }
        if (firstChild) {
            bindView(vg, mChildData.get(groupPosition).get(childPosition), mFirstChildFrom, mFirstChildTo);
        } else {
            bindView(vg, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
        }
        return vg;
    }


    public View newChildView(boolean isFirstChild, ViewGroup parent) {
        return mInflater.inflate((isFirstChild) ? mFirstChildLayout : mChildLayout, parent, false);
    }

    private void bindView(View view, Map<String, ?> data, String[] from, int[] to) {
        int len = to.length;

        for (int i = 0; i < len; i++) {
            TextView v = view.findViewById(to[i]);
            if (v != null) {
                v.setText((String) data.get(from[i]));
            }
        }
    }

    public int getChildrenCount(int groupPosition) {
        return mChildData.get(groupPosition).size();
    }

    public Object getGroup(int groupPosition) {
        return mGroupData.get(groupPosition);
    }

    public int getGroupCount() {
        return mGroupData.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        ViewGroup vg;
        if (convertView == null) {
            vg = (ViewGroup) newGroupView(isExpanded, parent);
            defaultGroupTextColors = getTextColors(vg, R.id.show_all_group_viewgroup);
            if (groupPosition < mIndex) {
                setAllTextColor(vg, expired_color, R.id.show_all_group_viewgroup);
            }
        } else {
            vg = (ViewGroup) convertView;
            if (groupPosition < mIndex) {
                setAllTextColor(vg, expired_color, R.id.show_all_group_viewgroup);
            } else {
                setTextColors(vg, defaultGroupTextColors, R.id.show_all_group_viewgroup);
            }
        }
        bindView(vg, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
        return vg;
    }

    /**
     * Instantiates a new View for a group.
     *
     * @param isExpanded Whether the group is currently expanded.
     * @param parent     The eventual parent of this new View.
     * @return A new group View
     */
    public View newGroupView(boolean isExpanded, ViewGroup parent) {
        return mInflater.inflate(mGroupLayout, parent, false);
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

    private void setAllTextColor(ViewGroup vg, int color, int viewGroupID) {
        ViewGroup parent;
        if (viewGroupID == -1) {
            parent = vg;
        } else {
            parent = vg.findViewById(viewGroupID);
        }
        int child_num = parent.getChildCount();
        for (int i = 0; i < child_num; i++) {
            try {
                TextView tv = (TextView) parent.getChildAt(i);
                tv.setTextColor(color);
            } catch (Exception e) {
            }
        }
    }

    private List<Integer> getTextColors(ViewGroup vg, int viewGroupID) {
        ViewGroup parent = vg.findViewById(viewGroupID);
        int child_num = parent.getChildCount();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < child_num; i++) {
            View v = parent.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) parent.getChildAt(i);
                colors.add(tv.getCurrentTextColor());
            }
        }
        return colors;
    }

    private void setTextColors(ViewGroup vg, List<Integer> clrs, int viewGroupID) {
        ViewGroup parent = vg.findViewById(viewGroupID);
        int child_num = parent.getChildCount();
        for (int i = 0, j = 0; i < child_num; i++) {
            View v = parent.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) parent.getChildAt(i);
                tv.setTextColor(clrs.get(j));
                j++;
            }
        }
    }

}
