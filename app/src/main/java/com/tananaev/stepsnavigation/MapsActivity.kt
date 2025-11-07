package com.tananaev.stepsnavigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.cos
import kotlin.math.sin

class MapsActivity : FragmentActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMapLongClickListener,
    SensorEventListener {

    private lateinit var fragment: SupportMapFragment
    private lateinit var map: GoogleMap

    private val markers = mutableListOf<Marker>()

    private var lat = 0.0
    private var lon = 0.0

    private val step = 0.762
    private val radius = 6378137.0

    private var gravity: FloatArray? = null
    private var magnetic: FloatArray? = null

    private lateinit var sensorManager: SensorManager

    private fun showInfo(message: Int) {
        val rootView = findViewById<View>(android.R.id.content)
        //snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE)
        //snackbar?.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        fragment.getMapAsync(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                if (gravity != null && magnetic != null && markers.isNotEmpty()) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, magnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val azimut = orientation[0]

                        lat += step * cos(azimut.toDouble()) * 180 / radius / Math.PI
                        lon += step * sin(azimut.toDouble()) * 180 / radius / cos(Math.PI * lat / 180) / Math.PI

                        val latLng = LatLng(lat, lon)
                        map.addMarker(MarkerOptions().position(latLng))
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    }
                }
            }

            Sensor.TYPE_GRAVITY -> gravity = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> magnetic = event.values.clone()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapLongClickListener(this)
        showInfo(R.string.info_select_location)
    }

    override fun onMapLongClick(latLng: LatLng) {
        lat = latLng.latitude
        lon = latLng.longitude

        markers.forEach { it.remove() }
        markers.clear()

        markers.add(map.addMarker(MarkerOptions().position(latLng))!!)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.maxZoomLevel))
        showInfo(R.string.info_navigation)
    }
}
