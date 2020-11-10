package com.example.erstespraktikum

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.json.JSONObject
import java.sql.Timestamp
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener {
    private var tvAccelerometer:ArrayList<TextView> = ArrayList()
    private var idAccelerometer:ArrayList<Int> = arrayListOf(R.id.tv_acc_x,R.id.tv_acc_y,R.id.tv_acc_z,R.id.tv_mag)
    private var tvPosition:ArrayList<TextView> = ArrayList()
    private var idPosition:ArrayList<Int> = arrayListOf(R.id.tv_lat,R.id.tv_long,R.id.tv_alt,R.id.tv_acc)
    private lateinit var sensorManager : SensorManager
    private lateinit var sensorAccelerometer : Sensor
    private var accelerometerData : SensorData? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var PERMISSION_ID = 1000
    private var jsonObject = JSONObject()
    private var jsonObject2 = JSONObject()
    private val timestamp: Timestamp= Timestamp(System.currentTimeMillis())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if(sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!=null) {
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        }
        //Initialisierung der TextViews und Buttons
        for(i in idAccelerometer){
            tvAccelerometer.add(findViewById(i))
        }
        for(i in idPosition){
            tvPosition.add(findViewById(i))
        }
        val buttonStart: Button = findViewById(R.id.startButton)
        val buttonStop : Button = findViewById(R.id.stopButton)
        //Start des Positionstrackings
        buttonStart.setOnClickListener(){
            getLastLocation()
            registerListener()
        }
        //Stoppen des Positionstrackings
        buttonStop.setOnClickListener(){
            unregisterListener()
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }
    //Bestimmung der letzten Lokation nach Prüfung und ggf. Erteilung der erforderlichen Berechtigungen
    private fun getLastLocation(){
        if(checkPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener{task->
                    var location = task.result
                    locationRequest = LocationRequest().setFastestInterval(1000).setInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    //Falls vorhanden, die initialen Positionsdaten
                    if(location!=null){
                        tvPosition[0].text = "Latitude: "+location.latitude
                        tvPosition[1].text = "Longitude: "+location.longitude
                        tvPosition[2].text = "Altitude: "+location.altitude
                        tvPosition[3].text = "Genauigkeit: "+location.accuracy
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper())
                }
            }
            else{
                Toast.makeText(this, "Please Enable Your Service", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            requestPermission()
        }
    }
    //Location Callback
    private val locationCallback = object: LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            tvPosition[0].text = "Latitude: "+p0.lastLocation.latitude
            tvPosition[1].text = "Longitude: "+p0.lastLocation.longitude
            tvPosition[2].text = "Altitude: "+p0.lastLocation.altitude
            tvPosition[3].text = "Genauigkeit: "+p0.lastLocation.accuracy
            jsonObject.put("Latitude",p0.lastLocation.latitude)
            jsonObject.put("Longitude", p0.lastLocation.longitude)
            jsonObject.put("Altitude", p0.lastLocation.altitude)
            jsonObject.put("Genauigkeit", p0.lastLocation.accuracy)
            jsonObject.put("Timestamp", timestamp)
            Log.d("data",jsonObject.toString())
        }
    }
    //Prüfung, ob erforderliche (kritische) Berechtigungen vorliegen
    private fun checkPermission():Boolean{
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }
    //Erfragen der erforderlichen (kritischen) Berechtigungen beim Nutzer
    private fun requestPermission(){
        ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION),PERMISSION_ID)
    }
    //Prüfung, ob Positionsbestimmung mittels GPS oder Netzwerk aktiviert ist
    private fun isLocationEnabled():Boolean{
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode==PERMISSION_ID){
            if(grantResults.isNotEmpty()&&grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Log.d("Debug", "You Have the Permission")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event!!.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION){
            getAccelerometerData(event)
        }
    }
    //RegisterListener für den Beschleunigungssenor
    private fun registerListener(){
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null){
            sensorManager.registerListener(this,sensorAccelerometer,SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    //Aufheben des RegisterListeners für Beschleunigungssensor
    private fun unregisterListener(){
        sensorManager.unregisterListener(this,sensorAccelerometer)
    }
    //Bestimmung der Daten des Beschleunigungssensors mittels DataClass SensorData
    private fun getAccelerometerData(e:SensorEvent?){
        if(accelerometerData == null) {
            accelerometerData = SensorData(e!!.values[0], e.values[1], e.values[2], e.timestamp)
        }
        else{
            accelerometerData!!.x1 = e!!.values[0]
            accelerometerData!!.x2 = e.values[1]
            accelerometerData!!.x3 = e.values[2]
        }
        tvAccelerometer[0].text = "X: ${"%.2f".format(accelerometerData!!.x1)}"
        tvAccelerometer[1].text = "Y: ${"%.2f".format(accelerometerData!!.x2)}"
        tvAccelerometer[2].text = "Z: ${"%.2f".format(accelerometerData!!.x3)}"
        val magnitude : Float = sqrt(accelerometerData!!.x1*accelerometerData!!.x1+accelerometerData!!.x2*accelerometerData!!.x2+accelerometerData!!.x3*accelerometerData!!.x3)
        tvAccelerometer[3].text = "Magnitude: ${"%.2f".format(magnitude)}"
        jsonObject2.put("X",accelerometerData!!.x1.toDouble())
        jsonObject2.put("Y", accelerometerData!!.x2.toDouble())
        jsonObject2.put("Z", accelerometerData!!.x3.toDouble())
        jsonObject2.put("Time", timestamp)
        Log.d("data2",jsonObject2.toString())
    }
}