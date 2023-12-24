// MainActivity.kt
package com.example.myapplication

import PostRequestAsync
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask

class MyAsyncTask : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg params: String): String {
        val url = URL(params[0])
        val postData = params[1].toByteArray(Charsets.UTF_8)

        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true

            val outputStream: OutputStream = connection.outputStream
            outputStream.write(postData)
            outputStream.flush()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                return response.toString()
            }
        } catch (e: Exception) {
            println("Error while sending POST request: ${e.toString()}")
            e.printStackTrace()
        }

        return "Error: Unable to perform the POST request."
    }

    override fun onPostExecute(result: String) {
        // Handle the result on the main thread (update UI, etc.)
        // Note: onPostExecute runs on the main thread.
    }
}

fun sendPostRequest(i : Int, newState : Int) {
    val endpoint = if(newState == 0) "turn-off"
    else "turn-on"
    val url = "http://192.168.1.111:80/$endpoint"
    val data = "{\"port_number\": $i}"
    val myAsyncTask = MyAsyncTask()
    myAsyncTask.execute(url, data)
}

class MainActivity : Activity() {
    private lateinit var timePicker: TimePicker
    private lateinit var btnEnable: Button
    private lateinit var btnDisable: Button
    private lateinit var btnEnableAll: Button
    private lateinit var btnDisableAll: Button
    private lateinit var listView: ListView
    private lateinit var alertDialog: AlertDialog
    private lateinit var checkboxTimer: CheckBox
    private lateinit var spinnerItemsArray: Array<String>

    private var selectedItems = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    private var isTimerOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnEnable = findViewById(R.id.btnEnable)
        btnDisable = findViewById(R.id.btnDisable)
        btnEnableAll = findViewById(R.id.btnEnableAll)
        btnDisableAll = findViewById(R.id.btnDisableAll)
        timePicker = findViewById(R.id.timePicker)
        checkboxTimer = findViewById(R.id.checkboxTimer)
        spinnerItemsArray = resources.getStringArray(R.array.spinner_items)

        // Initialize list view
        listView = ListView(this)
        val items = resources.getStringArray(R.array.spinner_items)
        val adapter = CustomAdapter(this, items)
        listView.adapter = adapter

        // Add click listener for the "Enable" button
        btnEnable.setOnClickListener {
            showListDialog(true)
        }

        // Add click listener for the "Disable" button
        btnDisable.setOnClickListener {
            showListDialog(false)
        }

        btnEnableAll.setOnClickListener {
            sendPostRequest(-1, 1)
            for (i in 0..7)
                selectedItems[i] = 1
        }

        btnDisableAll.setOnClickListener {
            sendPostRequest(-1, 0)
            for (i in 0..7)
                selectedItems[i] = 0
        }

        checkboxTimer.setOnClickListener {
            isTimerOn = !isTimerOn

            // Toggle the visibility of the TimePicker
            val timePickerVisibility = if (findViewById<CheckBox>(R.id.checkboxTimer).isChecked) View.VISIBLE else View.GONE
            timePicker.visibility = timePickerVisibility
        }

        val timer = Timer()
        // Schedule the task to run every second
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if(!findViewById<CheckBox>(R.id.checkboxTimer).isChecked)
                    return

                val targetHour = timePicker.hour
                val targetMinute = timePicker.minute
                val currentTime = Calendar.getInstance()

                // Check if the current time matches the target time
                if (currentTime.get(Calendar.HOUR_OF_DAY) == targetHour &&
                    currentTime.get(Calendar.MINUTE) == targetMinute) {
                    sendPostRequest(-1, 0)
                    for (i in 0..7)
                        selectedItems[i] = 0
                }
            }
        }, 0, 1000) // 0 milliseconds delay, 1000 milliseconds (1 second) interval
    }

    private fun showListDialog(isOnBtn : Boolean) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_list_dialog, null)

        val listViewContainer = dialogView.findViewById<LinearLayout>(R.id.listViewContainer)
        listViewContainer.removeAllViews()

        val items = spinnerItemsArray
        val adapter = CustomAdapter(this, items)

        for (i in 0 until adapter.count) {
            val listItemView = adapter.getView(i, isOnBtn, null, listViewContainer)
            listItemView.isClickable = true
            listViewContainer.addView(listItemView)
        }

        // Set a listener for the background view to close the dialog if clicked
        dialogView.findViewById<View>(R.id.backgroundView).setOnClickListener {
            alertDialog.dismiss()
        }

        val listViewVisibility = if (true || listViewContainer.isVisible) View.VISIBLE else View.GONE
        listViewContainer.visibility = listViewVisibility

        dialogBuilder.setView(dialogView)
        alertDialog = dialogBuilder.create()
        alertDialog.show()

    }

    inner class CustomAdapter(private val context: Context, private val items: Array<String>) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?) : View {
            val inflater = LayoutInflater.from(context)
            return inflater.inflate(R.layout.custom_list_item, parent, false)
        }

        fun getView(position: Int, isOnBtn: Boolean, convertView: View?, parent: ViewGroup?) : View {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.custom_list_item, parent, false)

            val textView = view.findViewById<TextView>(R.id.textView)
            val editButton = view.findViewById<ImageButton>(R.id.editButton)
            val editText = view.findViewById<EditText>(R.id.editText)

            // Set initial background color based on the item's state
            updateBackground(textView, selectedItems[position])

            // Set text
            textView.text = items[position]

            // Set click listener to toggle item state
            textView.setOnClickListener {
                val st = if(isOnBtn) 1 else 0

                selectedItems[position] = st
                updateBackground(textView, selectedItems[position])
                notifyDataSetChanged()

                sendPostRequest(position, st)
            }

            // Set click listener for the edit button
            editButton.setOnClickListener {
                // Toggle visibility of EditText when the edit button is clicked
                editText.visibility = if (editText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            // Set focus change listener for the EditText
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // When the EditText loses focus, set the name to its current text
                    textView.text = editText.text.toString()
                    spinnerItemsArray[position] = editText.text.toString()
                    editText.visibility = View.GONE
                }
            }

            return view
        }

        private fun updateBackground(textView: TextView, state: Int) {
            val bgColor = if (state == 1) R.color.yellow else R.color.white
            textView.setBackgroundResource(bgColor)
        }
    }
}
