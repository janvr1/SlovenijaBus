package si.uni_lj.fe.tnuv.slovenijabus;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;
import android.widget.TextView;

import java.util.Calendar;

public class SetDateFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar calendar = Calendar.getInstance();
        int yy = calendar.get(Calendar.YEAR);
        int mm = calendar.get(Calendar.MONTH);
        int dd = calendar.get(Calendar.DAY_OF_MONTH);
        return new DatePickerDialog(getActivity(), this, yy, mm, dd);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        TextView tv = getActivity().findViewById(R.id.datum_vnos);
        tv.setText(dateStringBuilder(year, month, dayOfMonth));
    }

    private String dateStringBuilder(int year, int month, int day) {
        month++; // da je pravilen mesec :)
        return day + "." + month + "." + year;
    }
}
