package com.jgeig001.kigga.model.persitence

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.jgeig001.kigga.model.domain.*
import java.io.*
import java.lang.reflect.Field
import java.util.*
import javax.inject.Inject

class PersistenceManager @Inject constructor(context: Context) {

    private val SERIALIZE_FILE = "serialized"
    private val MAX_TIME_TO_WAIT = 60
    private val first_init_done = false
    var model: ModelWrapper

    init {
        // 1. load data from persitence
        try {
            Log.d("123", "load data from local storage")
            model = loadSerializedModel(context)
            Log.d("123", "load data from local storage: successful")
        } catch (e: IOException) {
            // nothing saved yet
            val user = User("USER_NAME", Club("NOCLUB", "NOCLUB"))
            val liga = Liga()
            val history = History(mutableListOf())
            model = ModelWrapper(user, liga, history)
            Log.d("123", "load data from local storage: failed")
        } catch (e: ClassNotFoundException) {
            val user = User("USER_NAME", Club("NOCLUB", "NOCLUB"))
            val liga = Liga()
            val history = History(mutableListOf())
            model = ModelWrapper(user, liga, history)
            Log.d("123", "load data from local storage: failed")
        }
        // 2. load new data from web
        Log.d("123", "load data from web")
        val dataPoller = DataPoller(model.getHistory())
        dataPoller.poll()
        Log.d("123", "go on")
        // TODO: check: if loading takes too long. will view notice model change ?
    }

    /**
     * creates any model for developing and testing
     *
     * @return dummy modelWrapper
     */
    private fun createSomeTestingModel(): ModelWrapper {
        val colors = mutableListOf<Int>()
        colors.add(Color.RED)
        colors.add(-0x1)
        val user = User("Mr_Dummy", Club("Mainz05", "M05"))
        return ModelWrapper(user, Liga(), History(mutableListOf())).also { model = it }
    }

    /**
     * method that deserialize model and returns it
     *
     * @param context
     * @return loaded model: ModelWrapper
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun loadSerializedModel(context: Context): ModelWrapper {
        var fis: FileInputStream? = null
        fis = context.openFileInput(SERIALIZE_FILE)
        val inputStream = ObjectInputStream(fis)
        val lis = inputStream.readObject() as ArrayList<*>
        inputStream.close()
        fis.close()
        val user = lis[0] as User
        val liga = lis[1] as Liga
        val history = lis[2] as History
        return ModelWrapper(user, liga, history)
    }

    /**
     * Saves the data with java serialization.
     * @param context
     */
    fun saveData(context: Context) {
        println("###savedData()")
        try {
            val fos: FileOutputStream = context.openFileOutput(SERIALIZE_FILE, Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            val lis = mutableListOf<Any>()
            lis.add(model.getUser())
            lis.add(model.getLiga())
            lis.add(model.getHistory())
            os.writeObject(lis)
            os.close()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

}