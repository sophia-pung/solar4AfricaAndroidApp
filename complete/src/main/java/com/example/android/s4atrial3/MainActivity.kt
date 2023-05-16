/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.s4atrial3;

import android.Manifest
import android.content.*
import android.view.View
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import java.io.File
import android.widget.EditText
import android.widget.Toast
import com.example.android.s4atrial3.ForegroundOnlyLocationService
import com.example.android.s4atrial3.SharedPreferenceUtil
import com.example.android.s4atrial3.toText
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.security.AccessControlContext
import java.security.AccessController.getContext
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates

private const val TAG = "MainActivity"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val FILENAME = "car_data.txt"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.

    private var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver? = null


    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var foregroundOnlyLocationButton: Button

    private var outputTextView: TextView by Delegates.notNull()


    private val REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Set up Firebase database emulator
        Firebase.database.useEmulator("10.0.2.2", 9000)


        outputTextView = findViewById(R.id.output_text_view)

        // Add click listener to the Begin Journey button
        val beginJourneyButton = findViewById<Button>(R.id.beginjourneybutton)
        beginJourneyButton.setOnClickListener {
            onBeginJourneyClick(it)
        }

        // Initialize shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // Set up listener for database updates
        val database = FirebaseDatabase.getInstance()
        val myRef = database.reference.child("users")
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // This method will be called whenever data is changed or added to the "users" child node in the database
                // Use snapshot.children to iterate over each child node, and extract relevant data as necessary
            }

            override fun onCancelled(error: DatabaseError) {
                // This method will be called if there is an error while trying to retrieve data from the database
            }
        })

        // Request permissions for writing to external storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
                Log.d("MyApp", "WRITE_EXTERNAL_STORAGE permission not granted. Requesting permission...")
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION)
            } else {
                Log.d("MyApp", "WRITE_EXTERNAL_STORAGE permission granted. Creating and writing to file...")
            }
        } else {
            Log.d("MyApp", "API level is less than 23. Creating and writing to file...")
        }

        // Initialize foreground location updates button
        foregroundOnlyLocationButton = findViewById<Button>(R.id.foreground_only_location_button)

        // Set click listener for foreground location updates button
        foregroundOnlyLocationButton.setOnClickListener {
            Log.d(TAG, "foregroundOnlyLocationButton clicked")
            val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
                if (foregroundPermissionApproved()) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                        ?: Log.d(TAG, "Service Not Bound")
                } else {
                    requestForegroundPermissions()
                }
            }
        }
    }


    fun onBeginJourneyClick(view: View?) {
        // Get the values of the name and vehicle ID inputs
        val nameInput = findViewById<EditText>(R.id.Nametextinput)
        val vehicleIDInput = findViewById<EditText>(R.id.VehicleIDtextinput)
        val name = nameInput.text.toString()
        val vehicleID = vehicleIDInput.text.toString()

        Log.d("MyApp", "beginJourney OnClickListener called test")

        // Construct the output string
        val output = "$name is beginning a journey in vehicle $vehicleID"
//
//        // Update the output text view with the output string
//        val outputTextView = findViewById<TextView>(R.id.output)
//        outputTextView.text = output

        // Write the output string to a file
        val fileName = "journey_${System.currentTimeMillis()}.txt"
        val fileContents = "Journey details: $output"
        applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }
//
//        // Write the name and vehicle ID to Firebase
//        val userRecord = mapOf(
//            "name" to name,
//            "vehicleID" to vehicleID
//        )
//        val database = Firebase.database
//        val myRef = database.getReference("users").push()
//        myRef.setValue(userRecord)

        // Display a success message to the user
        val toastMessage = "$name is starting journey in vehicle $vehicleID"
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d("MyApp", "onRequestPermissionsResult called")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, permissions, grantResults, "extraParam")
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray, extraParam: String) {
        Log.d("MyApp", "onRequestPermissionsResult with extraParam $extraParam called")

        when (requestCode) {
            REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MyApp", "WRITE_EXTERNAL_STORAGE permission granted. Creating and writing to file...")
                } else {
                    Log.d("MyApp", "WRITE_EXTERNAL_STORAGE permission not granted")
                    Toast.makeText(this, "Write permission was not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MyApp", "ONSTART() called")

        // Initialize ForegroundOnlyBroadcastReceiver
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver(null)
        foregroundOnlyBroadcastReceiver!!.start()

        updateButtonState(sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false))
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }


    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver!!,
            IntentFilter(ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        foregroundOnlyBroadcastReceiver!!.stop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(foregroundOnlyBroadcastReceiver!!)
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            updateButtonState(sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun writePermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    private fun requestWritePermissions() {
        val provideRationale = writePermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request write permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Handles permission result.

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            foregroundOnlyLocationButton.text = getString(R.string.stop_location_updates_button_text)
        } else {
            foregroundOnlyLocationButton.text = getString(R.string.start_location_updates_button_text)
        }
    }

//    private fun logResultsToScreen(output: String) {
//        val outputWithPreviousLogs = "$output\n${outputTextView.text}"
//        outputTextView.text = outputWithPreviousLogs
//    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver(private val location: Location?) : BroadcastReceiver() {
        var handler = Handler(Looper.getMainLooper())
        private var mRunnable: Runnable? = null

        private val broadcastRunnable = object : Runnable {
            override fun run() {
                // Schedule this runnable to run again after 10 seconds
                handler.postDelayed(this, 10000)

                val intent = Intent()
                onReceive(applicationContext, intent) // Call onReceive function with an empty Intent
            }
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            val location = intent?.getParcelableExtra<Location>(ForegroundOnlyLocationService.EXTRA_LOCATION)
            if (location != null) {
                // logResultsToScreen("Foreground location: ${location.toText()}")
                val tstamp = System.currentTimeMillis()
                // logResultsToScreen(tstamp.toString())

                File(applicationContext.filesDir, FILENAME).printWriter().use{ out ->
                    out.println("Location: ${location.toText()} Time: $tstamp")
                }
                Log.d("MyApp", "LOCATION WRITTEN SUCCESSFULLY")

                val latitude = location.latitude
                val longitude = location.longitude
                val accuracy = location.accuracy
                val locationText = "Latitude: $latitude\nLongitude: $longitude\nAccuracy: $accuracy meters"
                Log.i("MyApp", "New location:\n$locationText")
                Toast.makeText(this@MainActivity, locationText, Toast.LENGTH_SHORT).show()

                createAndWriteToFile(location) // Call createAndWriteToFile within ForegroundOnlyBroadcastReceiver with stored location
            }
        }

        // Start running the code every 10 seconds
        fun start() {
            Log.d("MyApp", "Starting ForegroundOnlyBroadcastReceiver")
            val filter = IntentFilter()
            filter.addAction(ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(this, filter)

            if (handler == null) {
                Log.d("MyApp", "Handler is null")
                handler = Handler(Looper.getMainLooper())
            }

            if (mRunnable == null) {
                Log.d("MyApp", "Runnable is null")
                mRunnable = broadcastRunnable
            }

            handler.postDelayed(mRunnable!!, 10000)
        }

        // Stop running the code periodically
        fun stop() {
            Log.d("MyApp", "Stopping ForegroundOnlyBroadcastReceiver")
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(this)
            mRunnable?.let { handler.removeCallbacks(it) }
        }
    }

    // Define createAndWriteToFile top-level function
    fun createAndWriteToFile(location: Location) {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val fileName = "car_coordinates_${dateFormat.format(date)}.txt"
        val relativePath = "Download/MyApp"
        val currentTimeInMillis = System.currentTimeMillis()

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }

        val resolver = applicationContext.contentResolver

        try {
            val queryUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(MediaStore.Downloads._ID) // Add _ID column to projection
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            val sortOrder = null

            val nameInput = findViewById<EditText>(R.id.Nametextinput)
            val vehicleIDInput = findViewById<EditText>(R.id.VehicleIDtextinput)
            val name = nameInput.text.toString()
            val vehicleID = vehicleIDInput.text.toString()

            val query = resolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

            if (query != null && query.moveToFirst()) {
                val columnIdIndex = query.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val fileId = query.getLong(columnIdIndex)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, fileId)

                resolver.openOutputStream(uri, "wa")?.use { outputStream ->
                    val fileContent = "Location: ${location.toText()} Time: $currentTimeInMillis\n"
                    outputStream.write(fileContent.toByteArray())
                    Log.d("MyApp", "File written to successfully $fileName, location: $location, time: $currentTimeInMillis")
                }
            } else {
                val newUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                newUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        val fileTitle = "Car Details || Driver: $name, CarID: $vehicleID\n"
                        outputStream.write(fileTitle.toByteArray())
                        val fileContent = "Location: ${location.toText()} Time: $currentTimeInMillis\n"
                        outputStream.write(fileContent.toByteArray())
                        Log.d("MyApp", "File not yet created, now created successfully $fileName, location: $location, time: $currentTimeInMillis")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MyApp", "Exception occurred: ${e.message}")
            e.printStackTrace()
        }
    }
}
