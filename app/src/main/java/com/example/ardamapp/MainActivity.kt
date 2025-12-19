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
import androidx.compose.foundation.background
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.maps.android.compose.Polyline
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.google.android.libraries.places.api.Places
import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Place
import androidx.compose.foundation.layout.statusBarsPadding
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshotFlow












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
        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                "AIzaSyAo9qkxqm2unYQIhfOHegFw-vdbNEYZBXU"
            )
        }

        enableEdgeToEdge()
        setContent {
            ArdaMappTheme {
                @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    MainScreen(modifier = Modifier.fillMaxSize())
                }

            }
            }
        }
    }


       @OptIn(ExperimentalMaterial3Api::class)
       @Composable


       fun MainScreen(modifier: Modifier = Modifier) {
           val konumIzni = rememberMultiplePermissionsState(
               permissions = listOf(
                   Manifest.permission.ACCESS_FINE_LOCATION,
                   Manifest.permission.ACCESS_COARSE_LOCATION
               )
           )
           var searchText by remember { mutableStateOf("") }
           var searchCompleted by remember { mutableStateOf(false) }
           val scope = rememberCoroutineScope()




           LaunchedEffect(Unit) {
               konumIzni.launchMultiplePermissionRequest()
           }
           when {

               konumIzni.allPermissionsGranted -> {
                   val bottomSheetState=  rememberModalBottomSheetState(
                       skipPartiallyExpanded = true
                   )
                   var rotalarGosterilsin by remember { mutableStateOf(false) }


                   var bottomSheetAcik by remember { mutableStateOf(false) }
                   var rotalar by remember { mutableStateOf<List<RotaSecenegi>>(emptyList()) }
                   var seciliUlasimModları by remember { mutableStateOf(UlasimModları.YURUYUS) }
                   val directionsKey = "AIzaSyAo9qkxqm2unYQIhfOHegFw-vdbNEYZBXU"
                   val context = LocalContext.current
                   val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

                   val placesClient = remember { Places.createClient(context) }
                   var suggestions by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
                   var suggestionsVisible by remember { mutableStateOf(false) }
                   var hataMesajı by remember { mutableStateOf<String?>(null) }
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
                                       "mod=${m.etiket} status=${response.status} routes=${response.routes.size}"
                                   )

                                   val route = response.routes.firstOrNull() ?: continue
                                   val leg = route.legs.firstOrNull() ?: continue
                                   val poly = route.overview_polyline?.points ?: continue


                                   yeniRotalar.add(
                                       RotaSecenegi(
                                           ulasimModları = m,
                                           sureYazisi = leg.duration?.text,
                                           mesafeYazisi = leg.distance?.text,
                                           polylineNoktalari = poly
                                       )
                                   )
                               }
                               if (yeniRotalar.isEmpty()) {
                                   hataMesajı = "Seçilen hedef için geçerli bir rota bulunamadı."
                                   rotalar = emptyList()
                                   return@LaunchedEffect
                               }

                               hataMesajı = null
                               rotalar = yeniRotalar


                           } catch (e: Exception) {
                               Log.e("DIRECTIONS", "Directions çağrısı hata verdi", e)
                               rotalar = emptyList()
                               hataMesajı ="İnternet bağlantısı yok veya rota alınamadı."



                           }


                       }



                   }
                   LaunchedEffect(placesClient) {
                       snapshotFlow { searchText }
                           .map { it.trim() }
                           .debounce(300)
                           .distinctUntilChanged()
                           .collectLatest { q ->
                               if (searchCompleted) {
                                   suggestions = emptyList()
                                   suggestionsVisible = false
                                   return@collectLatest
                               }

                               if (q.length < 2) {
                                   suggestions = emptyList()
                                   suggestionsVisible = false
                                   return@collectLatest
                               }
                               if (q.length < 2) {
                                   suggestions = emptyList()
                                   suggestionsVisible = false
                                   return@collectLatest
                               }

                               try {
                                   val req = FindAutocompletePredictionsRequest.builder()
                                       .setQuery(q)
                                       .build()

                                   val res = placesClient.findAutocompletePredictions(req).await()

                                   suggestions = res.autocompletePredictions
                                       .take(6)
                                       .map {
                                           Triple(
                                               it.getPrimaryText(null).toString(),
                                               it.getSecondaryText(null).toString(),
                                               it.placeId
                                           )
                                       }

                                   suggestionsVisible = suggestions.isNotEmpty()

                                   Log.d("PLACES_SUG", "count=${suggestions.size} visible=$suggestionsVisible q=$q")
                               } catch (e: Exception) {
                                   suggestions = emptyList()
                                   suggestionsVisible = false
                                   Log.e("PLACES", "autocomplete error", e)
                               }
                           }
                   }

                   fun selectSuggestion(placeId: String) {
                       scope.launch {
                           try {
                               val fields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                               val req = FetchPlaceRequest.builder(placeId, fields).build()
                               val place = placesClient.fetchPlace(req).await().place
                               val ll = place.latLng
                               if (ll != null) {
                                   searchCompleted = true
                                   hedef = LatLng(ll.latitude, ll.longitude)
                                   rotalarGosterilsin = false
                                   bottomSheetAcik = false
                                   suggestionsVisible = false
                                   place.name?.let { searchText = it }
                               }
                           } catch (e: Exception) {
                               hataMesajı = "Konum alınamadı"
                               Log.e("PLACES", "fetch place error", e)
                           }
                       }
                   }

                   if (benimKonumum == null) {
                       Box(
                           modifier = Modifier
                               .fillMaxSize(),
                           contentAlignment = Alignment.Center
                       ){
                           Column(
                               horizontalAlignment = Alignment.CenterHorizontally
                           ) {
                               Text("Konum alınıyor",fontSize = 20.sp)

                               Spacer(Modifier.height(8.dp))

                               LoadingDots()
                           }

                       }
                   } else {

                       Box(modifier = modifier.fillMaxSize()) {

                           val seciliRota = rotalar.firstOrNull { it.ulasimModları == seciliUlasimModları }


                           GoogleMap(
                               modifier = Modifier.fillMaxSize(),
                               cameraPositionState = cameraPositionState,
                               onMapClick = { tiklanan ->
                                   suggestionsVisible = false
                                   hedef = tiklanan
                                   rotalarGosterilsin = false
                                   bottomSheetAcik = false

                               }
                           ) {
                               benimKonumum?.let {
                                   Marker(state = MarkerState(it), title = "Bulunduğum konum")
                               }
                               hedef?.let {
                                   Marker(state = MarkerState(it), title = "Hedef nokta")
                               }
                               seciliRota?.polylineNoktalari?.let { encoded ->
                                   Polyline(points = decodePolyline(encoded))
                               }
                           }




                           hataMesajı?.let { mesaj ->
                               Box(
                                   modifier = Modifier
                                       .fillMaxSize()
                                       .background(
                                           androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)
                                       ),
                                   contentAlignment = Alignment.Center
                               ) {
                                   Column(
                                       modifier = Modifier
                                           .fillMaxWidth(0.85f)
                                           .background(
                                               color = androidx.compose.ui.graphics.Color.White,
                                               shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                           )
                                           .padding(16.dp),
                                       horizontalAlignment = Alignment.CenterHorizontally
                                   ) {


                                       Text(
                                           text = mesaj,
                                           modifier = Modifier.padding(bottom = 12.dp)
                                       )


                                       Button(
                                           onClick = { hataMesajı = null },
                                           modifier = Modifier.fillMaxWidth()
                                       ) {
                                           Text("Tamam")
                                       }
                                   }
                               }
                           }


                           MapTopControls(
                               query = searchText,
                               onQueryChange = {
                                   searchText = it
                                   searchCompleted = false
                                   suggestionsVisible = true },
                               onSearch = {
                                   scope.launch {
                                       searchCompleted = true
                                       suggestionsVisible = false
                                       focusManager.clearFocus()
                                       val latLng = searchLocationSuspend(context, searchText)
                                       if (latLng != null) {
                                           hedef = latLng
                                           rotalarGosterilsin = false
                                           bottomSheetAcik = false
                                       } else {
                                           hataMesajı = "Sonuç bulunamadı"
                                       }
                                   }
                               },
                               onShowRoutes = {
                                   rotalarGosterilsin = true
                                   bottomSheetAcik = true
                                   suggestionsVisible = false
                               },
                               suggestions = suggestions,
                               suggestionsVisible = suggestionsVisible,
                               onSuggestionClick = { placeId -> selectSuggestion(placeId) },
                               searchCompleted = searchCompleted,

                           )



                           if (bottomSheetAcik && rotalarGosterilsin) {
                               ModalBottomSheet(
                                   onDismissRequest = { bottomSheetAcik = false },
                                   sheetState = bottomSheetState
                               ) {

                                   Column(

                                       modifier = Modifier
                                           .fillMaxWidth()
                                           .statusBarsPadding()
                                           .padding(top = 2.dp, bottom = 6.dp)
                                   ) {
                                       Text("Ulaşım Seçenekleri", modifier = Modifier.padding(bottom = 8.dp))

                                       rotalar.forEach { r ->
                                           Button(
                                               onClick = { seciliUlasimModları = r.ulasimModları },
                                               modifier = Modifier.fillMaxWidth()
                                           ) {
                                               val seciliMi = r.ulasimModları == seciliUlasimModları
                                               Text("${if (seciliMi) "✅ " else ""}${r.ulasimModları.etiket}: ${r.sureYazisi ?: "-"} • ${r.mesafeYazisi ?: "-"}")
                                           }
                                           Spacer(Modifier.height(6.dp))
                                       }

                                       Button(
                                           onClick = { bottomSheetAcik = false },
                                           modifier = Modifier.fillMaxWidth()
                                       ) { Text("Kapat") }
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
@Composable
fun LoadingDots() {
    val dots = listOf("•", "••", "•••")
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            index = (index + 1) % dots.size
        }
    }
    Text(dots[index])
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTopControls(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onShowRoutes: () -> Unit,
    suggestions: List<Triple<String, String, String>>,
    suggestionsVisible: Boolean,
    onSuggestionClick: (String) -> Unit,
    searchCompleted: Boolean,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 2.dp, bottom = 6.dp)
    ) {

        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = if (searchCompleted) 0.dp else 6.dp,
            color = if (searchCompleted)
                Color.White.copy(alpha = 0.75f)
            else
                MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (searchCompleted) {
                        shadowElevation = 0f
                    }
                }
        ) {
            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(10.dp))

                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Hedef konum ara") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onSearch()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(10.dp))

                    FilledTonalButton(
                        onClick = onSearch,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Ara")
                    }
                }


                if (suggestionsVisible && suggestions.isNotEmpty()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Black.copy(alpha = 0.08f))
                    )

                    Column(
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        suggestions.take(6).forEach { (primary, secondary, placeId) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionClick(placeId) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(primary)
                                    if (secondary.isNotBlank()) {
                                        Text(
                                            secondary,
                                            color = Color.Black.copy(alpha = 0.60f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ElevatedButton(
                onClick = onShowRoutes,
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Default.Route, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rotalar")
            }
        }
    }
}



private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }




suspend fun searchLocationSuspend(
    context: Context,
    query: String
): LatLng? = withContext(Dispatchers.IO) {
    if (query.isBlank()) return@withContext null
    try {
        val geocoder = Geocoder(context)
        val results = geocoder.getFromLocationName(query.trim(), 1)
        if (!results.isNullOrEmpty()) {
            val a = results[0]
            LatLng(a.latitude, a.longitude)
        } else null
    } catch (e: Exception) {
        Log.e("SEARCH", "Geocoder error", e)
        null
    }
}




