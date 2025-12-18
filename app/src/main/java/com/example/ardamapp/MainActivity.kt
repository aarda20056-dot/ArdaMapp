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

import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.ui.Alignment
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.maps.android.compose.Polyline
import kotlin.math.*
import com.google.android.gms.location.Priority












enum class UlasimModları(val apiDegeri: String, val etiket: String) {
    YURUYUS("walking", "Yürüyüş"),
    ARABA("driving", "Araba"),
    TOPLU_TASIMA("transit", "Toplu Taşıma")
}

data class RotaSecenegi(
    val ulasimModları: UlasimModları,
    val sureYazisi: String?,
    val mesafeYazisi: String?,
    val polylineNoktalari: String?
)


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
                   var rotalar by remember { mutableStateOf<List<RotaSecenegi>>(emptyList()) }
                   var seciliUlasimModları by remember { mutableStateOf(UlasimModları.YURUYUS) }
                   val directionsKey = "AIzaSyAo9qkxqm2unYQIhfOHegFw-vdbNEYZBXU"
                   val context = LocalContext.current
                   val fusedLocationClient = remember {
                       LocationServices.getFusedLocationProviderClient(context)
                   }
                   var benimKonumum by remember { mutableStateOf<LatLng?>(null) }

                   var hedef by remember { mutableStateOf<LatLng?>(null) }

                   val cameraPositionState = rememberCameraPositionState {


                   }


                   LaunchedEffect(hedef) {
                       hedef?.let { secilen ->
                           cameraPositionState.animate(
                               update = CameraUpdateFactory.newLatLngZoom(secilen, 14f),
                               durationMs = 800
                           )

                       }
                   }
                   @SuppressLint("MissingPermission")
                   LaunchedEffect(Unit) {
                       fusedLocationClient.getCurrentLocation(
                           com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                           null
                       ).addOnSuccessListener { location ->
                           location?.let {
                               benimKonumum = LatLng(it.latitude, it.longitude)

                           }

                       }
                   }
                   //ilk konum kamera efekti
                   LaunchedEffect(benimKonumum) {
                       benimKonumum?.let {
                           cameraPositionState.animate(
                               update = CameraUpdateFactory.newLatLngZoom(it, 15f),
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
                   LaunchedEffect(hedef, benimKonumum) {
                       val ben = benimKonumum
                       val şimal = hedef
                       if (ben == null || şimal == null) return@LaunchedEffect

                       if (ben != null && şimal != null) {
                           try {
                               val modlar = listOf(
                                   UlasimModları.YURUYUS,
                                   UlasimModları.ARABA,
                                   UlasimModları.TOPLU_TASIMA
                               )
                               val yeniRotalar = mutableListOf<RotaSecenegi>()

                               for (m in modlar) {
                                   val response = directionsApi.getDirections(
                                       origin = "${ben.latitude},${ben.longitude}",
                                       destination = "${şimal.latitude},${şimal.longitude}",
                                       mode = m.apiDegeri,
                                       key = directionsKey,
                                       units = "metric"
                                   )
                                   Log.d(
                                       "DIRECTIONS_MODE",
                                       "mod=${m.etiket} status=${response.status} routes=${response.routes.size}")
                                   val leg = response.routes.firstOrNull()?.legs?.firstOrNull()
                                   val poly =
                                       response.routes.firstOrNull()?.overview_polyline?.points

                                   yeniRotalar.add(
                                       RotaSecenegi(
                                           ulasimModları = m,
                                           sureYazisi = leg?.duration?.text,
                                           mesafeYazisi = leg?.distance?.text,
                                           polylineNoktalari = poly
                                       )
                                   )
                               }
                               rotalar = yeniRotalar



                           } catch (e: Exception) {
                               Log.e("DIRECTIONS", "Directions çağrısı hata verdi", e)
                               rotalar = emptyList()



                           }


                       }


                   }
                   if (benimKonumum == null) {
                       Text("Konum Alınıyor...")
                   } else {

                       Box(modifier = modifier.fillMaxSize()) {
                           val seciliRota = rotalar.firstOrNull { it.ulasimModları == seciliUlasimModları }


                           GoogleMap(
                               modifier = modifier.fillMaxSize(),
                               cameraPositionState = cameraPositionState,
                               onMapClick = { tiklanan ->
                                   hedef = tiklanan


                               }
                           ) {

                               benimKonumum?.let {
                                   Marker(
                                       state = MarkerState(it),
                                       title = "Bulunduğum konum"
                                   )
                               }
                               hedef?.let {
                                   Marker(
                                       state = MarkerState(it),
                                       title = "Hedef nokta"
                                   )
                               }
                              seciliRota?.polylineNoktalari?.let { encoded ->
                                   val decoded = decodePolyline(encoded)
                                   Polyline(points = decoded)

                               }
                           }
                           Column(
                               modifier = Modifier
                                   .align(Alignment.TopCenter)
                                   .padding(12.dp),
                               horizontalAlignment = Alignment.CenterHorizontally
                           ) {
                               rotalar
                                   .sortedByDescending { it.ulasimModları==seciliUlasimModları }
                                   .forEach { r ->
                                   Button(
                                       onClick = {
                                           seciliUlasimModları = r.ulasimModları

                                       },
                                       modifier = Modifier.fillMaxWidth(0.92f)
                                   ) {
                                       val seçililiMod =r.ulasimModları==seciliUlasimModları
                                       val yuruyus = rotalar.firstOrNull { it.ulasimModları == UlasimModları.YURUYUS }
                                       val transitYuruyusGibiMi =
                                           (r.ulasimModları == UlasimModları.TOPLU_TASIMA) &&
                                                   (yuruyus != null) &&
                                                   (r.sureYazisi == yuruyus.sureYazisi) &&
                                                   (r.mesafeYazisi == yuruyus.mesafeYazisi)
                                       Text(
                                           "${if (seçililiMod) "✅ " else ""}${r.ulasimModları.etiket}" +
                                                   "${if (transitYuruyusGibiMi) " (yürüyüş gibi)" else ""}: " +
                                                   "${r.sureYazisi ?: "-"} • ${r.mesafeYazisi ?: "-"}"
                                       )


                                   }

                                   Spacer(Modifier.height(4.dp))
                               }
                               seciliRota?.let { secili ->
                                   Spacer(Modifier.height(6.dp))
                                   Text("Seçili: ${secili.ulasimModları.etiket} • ${secili.sureYazisi ?: "-"} • ${secili.mesafeYazisi ?: "-"}")

                                   val yuruyus = rotalar.firstOrNull { it.ulasimModları == UlasimModları.YURUYUS }
                                   val toplu = rotalar.firstOrNull { it.ulasimModları == UlasimModları.TOPLU_TASIMA }

                                   if (
                                       yuruyus != null && toplu != null &&
                                       toplu.sureYazisi == yuruyus.sureYazisi &&
                                       toplu.mesafeYazisi == yuruyus.mesafeYazisi
                                   ) {
                                       Text("Not: Bu saat için toplu taşıma uygun görünmüyo; yürüyüş ile aynı rota öneriliyor.")
                                   }
                               }
                           }

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


