package com.example.intentexample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.intentexample.ui.theme.IntentExampleTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntentExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SearchScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var textoBusca by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Cliente de localização do Google
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Launcher que abre o popup de permissão para o usuário
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            // Permissão aceita → busca a localização e abre o Maps
            buscarLocalizacaoEAbrir(context, fusedLocationClient, textoBusca)
        } else {
            Toast.makeText(context, "Permissão de localização negada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun onBuscarClick() {
        if (textoBusca.trim().isEmpty()) {
            Toast.makeText(context, "Digite algo para buscar!", Toast.LENGTH_SHORT).show()
            return
        }

        // Verifica se já tem permissão concedida
        val temPermissao = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (temPermissao) {
            buscarLocalizacaoEAbrir(context, fusedLocationClient, textoBusca)
        } else {
            // Pede a permissão ao usuário
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Buscar no Google Maps", fontSize = 22.sp)

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = textoBusca,
            onValueChange = { textoBusca = it },
            label = { Text("Ex: Posto de Gasolina, Farmácia...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onBuscarClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar")
        }

        if (statusMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = statusMsg, fontSize = 14.sp)
        }
    }
}

fun buscarLocalizacaoEAbrir(
    context: android.content.Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    textoBusca: String
) {
    try {
        // Pega a última localização conhecida do aparelho
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val buscaFormatada = textoBusca.trim().replace(" ", "+")
                    val lat = location.latitude
                    val lng = location.longitude

                    // Monta URL com coordenadas — força o Maps a buscar perto de você
                    val url = "https://www.google.com/maps/search/$buscaFormatada/@$lat,$lng,14z"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)

                } else {
                    // Localização nula — fallback sem coordenadas
                    Toast.makeText(context, "Localização indisponível, buscando sem ela...", Toast.LENGTH_SHORT).show()
                    val buscaFormatada = textoBusca.trim().replace(" ", "+")
                    val url = "https://www.google.com.br/maps/search/$buscaFormatada"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao obter localização!", Toast.LENGTH_SHORT).show()
            }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Permissão de localização não concedida!", Toast.LENGTH_SHORT).show()
    }
}

// Preview — só para visualizar no Android Studio, não afeta o app
@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    IntentExampleTheme {
        SearchScreen()
    }
}