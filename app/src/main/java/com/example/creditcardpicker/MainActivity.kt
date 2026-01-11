package com.example.creditcardpicker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.creditcardpicker.ui.theme.CreditCardPickerTheme
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
            getLocation()
        } else {
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val location = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)

        enableEdgeToEdge()
        setContent {
            CreditCardPickerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationScreen(location.value, Modifier.padding(innerPadding)) {
                        when {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // You can use the API that requires the permission.
                                getLocation()
                            }
                            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                                // In an educational UI, explain to the user why your app requires this
                                // permission for a specific feature to behave as expected. In this UI,
                                // include a "cancel" or "no thanks" button that allows the user to
                                // continue using your app without granting the permission.
                                Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
                                requestPermissionLauncher.launch(
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            else -> {
                                // You can directly ask for the permission.
                                requestPermissionLauncher.launch(
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    findPlace()
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun findPlace() {
        // Use fields to define the data types to return.
        val placeFields = listOf(Place.Field.NAME)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        // Get the likely places - that is, the businesses and other points of interest that
        // are the best match for the device's current location.
        val placesClient = Places.createClient(this)
        val placeResult = placesClient.findCurrentPlace(request)
        placeResult.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val likelyPlaces = task.result
                if(likelyPlaces.placeLikelihoods.isNotEmpty()){
                    val place = likelyPlaces.placeLikelihoods.first().place
                    location.value = place.name
                } else {
                    location.value = "No places found nearby"
                }

            } else {
                val exception = task.exception
                if (exception is ApiException) {
                    Log.e("Places API", "Place not found: ${exception.statusCode}")
                    Log.e("Places API", "Place not found: ${exception.message}")
                    location.value = "Error getting places: ${exception.message}"
                } else {
                    location.value = "An unknown error occurred."
                    Log.e("Places API", "Unknown error: ", exception)
                }
                Toast.makeText(this, "Error getting places", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun LocationScreen(location: String?, modifier: Modifier = Modifier, onGetLocationClick: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onGetLocationClick) {
            Text("Get Location")
        }
        location?.let {
            Text(text = it)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CreditCardPickerTheme {
        LocationScreen(location = "A cool restaurant") {}
    }
}
