package si.djdado.ddrive

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import si.djdado.ddrive.databinding.ActivityMapBinding
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapBinding
    private lateinit var map: MapView
    private lateinit var mapController: IMapController

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var uid: String
    private lateinit var driveId: String

    private val measurmentIds = ArrayList<String>();

    private lateinit var measurmentsListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        if (extras?.getString("uid") != null) {
            uid = extras.getString("uid")!!
        } else finish()
        if (extras?.getString("driveId") != null) {
            driveId = extras.getString("driveId")!!
        } else finish()

        Configuration.getInstance().load(applicationContext, this.getPreferences(Context.MODE_PRIVATE))

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)

        map.setMultiTouchControls(true)
        mapController = map.controller

        mapController.setZoom(10)

        val startPoint = GeoPoint(46.05678489934201, 14.503592098372705)
        mapController.setCenter(startPoint)

        auth = Firebase.auth
        db = Firebase.firestore
    }

    fun getRatingColor(rating: Int?): String {
        return when (rating) {
            1 -> "#ff0000"
            2 -> "#ff4000"
            3 -> "#ff8000"
            4 -> "#ffbf00"
            5 -> "#ffff00"
            6 -> "#bfff00"
            7 -> "#80ff00"
            8 -> "#40ff00"
            9 -> "#00ff00"
            10 -> "#00ff40"
            else -> "#3c64c8"
        }
    }

    override fun onStart() {
        super.onStart()

        measurmentsListener  = db.collection("users/$uid/drives/$driveId/measurements").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Toast.makeText(this, "Error receiving data", Toast.LENGTH_SHORT).show()
                }

                for (doc in value!!) {
                    val docId = doc.id

                    val startLat = doc.getDouble("startLat")
                    val startLng = doc.getDouble("startLng")

                    val endLat = doc.getDouble("endLat")
                    val endLng = doc.getDouble("endLng")

                    val rating = Math.toIntExact(doc.getLong("rating") ?: 0)

                    if(!measurmentIds.contains(docId)) {
                        measurmentIds.add(docId)

                        val startPoint = GeoPoint(startLat!!, startLng!!);
                        val endPoint = GeoPoint(endLat!!, endLng!!);

                        val line = Polyline();
                        line.setPoints(listOf(startPoint,endPoint))
                        line.color = Color.parseColor(getRatingColor(rating))

                        map.overlays.add(line);

                        //Log.i("Lines", "$docId - $startLat, $startLng - $endLat, $endLng")
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume();
        map.onResume()
    }

    override fun onPause() {
        super.onPause();
        map.onPause()
    }

    override fun onStop() {
        super.onStop()

        if (this::measurmentsListener.isInitialized) {
            measurmentsListener.remove()
            Toast.makeText(this, "Stopped receiving updates", Toast.LENGTH_SHORT).show()
        }
    }
}