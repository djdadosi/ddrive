package si.djdado.ddrive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.location.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import si.djdado.ddrive.databinding.ActivityMainBinding
import java.time.Instant
import java.time.format.DateTimeFormatter

class  MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref: SharedPreferences;
    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var accelerationListener: SensorEventListener
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var driving = false;
    private var driveId = ""

    private var lastLocation: Location? = null

    private var shocks = arrayOf<Float>( 0.0f, 0.0f, 0.0f )

    private lateinit var driveAdapter: DriveAdapter

    fun initialize() {
        //https://stackoverflow.com/questions/66489605/is-constructor-locationrequest-deprecated-in-google-maps-v2
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000).setMinUpdateDistanceMeters(5F).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.locations[0]

                if (lastLocation != null) {
                    postData(location)
                    shocks[0] = 0.0f
                    shocks[1] = 0.0f
                    shocks[2] = 0.0f
                }

                lastLocation = location
            }
        }

        accelerationListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not in use
            }

            override fun onSensorChanged(event: SensorEvent) {
                val gravity = FloatArray(3)
                val linearAcceleration = FloatArray(3)
                val alpha: Float = 0.8f

                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                // Remove the gravity contribution with the high-pass filter.
                linearAcceleration[0] = event.values[0] - gravity[0]
                linearAcceleration[1] = event.values[1] - gravity[1]
                linearAcceleration[2] = event.values[2] - gravity[2]

                if (shocks[0] < linearAcceleration[0]) shocks[0] = linearAcceleration[0]
                if (shocks[1] < linearAcceleration[1]) shocks[1] = linearAcceleration[1]
                if (shocks[2] < linearAcceleration[2]) shocks[2] = linearAcceleration[2]
            }
        }

        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            var allAreGranted = true
            for (permission in result.values) {
                allAreGranted = allAreGranted && permission
            }

            /*if (allAreGranted) {

            }*/
        }
    }

    private fun postDrive() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            val data = hashMapOf(
                "createdAt" to FieldValue.serverTimestamp(),
            )

            db.collection("users/${uid}/drives").add(data)
                .addOnSuccessListener {documentReference ->
                    driveId = documentReference.id;
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Couldn't start drive: ${e.message}", Toast.LENGTH_SHORT).show()
                    logout()
                }
        }
        else {
            Toast.makeText(this, "You must be logged in to start a drive", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateQuality(): Int {
        var biggestShock: Float = 0F;

        for (shock in shocks) {
            if (shock > biggestShock) biggestShock = shock;
        }

        val final = 10 - Math.round(biggestShock / 2) + 1;

        if (final < 1) return 1;
        else if (final > 10) return 10;
        return final;
    }

    fun postData(location: Location) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        if (uid != null && driveId != "") {
            val data = hashMapOf(
                "startLat" to lastLocation!!.latitude,
                "startLng" to lastLocation!!.longitude,
                "endLat" to location.latitude,
                "endLng" to location.longitude,
                "measurements" to shocks.asList(),
                "rating" to calculateQuality(),
                "createdAt" to FieldValue.serverTimestamp(),
            )

            db.collection("users/${uid}/drives/${driveId}/measurements")
                .add(data)
                .addOnSuccessListener { documentReference ->
                    binding.textView2.text = "Latest update: ${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}"
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to submit data - ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startLocationUpdates() {
        postDrive()
        driving = true
        binding.buttonDrive.text = getString(R.string.stopButton)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )

        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(accelerationListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private fun stopLocationUpdates() {
        driving = false
        binding.buttonDrive.text = getString(R.string.startButton)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(accelerationListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences("JWT", Context.MODE_PRIVATE)
        //getSettings(true)
        initialize()
        //if(checkLogin()) getProfile()

        val appPerms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
        )
        activityResultLauncher.launch(appPerms)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //startLocationUpdates()

        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onStart() {
        super.onStart()

        checkLogin()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            getProfile()

            val uid = currentUser.uid

            val query = db.collection("users/$uid/drives").orderBy("createdAt", Query.Direction.DESCENDING)
            val options =
                FirestoreRecyclerOptions.Builder<Drive>().setQuery(query, Drive::class.java)
                    .setLifecycleOwner(this).build()

            binding.recyclerViewDrive.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewDrive.itemAnimator = null
            driveAdapter = DriveAdapter(options, object : DriveAdapter.DriveOnClick {
                override fun onClick(id: String) {
                    openDrive(uid, id)
                }

                override fun onLongClick(id: String) {
                    deleteDrive(uid, id)
                }
            })
            binding.recyclerViewDrive.adapter = driveAdapter
        }
    }

    fun openDrive(uid: String, id: String) {
        val intent = Intent(this, MapActivity::class.java)

        intent.putExtra("uid", uid)
        intent.putExtra("driveId", id)
        startActivity(intent)
    }

    fun deleteDrive(uid: String, id: String) {
        val builder = AlertDialog.Builder(this@MainActivity)

        builder.setTitle("Delete drive?")
        builder.setMessage("This will delete the drive and all of the data associated.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Delete") { _, _ ->
            db.collection("users/$uid/drives").document(id)
                .delete()
                .addOnFailureListener {
                    Toast.makeText(this, "Couldn't delete drive", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNeutralButton("Cancel") { _, _ -> }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun checkLogin(): Boolean {
        val currentUser = auth.currentUser

        return if (currentUser == null) {
            binding.textView4.text = "Not logged in"
            binding.buttonLogout.isEnabled = false
            false
        } else {
            binding.buttonLogout.isEnabled = true
            true
        }
    }

    fun openRegisterActivity(view: android.view.View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    fun openLoginActivity(view: android.view.View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    fun logout() {
        auth.signOut()
        checkLogin()
    }

    fun logoutClick(view: android.view.View) {
        logout()
    }

    fun drive(view: android.view.View) {
        if (driving) {
            stopLocationUpdates()
        } else if (checkLogin()) {
            startLocationUpdates()
        }
    }

    fun getProfile() {
        val currentUser = auth.currentUser
        binding.textView4.text = "${currentUser?.email} (${currentUser?.uid})"
    }
}