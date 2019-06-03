package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

import static si.uni_lj.fe.tnuv.slovenijabus.MainActivity.EXTRA_DATE;
import static si.uni_lj.fe.tnuv.slovenijabus.MainActivity.EXTRA_ENTRY;
import static si.uni_lj.fe.tnuv.slovenijabus.MainActivity.EXTRA_EXIT;

public class favoritesRecyclerAdapter extends RecyclerView.Adapter<favoritesRecyclerAdapter.MyViewHolder> {
    private List<HashMap<String, String>> mDataSet;
    private Context mContext;
    private TextView mDateView;
    DatabaseHelper slovenijabus_DB;


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.favorites_list_item,
                viewGroup, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder viewHolder, final int i) {
        HashMap<String, String> favorite = mDataSet.get(i);
        viewHolder.from.setText(favorite.get("entry"));
        viewHolder.to.setText(favorite.get("exit"));

        if (favorite.containsKey("first")) {
            viewHolder.first.setText(favorite.get("first"));
            viewHolder.first.setVisibility(View.VISIBLE);
        } else {
            viewHolder.first.setVisibility(View.GONE);
        }
        if (favorite.containsKey("second")) {
            viewHolder.second.setText(favorite.get("second"));
            viewHolder.second.setVisibility(View.VISIBLE);
        } else {
            viewHolder.second.setVisibility(View.GONE);
        }
        if (favorite.containsKey("third")) {
            viewHolder.third.setText(favorite.get("third"));
            viewHolder.third.setVisibility(View.VISIBLE);
        } else {
            viewHolder.third.setVisibility(View.GONE);
        }
        viewHolder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper slovenijabus_DB = DatabaseHelper.getInstance(v.getContext());
                String entryID = slovenijabus_DB.getStationIDFromName(viewHolder.from.getText().toString());
                String exitID = slovenijabus_DB.getStationIDFromName(viewHolder.to.getText().toString());

                Intent intent = new Intent(mContext, showAllActivity.class);

                intent.putExtra(EXTRA_ENTRY, entryID);
                intent.putExtra(EXTRA_EXIT, exitID);
                intent.putExtra(EXTRA_DATE, mDateView.getText().toString());
                mContext.startActivity(intent);
            }
        });
        viewHolder.parent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ;
                slovenijabus_DB.removeFavoriteFromIndex(i);
                mDataSet.remove(i);
                notifyItemRemoved(i);
                Toast.makeText(mContext, R.string.remove_from_favorites, Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }

    public favoritesRecyclerAdapter(Context context, List<HashMap<String, String>> data, TextView dateView) {
        mContext = context;
        mDataSet = data;
        mDateView = dateView;
        slovenijabus_DB = new DatabaseHelper(mContext);
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView from, to, first, second, third;
        int pos;
        public View parent;

        public MyViewHolder(View view) {
            super(view);
            from = view.findViewById(R.id.favorites_from);
            to = view.findViewById(R.id.favorites_to);
            first = view.findViewById(R.id.first_bus);
            second = view.findViewById(R.id.second_bus);
            third = view.findViewById(R.id.third_bus);
            parent = view;
            pos = getLayoutPosition();
            //pos = getAdapterPosition();
        }
    }
}
