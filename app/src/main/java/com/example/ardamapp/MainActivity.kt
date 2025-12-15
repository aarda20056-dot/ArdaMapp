@file:OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
package com.example.ardamapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ardamapp.ui.theme.ArdaMappTheme
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.maps.android.compose.Marker
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.platform.LocalContext
import android.location.Location
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.maps.android.compose.Polyline
import kotlin.math.*














class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArdaMappTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        com.example.ardamapp.MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
       @Composable


       fun MainScreen(modifier: Modifier = Modifier) {
         val konumIzni = rememberMultiplePermissionsState(
             permissions = listOf(
               Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION
             )
         )

         LaunchedEffect(Unit) {
             konumIzni.launchMultiplePermissionRequest()
        }
           when {

               konumIzni.allPermissionsGranted -> {
                   val directionsKey = "AIzaSyAo9qkxqm2unYQIhfOHegFw-vdbNEYZBXU"
                   val context = LocalContext.current
                   var rotaPoints by remember { mutableStateOf<String?>(null) }

                   val fusedLocationClient = remember {
                       LocationServices.getFusedLocationProviderClient(context)
                   }
                   val izmir = LatLng(38.4189, 27.1305)
                   var benimKonumum by remember { mutableStateOf<LatLng?>(null) }

                   var hedef by remember { mutableStateOf<LatLng?>(null) }

                   val cameraPositionState = rememberCameraPositionState {
                       position= CameraPosition.fromLatLngZoom(izmir,12f)

                   }
                 val scope = rememberCoroutineScope()

                   LaunchedEffect(hedef) {
                       hedef?.let {  secilen ->
                               cameraPositionState.animate(
                                   update = CameraUpdateFactory.newLatLngZoom(secilen,14f),
                                   durationMs=800
                               )

                       }
                   }
                   @SuppressLint("MissingPermission")
                   LaunchedEffect(Unit) {
                       fusedLocationClient.lastLocation
                           .addOnSuccessListener { location -> location?.let {
                               benimKonumum= LatLng(it.latitude,it.longitude)
                           }

                           }
                   }
                   //ilk konum kamera efekti
                   LaunchedEffect(benimKonumum) {
                       benimKonumum?.let {
                           cameraPositionState.animate(
                               update = CameraUpdateFactory.newLatLngZoom(it,15f),
                               durationMs = 800
                           )
                       }
                   }
                   val directionsApi = remember {
                       Retrofit.Builder()
                           .baseUrl("https://maps.googleapis.com/")
                           .addConverterFactory(GsonConverterFactory.create())
                           .build()
                           .create(DirectionsApi::class.java)
                   }
                   LaunchedEffect(hedef,benimKonumum) {
                       val ben = benimKonumum
                       val şimal = hedef
                       Log.d("DIRECTIONS", "trigger ben=$ben hedef=$şimal")

                       if (ben != null && şimal != null){
                           try {
                               val response = directionsApi.getDirections(
                                   origin = "${ben.latitude},${ben.longitude}",
                                   destination = "${şimal.latitude},${şimal.longitude}",
                                   mode = "walking",
                                   key = directionsKey


                               )
                               rotaPoints= response.routes.firstOrNull()?.overview_polyline?.points
                               Log.d(
                                   "DIRECTIONS",
                                   "status=${response.status},routes=${response.routes.size}"
                               )

                           }catch (e: Exception){
                               Log.e("DIRECTIONS", "Directions çağrısı hata verdi", e)

                           }







                       }



                   }



            GoogleMap(
                modifier = modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { tiklanan ->
                    hedef = tiklanan
                }
            ) {
                Marker(
                    state = MarkerState(position = izmir),
                    title = "Başlangıç Noktası"

                )
                benimKonumum?.let {
                    Marker (
                        state = MarkerState(it),
                        title = "Bulunduğum konum"
                    )
                }
                hedef?.let {
                    Marker (
                        state = MarkerState(it),
                        title = "Hedef"
                    )
                }
                rotaPoints?.let { encoded ->
                    val decoded = decodePolyline(encoded)
                    Polyline(points = decoded)

                }
            }
        }


        konumIzni.shouldShowRationale -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rota Oluştumak İçin Konum İzini Gerekiyor.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    konumIzni.launchMultiplePermissionRequest()
                }) {
                    Text("İzin Ver")
                }
            }
        }


        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Konum izni verilmedi. Devam etmek için izin vermelisiniz.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    konumIzni.launchMultiplePermissionRequest()
                }) {
                    Text("Tekrar Deneyiniz")
                }
             }
         }
     }
 }
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lng += dlng

        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return poly
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ArdaMappTheme {
        Greeting("Android")
    }
}