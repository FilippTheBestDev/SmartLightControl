import android.os.AsyncTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class PostRequestAsync() : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg params: String?): String {
        val urlString = params[0]
        val requestMethod = params[1]
        val requestBody = params[2]

        return try {
            sendPostRequest(urlString, requestMethod, requestBody)
        } catch (e: IOException) {
            "Error: ${e.message}"
        }
    }

    private fun sendPostRequest(urlString: String?, requestMethod: String?, requestBody: String?): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        // Set request method and headers
        connection.requestMethod = requestMethod
        connection.setRequestProperty("Content-Type", "application/json")

        // Enable input/output streams
        connection.doOutput = true

        // Write request body
        val outputStream: OutputStream = connection.outputStream
        outputStream.write(requestBody?.toByteArray())

        // Get response
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }

        // Close resources
        outputStream.close()
        reader.close()

        return response.toString()
    }
}
