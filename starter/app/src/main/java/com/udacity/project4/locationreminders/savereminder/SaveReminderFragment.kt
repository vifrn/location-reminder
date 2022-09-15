package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.ACTION_GEOFENCE_INTENT
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private lateinit var reminder : ReminderDataItem

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_INTENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {

            reminder = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value)

            if(_viewModel.validateEnteredData(reminder)) {
                preGeofenceChecking()
            }
        }
    }

    private fun preGeofenceChecking() {
        if(!foregroundLocationPermissionApproved()) {
            requestForegroundLocationPermissions()
        } else if (!backgroundLocationPermissionApproved()) {
            requestBackgroundLocationPermissions()
        } else {
            checkDeviceLocationSettings()
        }
    }

    @TargetApi(29)
    private fun foregroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))

        return foregroundLocationApproved
    }

    @TargetApi(29)
    private fun backgroundLocationPermissionApproved(): Boolean {
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }

        return backgroundPermissionApproved
    }

    @TargetApi(29)
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

    @TargetApi(29)
    private fun requestBackgroundLocationPermissions() {
        if (backgroundLocationPermissionApproved() || !runningQOrLater)
            return

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_background_permission)
            .setMessage(R.string.dialog_message_background_permission)
            .setPositiveButton(R.string.dialog_ok_option) { dialog, id ->
                var permissionsArray = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                val resultCode = REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                Log.d(TAG, "Request background location permission")
                requestPermissions(
                    permissionsArray,
                    resultCode
                )
            }.setNegativeButton(R.string.dialog_cancel_option) { dialog, id ->
                showSnackbarWithMessage(this, R.string.background_permission_denied_explanation)
            }.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE == requestCode){
            if(!foregroundLocationPermissionApproved()) {
                    showSnackbarWithMessage(this, R.string.foreground_permission_denied_explanation)
            } else {
                preGeofenceChecking()
            }
        } else if (REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE == requestCode) {
            if (!backgroundLocationPermissionApproved()) {
                showSnackbarWithMessage(this, R.string.background_permission_denied_explanation)
            } else {
                preGeofenceChecking()
            }
        }
    }

    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                showSnackbarWithLocationServicesError()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addReminderGeofence()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_TURN_DEVICE_LOCATION_ON && resultCode == Activity.RESULT_OK) {
            addReminderGeofence()
        } else {
            showSnackbarWithLocationServicesError()
        }
    }

    private fun showSnackbarWithLocationServicesError () {
        Snackbar.make(
            binding.root,
            R.string.location_required_error, Snackbar.LENGTH_LONG
        ).setAction(android.R.string.ok) {
            checkDeviceLocationSettings()
        }.show()
    }

    @SuppressLint("MissingPermission")
    private fun addReminderGeofence() {
        if(backgroundLocationPermissionApproved() && this::reminder.isInitialized) {
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    reminder.latitude!!,
                    reminder.longitude!!,
                    GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            geofencingClient.addGeofences(request, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(reminder)
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Failed to add geofence",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}

private const val TAG = "SaveReminderFragment"
