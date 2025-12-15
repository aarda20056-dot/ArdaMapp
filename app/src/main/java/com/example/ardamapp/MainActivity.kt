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
                   val izmir = LatLng(38.4189, 27.1305)
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
                hedef?.let {
                    Marker (
                        state = MarkerState(it),
                        title = "Hedef"
                    )
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