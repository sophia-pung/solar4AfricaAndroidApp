package com.example.s4atrial3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        // Set up Firebase database emulator
        Firebase.database.useEmulator("10.0.2.2", 9000)

        var beginjourney = findViewById<Button>(R.id.beginjourneybutton)
        beginjourney.setOnClickListener {
            var name: String = findViewById<EditText>(R.id.Nametextinput).text.toString()
            var vehicleID: String = findViewById<EditText>(R.id.VehicleIDtextinput).text.toString()
            var GPSCoordinates: String = findViewById<EditText>(R.id.GPSCoordinates).text.toString()

            var output = "$name  $vehicleID $GPSCoordinates"
            Toast.makeText(this@MainActivity, output, LENGTH_LONG).show()

            // Write a message to the database
            val database = FirebaseDatabase.getInstance()
            val myRef = database.reference.child("users").push()
            myRef.child("name").setValue(name)
            myRef.child("vehicleID").setValue(vehicleID)
            myRef.child("GPSCoordinates").setValue(GPSCoordinates)
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
    }

}