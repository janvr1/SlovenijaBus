/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An easy adapter to map static data to group and child views defined in an XML
 * file. You can separately specify the data backing the group as a List of
 * Maps. Each entry in the ArrayList corresponds to one group in the expandable
 * list. The Maps contain the data for each row. You also specify an XML file
 * that defines the views used to display a group, and a mapping from keys in
 * the Map to specific views. This process is similar for a child, except it is
 * one-level deeper so the data backing is specified as a List<List<Map>>,
 * where the first List corresponds to the group of the child, the second List
 * corresponds to the position of the child within the group, and finally the
 * Map holds the data for that particular child.
 */
public class CustomExpandableListAdapter extends BaseExpandableListAdapter {
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

        Context mContext = context;
        mIndex = index;
        expired_color = mContext.getColor(R.color.expiredColor);
    }

    public Object getChild(int groupPosition, int childPosition) {
        return mChildData.get(groupPosition).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {

        ConstraintLayout cl;
        boolean firstChild;
        if (childPosition == 0) {
            firstChild = true;
        } else {
            firstChild = false;
        }
        cl = (ConstraintLayout) newChildView(firstChild, parent);
        defaultChildTextColor = ((TextView) cl.getChildAt(0)).getCurrentTextColor();
        if (groupPosition < mIndex) {
            setAllTextColor(cl, expired_color);
        } else {
            setAllTextColor(cl, defaultChildTextColor);
        }

/*        } else {
            cl = (ConstraintLayout) convertView;
            if (groupPosition < mIndex) {
                setAllTextColor(cl, expired_color);
            } else {
                setAllTextColor(cl, defaultChildTextColor);
            }*/
        if (firstChild) {
            bindView(cl, mChildData.get(groupPosition).get(childPosition), mFirstChildFrom, mFirstChildTo);
        } else {
            bindView(cl, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
        }

        return cl;
    }

    /**
     * Instantiates a new View for a child.
     *
     * @param isLastChild Whether the child is the last child within its group.
     * @param parent      The eventual parent of this new View.
     * @return A new child View
     */
    public View newChildView(boolean isFirstChild, ViewGroup parent) {
        return mInflater.inflate((isFirstChild) ? mFirstChildLayout : mChildLayout, parent, false);
    }

    private void bindView(View view, Map<String, ?> data, String[] from, int[] to) {
        int len = to.length;

        for (int i = 0; i < len; i++) {
            TextView v = (TextView) view.findViewById(to[i]);
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
        View v;
        ConstraintLayout cl;
        if (convertView == null) {
            cl = (ConstraintLayout) newGroupView(isExpanded, parent);
            defaultGroupTextColors = getTextColors(cl);
            if (groupPosition < mIndex) {
                setAllTextColor(cl, expired_color);
            }
        } else {
            cl = (ConstraintLayout) convertView;
            if (groupPosition < mIndex) {
                setAllTextColor(cl, expired_color);
            } else {
                setTextColors(cl, defaultGroupTextColors);
            }
        }
        Log.d("adapter_group_position", Integer.toString(groupPosition));
        Log.d("adapter_index", Integer.toString(mIndex));
        bindView(cl, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
        return cl;
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

    private void setAllTextColor(ViewGroup vg, int color) {
        int child_num = vg.getChildCount();
        for (int i = 0; i < child_num; i++) {
            try {
                TextView tv = (TextView) vg.getChildAt(i);
                tv.setTextColor(color);
            } catch (Exception e) {
            }
        }
    }

    private List<Integer> getTextColors(ViewGroup vg) {
        int child_num = vg.getChildCount();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < child_num; i++) {
            View v = vg.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) vg.getChildAt(i);
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
                TextView tv = (TextView) vg.getChildAt(i);
                tv.setTextColor(clrs.get(j));
                j++;
            }
        }

    }

}
