package si.uni_lj.fe.tnuv.slovenijabus;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadAsyncTask_POST extends AsyncTask<String, Void, String> {
    private static final String DEBUG_TAG = DownloadAsyncTask_POST.class.getSimpleName();

    private DownloadCallback caller;
    private String request_data;

    // Tole je konstruktor. Ob ustvarjanju objekta iz tega razreda mora klicatelj (uporabnik tega razreda)
    // posredovati sklic nase, tako da ga lahko ta razred obvešča o napredku.
    // Klicatelj je lahko vsak razred, ki deduje (izdela) vmesnik DownloadCallback

    DownloadAsyncTask_POST(DownloadCallback caller) {
        this.caller = caller;
    }

    @Override
    protected String doInBackground(String... urls) {
        request_data = urls[1];
        // params comes from the execute() call: params[0] is the url.
        try {
            return downloadUrl(urls[0]);
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
        caller.updateFromDownload(result);
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream inputStream = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoInput(true);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(request_data);
            // Starts the query
            conn.connect();

            //cakamo na odziv (zaradi tega cakanja je potrebna locena nit!)
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);

            inputStream = new BufferedInputStream(conn.getInputStream());

            // Convert the InputStream into a string
            String contentAsString = convertStreamToString(inputStream);
            Log.d(DEBUG_TAG, "Vsebina: " + contentAsString);
            return contentAsString;
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