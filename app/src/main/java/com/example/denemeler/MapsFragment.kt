package com.example.denemeler

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.denemeler.Constants.ACTION_SERVICE_START
import com.example.denemeler.Constants.ACTION_SERVICE_STOP
import com.example.denemeler.ExtensionFunctions.disable
import com.example.denemeler.ExtensionFunctions.enable
import com.example.denemeler.ExtensionFunctions.hide
import com.example.denemeler.ExtensionFunctions.show
import com.example.denemeler.MapUtil.calculateTheDistance
import com.example.denemeler.Permissions.hasBckgroundLocationPermission
import com.example.denemeler.Permissions.requestBackgroundLocationPermission
import com.example.denemeler.databinding.FragmentMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*

class MapsFragment : Fragment(R.layout.fragment_maps),OnMapReadyCallback,GoogleMap.OnMyLocationButtonClickListener,EasyPermissions.PermissionCallbacks {
    private lateinit var binding:FragmentMapsBinding
    private lateinit var map:GoogleMap
    private var locationList= mutableListOf<LatLng>()
    private var startTime=0L
    private var stopTime=0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding= FragmentMapsBinding.bind(view)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
            var total_distance=calculateTheDistance(locationList)
            Toast.makeText(requireContext(),"${total_distance} KM",Toast.LENGTH_LONG).show()
        }
        binding.resetButton.setOnClickListener {  }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map=googleMap
        map.isMyLocationEnabled=true
        map.setOnMyLocationButtonClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled=true
            isZoomGesturesEnabled=true
            isRotateGesturesEnabled=true
            isTiltGesturesEnabled=true
            isCompassEnabled=true
            isScrollGesturesEnabled=true
        }
        observeTrackerService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{
            requestBackgroundLocationPermission(this)
        }
    }
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }
    private fun drawPolyline(){
        val polyline=map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
    }
    private fun followPolyline(){
        if(!locationList.isEmpty()){
            map.animateCamera(
                (CameraUpdateFactory.newCameraPosition(MapUtil.setCameraPosition(locationList.last())
                )),1000,null)
        }

    }
    private fun onStartButtonClicked() {
        if(hasBckgroundLocationPermission(requireContext())){
            startCountDown()
            binding.startButton.disable()
            binding.startButton.hide()
            binding.startButton.show()
        }else{
            requestBackgroundLocationPermission(this)
        }
    }
    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopButton.disable()
        val timer: CountDownTimer =object : CountDownTimer(4000,1000){
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond=millisUntilFinished / 1000
                if(currentSecond.toString()=="0"){
                    binding.timerTextView.text="GO"
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(requireContext(),
                        R.color.black
                    ))
                    binding.startButton.hide()
                    binding.stopButton.show()
                }else{
                    binding.timerTextView.text=currentSecond.toString()
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(requireContext(),
                        R.color.red
                    ))
                }
            }
            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                lifecycleScope.launch {
                    delay(1500)
                    binding.timerTextView.hide()
                }
            }
        }
        timer.start()
    }
    private fun stopForegroundService() {
        binding.startButton.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    private fun sendActionCommandToService(action:String){
        Intent(requireContext(),
            TrackerService::class.java).apply {
            this.action=action
            requireContext().startService(this)
        }
    }
    private fun observeTrackerService(){
        TrackerService.locationList.observe(viewLifecycleOwner,{
            if(it!=null){
                locationList=it
                Log.e("Size",locationList.size.toString())
                val geocoder=Geocoder(requireContext(), Locale.getDefault())
                try{
                    val adresListesi=geocoder.getFromLocation(it.get(0).latitude,it.get(0).longitude,1)
                    // Toast.makeText(requireContext(),"${adresListesi.get(0).getAddressLine(0)}",Toast.LENGTH_LONG).show()
                }catch (e:Exception){

                }

                if(locationList.size>1){
                    binding.stopButton.enable()
                }
                Log.d("Location List",locationList.toString())
                drawPolyline()
                followPolyline()
            }
        })
        TrackerService.startTime.observe(viewLifecycleOwner, Observer {
            startTime = it
        })
        TrackerService.stopTime.observe(viewLifecycleOwner, Observer {
            stopTime=it
            if(stopTime!=0L){
                showBiggerPicture()
            }
        })
    }

    private fun showBiggerPicture() {
        val bounds= LatLngBounds.Builder()
        for(location in locationList){
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds.build(),100),2000,null)
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.startButton.show()
        }
        return false
    }
    private fun onStopButtonClicked() {
        stopForegroundService()
        binding.stopButton.hide()
        binding.startButton.show()
    }


}