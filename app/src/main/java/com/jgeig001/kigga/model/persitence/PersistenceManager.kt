package com.jgeig001.kigga.model.persitence

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.jgeig001.kigga.model.domain.*
import kotlinx.coroutines.Job
import java.io.*
import java.lang.reflect.Field
import java.util.*
import javax.inject.Inject

class PersistenceManager @Inject constructor(context: Context) {

    private val SERIALIZE_FILE = "serialized"
    private val MAX_TIME_TO_WAIT = 60
    private val first_init_done = false
    private var model: ModelWrapper
    private var dataPoller: DataPoller

    init {
        // 1. load data from persitence
        try {
            model = loadSerializedModel(context)
        } catch (e: FileNotFoundException) {
            // nothing saved yet
            val user = User("USER_NAME", null)
            val liga = Liga()
            val history = History(mutableListOf())
            model = ModelWrapper(user, liga, history)
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            val user = User("USER_NAME", null)
            val liga = Liga()
            val history = History(mutableListOf())
            model = ModelWrapper(user, liga, history)
            e.printStackTrace()
        }
        // 2. load new data from web
        dataPoller = DataPoller(model.getHistory())
        dataPoller.firstLoadFinishedCallback { this.saveData(context) }
        dataPoller.poll()
        // TODO: check stability
    }

    fun getLoadedModel(): ModelWrapper {
        return this.model
    }

    /**
     * method that deserialize model and returns it
     * @param context
     * @return loaded model: ModelWrapper
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun loadSerializedModel(context: Context): ModelWrapper {
        println("###loadSerializedModel()")
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

    fun firstLoadFinishedCallback(callback: () -> Unit) {
        dataPoller.firstLoadFinishedCallback(callback)
    }

}