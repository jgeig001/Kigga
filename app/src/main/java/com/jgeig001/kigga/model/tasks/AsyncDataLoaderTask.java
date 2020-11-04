package com.jgeig001.kigga.model.tasks;

import android.os.AsyncTask;

import com.jgeig001.kigga.model.domain.History;
import com.jgeig001.kigga.model.persitence.JSONLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class AsyncDataLoaderTask extends AsyncTask<URL, Integer, Object> {

    private boolean finished = false;
    private History history;

    public AsyncDataLoaderTask(History history) {
        this.history = history;
    }

    @Override
    protected Object doInBackground(URL... urls) {
        System.out.println("runs in background");

        JSONLoader loader = new JSONLoader(this.history);
        loader.updateData();

        this.onPostExecute(null);

        return null;
    }

    public boolean isFinished() {
        return this.finished;
    }

    /**
     * check if API works correctly
     */
    @Deprecated
    private void checkAPI() {
        try {
            URL url = new URL("https://www.openligadb.de/api/getmatchdata/bl1/2019/");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(15000);
            con.setReadTimeout(20000);
            int status = con.getResponseCode();
            System.out.println("status:");
            System.out.println(status);
            assert (status == 200);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                System.out.println("#" + inputLine);
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            System.out.println("\n#\n#\n" + content.toString() + "\n#\n#\n");
        } catch (IOException e) {
            System.out.println("\n\n\nERRRRRRROR:");
            e.printStackTrace();
        }
        this.onPostExecute(null);
    }


    @Override
    protected void onProgressUpdate(Integer... progress) {
        //setProgressPercent(progress[0]);
    }

    protected void onPostExecute(Long result) {
        //showDialog("Downloaded " + result + " bytes");
        this.finished = true;
    }

}