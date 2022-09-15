package com.udacity.project4.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig

fun showSnackbarWithMessage(fragment : Fragment, @StringRes message : Int) {
    Snackbar.make(fragment.requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        .setAction("Allow") {
            fragment.startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.show()
}


const val REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 33
const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
const val REQUEST_TURN_DEVICE_LOCATION_ON = 29