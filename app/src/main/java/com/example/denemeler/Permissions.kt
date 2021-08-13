package com.example.denemeler

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.example.denemeler.Constants.PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE
import com.example.denemeler.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {
    fun hasLocationPermission(context:Context)=
        EasyPermissions.hasPermissions(context,Manifest.permission.ACCESS_FINE_LOCATION)

    fun requestLocationPermission(fragment:Fragment){
        EasyPermissions.requestPermissions(fragment,
            "This application cannot work without Location Permission",
            PERMISSION_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    }
    fun hasBckgroundLocationPermission(context: Context):Boolean{
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            return EasyPermissions.hasPermissions(context,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return true
    }
    fun requestBackgroundLocationPermission(fragment:Fragment){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            EasyPermissions.requestPermissions(fragment,"Background Location permission is essential to this application",
                PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

    }

}