package si.uni_lj.fe.tnuv.slovenijabus;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class DownloadAsyncTask extends AsyncTask<HashMap<String, String>, Void, HashMap<String, Object>> {

    private static final String DEBUG_TAG = DownloadAsyncTask.class.getSimpleName();

    private DownloadCallback caller;

    DownloadAsyncTask(DownloadCallback caller) {
        this.caller = caller;
    }

    DownloadAsyncTask() {
    }

    @Override
    protected HashMap<String, Object> doInBackground(HashMap<String, String>... in) {
        HashMap input = in[0];
        try {
            return downloadUrl(input);
        } catch (IOException e) {
            HashMap<String, Object> error = new HashMap<>();
            error.put("response", "error");
            error.put("request", input);
            return error;
        }
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(HashMap<String, Object> result) {
        caller.updateFromDownload(result);
    }

    private HashMap<String, Object> downloadUrl(HashMap<String, String> input) throws IOException {
        InputStream inputStream = null;

        try {
            URL url = new URL(input.get("url"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000 /* milliseconds */);
            conn.setConnectTimeout(5000 /* milliseconds */);
            conn.setRequestMethod(input.get("method"));
            conn.setDoInput(true);
            if (input.get("method").equals("POST")) {
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(input.get("data"));
            }
            // Starts the query
            conn.connect();

            //cakamo na odziv (zaradi tega cakanja je potrebna locena nit!)
            int response = conn.getResponseCode();

            inputStream = new BufferedInputStream(conn.getInputStream());

            // Convert the InputStream into a string
            String contentAsString = convertStreamToString(inputStream);

            HashMap<String, Object> result = new HashMap();
            result.put("response", contentAsString);
            result.put("request", input);
            result.put("response_code", response);

            return result;

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}