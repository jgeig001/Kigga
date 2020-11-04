package com.jgeig001.kigga.model.persitence;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;

import com.jgeig001.kigga.MainActivity;
import com.jgeig001.kigga.model.domain.Bet;
import com.jgeig001.kigga.model.domain.Club;
import com.jgeig001.kigga.model.domain.History;
import com.jgeig001.kigga.model.domain.Liga;
import com.jgeig001.kigga.model.domain.Match;
import com.jgeig001.kigga.model.domain.ModelWrapper;
import com.jgeig001.kigga.model.domain.Season;
import com.jgeig001.kigga.model.domain.User;
import com.jgeig001.kigga.model.tasks.AsyncDataLoaderTask;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class PersistenceManager {

    private final String SERIALIZE_FILE = "serialized";
    private final int MAX_TIME_TO_WAIT = 60;

    private SharedPreferences preference;
    private SharedPreferences.Editor editor;
    private boolean first_init_done;
    private ModelWrapper model;

    public PersistenceManager(MainActivity context) throws JSONException, IOException {
        this.preference = context.getPreferences(Context.MODE_PRIVATE);
        editor = this.preference.edit();
        /*
         * to EDIT something:
         *           editor.putString("key", "value")
         *      /or/ editor.putBoolean("key", true)
         *      /or/ editor.putInt("key", 17)...
         *      editor.commit()
         */
        /*
         * to READ something:
         *           this.preference.getRepr("key", default)
         *      /or/ this.preference.pgetBoolean("key", default)
         *      /or/ this.preference.getInt("key", default)...
         */
        // first init
        /*this.first_init_done = this.preference.getBoolean("first_init_done", false);
        if (this.first_init_done == false) {
            // do first init stuff
            this.loadCurSeason();
            this.first_init_done = true;
        }*/

        // 1. load data from persitence
        try {
            this.model = this.loadSerializedModel(context);
        } catch (IOException | ClassNotFoundException e) {
            // nothing saved yet
            User user = new User("USER_NAME", new Club("NOCLUB", "NOCLUB"), new HashMap<Match, Bet>());
            Liga liga = new Liga();
            History history = new History(new ArrayList<Season>());
            this.model = new ModelWrapper(user, liga, history);
        }

        // 2. load new data from web

        // init ModelWrapper
        //this.model = this.createSomeTestingModel();

        // give historyObject to Loader and wait until Loader is finished
        AsyncDataLoaderTask task = new AsyncDataLoaderTask(this.model.getHistory());
        AsyncTask<URL, Integer, Object> r = task.execute();
        int time = 1;
        while (!((AsyncDataLoaderTask) r).isFinished()) {
            try {
                Thread.sleep(250);
                System.out.println("wait...");
                time += 1;
                if (time > MAX_TIME_TO_WAIT) {
                    throw new TimeoutException("");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException toe) {
                toe.printStackTrace();
            }
        }
    }

    /**
     * creates any model for developing and testing
     *
     * @return
     */
    private ModelWrapper createSomeTestingModel() {
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.RED);
        colors.add(0xFFFFFFFF);
        User user = new User("Mr_Dummy", new Club("Mainz05", "M05"), new HashMap<Match, Bet>());
        return this.model = new ModelWrapper(user, new Liga(), new History(new ArrayList<Season>()));
    }

    /**
     * method that deserialize model and returns it
     *
     * @param context
     * @return loaded model: ModelWrapper
     */
    private ModelWrapper loadSerializedModel(Context context) throws IOException, ClassNotFoundException {
        System.out.println("###loadSerializedModel()");
        FileInputStream fis = null;
        fis = context.openFileInput(SERIALIZE_FILE);
        ObjectInputStream is = new ObjectInputStream(fis);
        ArrayList lis = (ArrayList) is.readObject();
        is.close();
        fis.close();
        User user = (User) lis.get(0);
        Liga liga = (Liga) lis.get(1);
        History history = (History) lis.get(2);
        ModelWrapper model = new ModelWrapper(user, liga, history);
        return model;
    }

    /**
     * @param context
     */
    public void saveData(Context context) {
        System.out.println("###savedData()");
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(SERIALIZE_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            ArrayList lis = new ArrayList();
            lis.add(this.model.getUser());
            lis.add(this.model.getLiga());
            lis.add(this.model.getHistory());
            os.writeObject(lis);
            os.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ModelWrapper getModel() {
        return this.model;
    }

}
