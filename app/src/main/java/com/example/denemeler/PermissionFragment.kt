package com.example.denemeler

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.denemeler.Permissions.hasLocationPermission
import com.example.denemeler.Permissions.requestLocationPermission
import com.example.denemeler.databinding.FragmentPermissionBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint


class PermissionFragment : Fragment(R.layout.fragment_permission), EasyPermissions.PermissionCallbacks {
    private lateinit var binding: FragmentPermissionBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding= FragmentPermissionBinding.bind(view)
        if(hasLocationPermission(requireContext())){
            findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
        }
        binding.continueButton.setOnClickListener {
            if(hasLocationPermission(requireContext())){
                findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
            }else{
                requestLocationPermission(this)
            }

        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{
            requestLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)

    }
}