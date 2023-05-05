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
package com.example.android.whileinuselocation

import android.Manifest
import android.content.Context
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.s4atrial3.BuildConfig
import com.example.s4atrial3.R
import android.content.ContentValues
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileWriter
import java.net.FileNameMap
import android.os.Environment
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.FileOutputStream

private const val TAG = "MainActivity"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val FILENAME = "car_data.txt"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var foregroundOnlyLocationButton: Button

    private lateinit var outputTextView: TextView

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
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        // Set up Firebase database emulator
        Firebase.database.useEmulator("10.0.2.2", 9000)

        Log.d("MyApp", "test log message")

        var beginJourney = findViewById<Button>(R.id.beginjourneybutton)
        beginJourney.setOnClickListener {
            Log.d("MyApp", "beginJourney OnClickListener called")
            val fileName2 = "example_${System.currentTimeMillis()}.txt"
            var name: String = findViewById<EditText>(R.id.Nametextinput).text.toString()
            var vehicleID: String = findViewById<EditText>(R.id.VehicleIDtextinput).text.toString()
            if (BuildConfig.LOG_DEBUG) {
                Log.d("MyApp", "File was successfully stored")
                Log.d("MyApp", "File $fileName2 was successfully stored")
            }

            var output = "$name  $vehicleID"

            Log.d("TAG", "File was successfully stored")

            // Update the text of the TextView
            findViewById<TextView>(R.id.output).text = output

            Toast.makeText(this@MainActivity, output, Toast.LENGTH_LONG).show()
            Toast.makeText(this, "begin journey successful", Toast.LENGTH_LONG).show()
            Log.d("MyApp", "File successfully stored")

            // Write a message to the database
            val database = FirebaseDatabase.getInstance()
            val myRef = database.reference.child("users").push()
            myRef.child("name").setValue(name)
            myRef.child("vehicleID").setValue(vehicleID)

            // Log successful storage
            val fileName = "example_${System.currentTimeMillis()}.txt"
            Log.d("MyApp", "File $fileName was successfully stored")

            // Create and write to file
            createAndWriteToFile()
        }


        // Add listener to the database reference
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

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
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

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        foregroundOnlyLocationButton = findViewById(R.id.foreground_only_location_button)
        outputTextView = findViewById(R.id.output_text_view)

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

    fun createAndWriteToFile() {
        Log.d("MyApp", "createAndWriteToFile called")
        val fileName = "example_${System.currentTimeMillis()}.txt"
        val relativePath = "Download/MyApp"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }
        Log.d("ExampleApp", "Content values: $contentValues")
        Log.d("ExampleApp", "Downloads directory: ${Environment.DIRECTORY_DOWNLOADS}")

        val dir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir == null || !dir.exists() || !dir.isDirectory) {
            Log.e("MyApp", "Documents directory does not exist or is not a directory")
        }

        val resolver = applicationContext.contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val fileContent = "Test, Test, Test!"
                    outputStream.write(fileContent.toByteArray())
                    Log.d("MyApp", "File created and written to successfully")

                }
            }
        } catch (e: Exception) {
            Log.e("MyApp", "Exception occurred: ${e.message}")
            uri?.let { resolver.delete(it, null, null) }
            e.printStackTrace()
        }
    }


    override fun onStart() {
        super.onStart()

        updateButtonState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
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

    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs = "$output\n${outputTextView.text}"
        outputTextView.text = outputWithPreviousLogs
    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )

            if (location != null) {
                logResultsToScreen("Foreground location: ${location.toText()}")
                val tstamp = System.currentTimeMillis()
                logResultsToScreen(tstamp.toString())

                File(applicationContext.filesDir, FILENAME).printWriter().use{ out ->
                    out.println("Location: ${location.toText()} Time: $tstamp")
                }
                //var file = File(FILENAME)
                //file.createNewFile()
                //File(FILENAME).appendText("Location: ${location.toText()} Time: $tstamp")
            }
        }
    }
}
