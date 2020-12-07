package com.jgeig001.kigga.model.persitence

import org.json.JSONArray
import org.json.JSONException
import java.io.*
import java.net.URL
import java.nio.charset.Charset

object JSON_Reader {

    /**
     * helper function
     *
     * @param rd
     * @return content as String
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readAll(rd: Reader): String {
        val sb = StringBuilder()
        var cp: Int
        while (rd.read().also { cp = it } != -1) {
            sb.append(cp.toChar())
        }
        return sb.toString()
    }

    /**
     * Gets the JSON from the url and returns it as JSONArray
     *
     * @param url
     * @return JSONArray with all data
     * @throws IOException
     * @throws JSONException
     */
    @Throws(JSONException::class, IOException::class)
    fun readJsonFromUrl(url: String?): JSONArray? {
        var inputStream: InputStream? = null
        return try {
            val connection = URL(url).openConnection()
            connection.readTimeout = 30000 // 30 sec
            inputStream = connection.getInputStream()
            val rd = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            val jsonText: String = readAll(rd)
            val json = JSONArray(jsonText)
            inputStream.close()
            json
        } catch (e: IOException) {
            null
        } finally {
            inputStream?.close()
        }
    }

}