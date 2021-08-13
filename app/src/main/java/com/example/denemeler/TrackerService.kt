package com.example.denemeler

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.denemeler.Constants.ACTION_SERVICE_START
import com.example.denemeler.Constants.ACTION_SERVICE_STOP
import com.example.denemeler.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.denemeler.Constants.LOCATION_UPDATE_INTERVAL
import com.example.denemeler.Constants.NOTIFICATION_CHANNEL_ID
import com.example.denemeler.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.denemeler.Constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackerService:LifecycleService() {
    @Inject
    lateinit var notification:NotificationCompat.Builder
    @Inject
    lateinit var notificationManager:NotificationManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    companion object {
        val started= MutableLiveData<Boolean>()
        val startTime=MutableLiveData<Long>()
        val stopTime=MutableLiveData<Long>()
        val locationList=MutableLiveData<MutableList<LatLng>>()
    }
    private fun setInitialValues(){
        started.postValue(false)
        startTime.postValue(0L)
        stopTime.postValue(0L)
        locationList.postValue(mutableListOf())
    }
    private val locationCallBack=object:LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result?.locations?.let { locations->
                for(location in locations){
                    updateLocationList(location)
                    val newLatLng=LatLng(location.latitude,location.longitude)
                    //  val newLatLng=LatLng(location.latitude,location.longitude)
                      Log.d("Tracker Service",newLatLng.toString())
                }
            }
        }
    }
    private fun updateLocationList(location:Location){
        val newLatLng=LatLng(location.latitude,location.longitude)
        locationList.value?.apply {
            add(newLatLng)
            locationList.postValue(this)
        }
    }
    override fun onCreate() {
        setInitialValues()
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_SERVICE_START->{
                    started.postValue(true)
                    startForegroundService()
                    startLocationUpdates()
                }
                ACTION_SERVICE_STOP->{
                    started.postValue(false)
                    stopForegroundService()
                }
                else ->{

                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopForegroundService() {
        removeLocationUpdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack)
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val channel=NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService(){
        createNotificationChannel()
        startForeground(NOTIFICATION_ID,notification.build())
    }
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){

        val locationRequest=LocationRequest.create().apply {
            interval= LOCATION_UPDATE_INTERVAL.toLong()
            fastestInterval= LOCATION_FASTEST_UPDATE_INTERVAL.toLong()
            priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallBack,
            Looper.getMainLooper()
        )
        startTime.postValue(System.currentTimeMillis())
    }


}