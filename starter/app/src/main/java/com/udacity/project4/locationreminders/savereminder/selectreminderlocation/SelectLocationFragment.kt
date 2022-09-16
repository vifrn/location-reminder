package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.utils.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showSnackbarWithMessage
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map : GoogleMap
    private lateinit var marker: Marker
    private lateinit var selectedPoi : PointOfInterest

    companion object {
        private const val TAG = "SelectLocationFragment"
        private const val CUSTOM_PLACE_ID = "custom_place_id"
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        val googleplex = LatLng(37.4221, -122.0841)
        val zoomLevel = 15f
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(googleplex, zoomLevel))

        requestForegroundLocationPermissions()
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            changeMarkerLocation(PointOfInterest(latLng, CUSTOM_PLACE_ID, getString(R.string.dropped_pin)))
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            changeMarkerLocation(poi)
        }
    }

    private fun changeMarkerLocation(poi : PointOfInterest){
        val snippet = String.format(
            Locale.getDefault(),
            getString(R.string.lat_long_snippet),
            poi.latLng.latitude,
            poi.latLng.longitude
        )

        if (this::marker.isInitialized){
            if(!marker.position.equals(poi.latLng)) {
                marker.hideInfoWindow()

                marker.position = poi.latLng
                marker.snippet = snippet
                marker.title = poi.name

                selectedPoi = poi
            } else {
                Log.d(TAG, "Clicked exactly on the same place? Not updating")
            }

        } else {
            marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .snippet(snippet)
                    .title(poi.name)
            )
            selectedPoi = poi
        }

        marker.showInfoWindow()
        map.moveCamera(CameraUpdateFactory.newLatLng(poi.latLng))
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (foregroundLocationPermissionApproved()) {
            map.setMyLocationEnabled(true)

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    // Got last known location. In some rare situations this can be null.
                    location?.let {
                        val currentLocation = LatLng(it.latitude, it.longitude)
                        val zoomLevel = 15f
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel))
                    }
                }
        }
        else {
            requestForegroundLocationPermissions()
        }
    }

    private fun requestForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        Log.d(TAG, "Request foreground location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    private fun foregroundLocationPermissionApproved() : Boolean {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE == requestCode
            && !foregroundLocationPermissionApproved()) {
            showSnackbarWithMessage(this, R.string.foreground_permission_denied_explanation)
        } else {
            enableMyLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        binding.saveLocationButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        if(this::selectedPoi.isInitialized) {
            _viewModel.selectedPOI.value = selectedPoi
            _viewModel.longitude.value = selectedPoi.latLng.longitude
            _viewModel.latitude.value = selectedPoi.latLng.latitude
            _viewModel.reminderSelectedLocationStr.value = selectedPoi.name

            findNavController().popBackStack()
        } else {
            Toast.makeText(requireContext(), R.string.err_select_location, Toast.LENGTH_LONG).show()
        }

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}
