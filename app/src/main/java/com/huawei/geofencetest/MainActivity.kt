package com.huawei.geofencetest

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.huawei.hmf.tasks.OnCompleteListener
import com.huawei.hmf.tasks.Task
import com.huawei.hms.location.*
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.CircleOptions
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions

@Suppress("DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity(),OnMapReadyCallback {

    private lateinit var  hMap: HuaweiMap
    private lateinit var mMapView: MapView
    private lateinit var fusedLocation: FusedLocationProviderClient
    private  var locationCallback: LocationCallback?=null
    private lateinit var locationRequest : LocationRequest
    private lateinit var settingsClient : SettingsClient
    private lateinit var geofenceServices: GeofenceService

    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    private var mGeofenceList: ArrayList<Geofence>? = null
    private var mGeofencePendingIntent: PendingIntent? = null
    private lateinit var editor : SharedPreferences.Editor
    var activityTransitionRequest: ActivityConversionRequest? = null
    private lateinit var activityConversionInfos: List<ActivityConversionInfo>


    private var pendingIntent: PendingIntent? = null

    private var activityIdentificationService: ActivityIdentificationService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Fused Location
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        settingsClient =LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 5000
        locationRequest.priority =LocationRequest.PRIORITY_HIGH_ACCURACY

        //Activity recognition
        activityIdentificationService = ActivityIdentification.getService(this)
        pendingIntent = getPendingIntent()

        //Geofence
        mGeofenceList = ArrayList<Geofence>()
        geofenceServices = LocationServices.getGeofenceService(applicationContext)

        if (null == locationCallback) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    for(i in locationResult!!.locations){
                        Toast.makeText(
                            applicationContext,
                            i.latitude.toString() + i.longitude.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        hMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(i.latitude, i.longitude), 16f))
                    }
                }
            }
        }

        mMapView = findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mMapView.onCreate(mapViewBundle)
        //get map instance
        //get map instance
        mMapView.getMapAsync(this)

        val mockLocation = Location(LocationManager.GPS_PROVIDER)
        mockLocation.latitude = 41.043912
        mockLocation.longitude = 29.1432343
        fusedLocation.setMockMode(true)
        val voidTask: Task<Void> =
            fusedLocation.setMockLocation(mockLocation)
        voidTask.addOnSuccessListener {
            Log.i(
                "mockLocation",
                "setMockLocation onSuccess $mockLocation"
            )
        }
            .addOnFailureListener { e ->
                Log.e(
                    "mockLocation",
                    "setMockLocation onFailure:" + e.message
                )
            }

        requestPermissions()

        //Update request
        updateRequestWithCallback()

        //Geofence
        populateGeofenceList()

        //Activity Recognition part
        listenUserIdentification()
        createActivityConversionUpdate()
        requestActivityTransitionUpdate()


    }

    private fun populateGeofenceList() {
        for ((key, value) in Constants.BAY_AREA_LANDMARKS.entries) {
            mGeofenceList!!.add(
                Geofence.Builder()
                    .setUniqueId(key)
                    .setRoundArea(
                        value.latitude,
                        value.longitude,
                        Constants.GEOFENCE_RADIUS_IN_METERS
                    ) // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setValidContinueTime(Geofence.GEOFENCE_NEVER_EXPIRE)
                    .setConversions(
                        Geofence.ENTER_GEOFENCE_CONVERSION
                    )
                    .build()
            )
        }
   }

    private fun getGeofencingRequest(): GeofenceRequest? {
        val builder: GeofenceRequest.Builder = GeofenceRequest.Builder()
        builder.setInitConversions(GeofenceRequest.ENTER_INIT_CONVERSION)
        // Add the geofences to be monitored by geofencing service.
        builder.createGeofenceList(mGeofenceList)
        // Return a GeofencingRequest.
        return builder.build()
    }

    private fun addGeofences() {
        geofenceServices.createGeofenceList(getGeofencingRequest(), getGeofencePendingIntent())
            .addOnCompleteListener(OnCompleteListener<Void?> { task ->
                if (task.isSuccessful) {
                    Log.i("HuaweiGeofence", "add geofence success！")
                } else {
                    Log.w("HuaweiGeofence", "add geofence failed : " + task.exception.message)
                }
            })
    }

    private fun getGeofencePendingIntent(): PendingIntent? {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        mGeofencePendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return mGeofencePendingIntent
    }

    override fun onMapReady(map: HuaweiMap?) {
        map!!.isMyLocationEnabled = true //Enables the my-location function.
        map.uiSettings.isMyLocationButtonEnabled = false

        map.setOnMapClickListener {
            addGeofences()
            map.addMarker(MarkerOptions().position(it).alpha(0.5f))
            map.addCircle(CircleOptions().center(it).radius(100.0).fillColor(R.color.colorAccent).strokeWidth(5f)).strokeColor
        }

        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
        map.uiSettings.isTiltGesturesEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.setAllGesturesEnabled(true)

        hMap = map
    }

    private fun requestPermissions() {
        //You must have the ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission.
        // Otherwise, the location service is unavailable.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                ActivityCompat.requestPermissions(this, strings, 1)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,Manifest.permission.ACTIVITY_RECOGNITION)
                  !== PackageManager.PERMISSION_GRANTED
            )
            {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
                ActivityCompat.requestPermissions(this, strings, 2)
            }
        }
    }

    private fun updateRequestWithCallback(){
        try{
              fusedLocation.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
                    .addOnSuccessListener {
                        Toast.makeText(applicationContext, "Success Update",Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(applicationContext,"Not Success Update",Toast.LENGTH_SHORT).show()
                    }
        }catch (e:Exception){
            Toast.makeText(applicationContext,e.message,Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteUserIdentification()
    }

    private fun listenUserIdentification(){
        RecognitionBroadcastReceiver.addIdentificationListener()
        activityIdentificationService!!.createActivityIdentificationUpdates(5000, pendingIntent)
            .addOnSuccessListener {
                Log.i(
                    "IdentificationLog",
                    "createActivityIdentificationUpdates onSuccess"
                )
            }
            .addOnFailureListener {
                Log.e(
                    "IdentificationLog",
                    "createActivityIdentificationUpdates onFailure:" + it.message
                )
            }
    }

    private fun deleteUserIdentification(){
        activityIdentificationService!!.deleteActivityIdentificationUpdates(pendingIntent)
            .addOnSuccessListener {
                Log.i(
                    "IdentificationLog",
                    "createActivityIdentificationUpdates onSuccess"
                )
            }
            .addOnFailureListener {
                Log.e(
                    "IdentificationLog",
                    "createActivityIdentificationUpdates onFailure:" + it.message
                )
            }
    }

    private fun createActivityConversionUpdate(){
        val STILL = 103
        val activityConversionInfo1 =
            ActivityConversionInfo(STILL, ActivityConversionInfo.ENTER_ACTIVITY_CONVERSION)
        val activityConversionInfo2 =
            ActivityConversionInfo(STILL, ActivityConversionInfo.EXIT_ACTIVITY_CONVERSION)
        activityConversionInfos = ArrayList<ActivityConversionInfo>()
        (activityConversionInfos as ArrayList<ActivityConversionInfo>).add(activityConversionInfo1)
        (activityConversionInfos as ArrayList<ActivityConversionInfo>).add(activityConversionInfo2)
        val request = ActivityConversionRequest()
        request.activityConversions = activityConversionInfos
    }

    private fun requestActivityTransitionUpdate(){
        RecognitionBroadcastReceiver.addConversionListener()
        pendingIntent = getPendingIntent()
        activityTransitionRequest = ActivityConversionRequest(activityConversionInfos)
        val task =  activityIdentificationService!!.createActivityConversionUpdates(
            activityTransitionRequest,
            pendingIntent
        )
        task.addOnSuccessListener {
            Log.i(
                "ConversionLog",
                "createActivityConversionUpdates onSuccess"
            )
        }.addOnFailureListener { e ->
                Log.e(
                    "ConversionLog",
                    "createActivityConversionUpdates onFailure:" + e.message
                )
            }
        }

    private fun getPendingIntent(): PendingIntent? {
        //The LocationBroadcastReceiver class is a custom class. For detailed implementation methods, please refer to the sample code.
        val intent = Intent(this, RecognitionBroadcastReceiver::class.java)
        intent.action = RecognitionBroadcastReceiver.ACTION_PROCESS_LOCATION
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        @JvmStatic
        fun sendData(list: MutableList<ActivityIdentificationData>) {
            for(i in list){
                val type = i.identificationActivity
                val value = i.possibility
                val result = when(type){
                    100 -> "activity IN VEHICLE"
                    101 -> "activity ON BICYCLE"
                    102 -> "activity ON FOOT"
                    103 -> "activity STILL"
                    104 -> "activity OTHERS"
                    105 -> "activity TILTING"
                    107 -> "activity WALKING"
                    108 -> "activity RUNNING"
                    else-> "null"
                }
                Log.i("RecognitionResult",result)
            }
        }
    }
}


