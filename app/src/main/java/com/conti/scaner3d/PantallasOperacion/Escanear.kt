package com.conti.scaner3d.PantallasOperacion

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.conti.scaner3d.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearScreen(
    onNavigate: (String) -> Unit = {}
) {
    val selectedItem = 1
    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.BookmarkBorder, Icons.Default.Person)

    // Variables para el permiso de la cámara
    val context = LocalContext.current
    var tienePermisoCamara by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val pedirPermisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> tienePermisoCamara = isGranted }

    LaunchedEffect(Unit) {
        if (!tienePermisoCamara) {
            pedirPermisoLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ---------------------------------------------------------------
    // NUEVAS VARIABLES PARA EL PROGRESO DEL ESCANEO
    // ---------------------------------------------------------------
    var escaneando by remember { mutableStateOf(false) }
    var progresoEscaneo by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanner 3D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(bottomNavIcons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            if (item != "Escanear") {
                                onNavigate(item)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2), selectedTextColor = Color(0xFF1976D2),
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TARJETA 1: Cámara y Control de Escaneo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Vista de Cámara
                    if (tienePermisoCamara) {
                        VistaCamara(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Permiso de cámara requerido", color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { pedirPermisoLauncher.launch(Manifest.permission.CAMERA) }) {
                                    Text("Otorgar Permiso")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Título de la sección
                    Text(
                        text = "Realizando Escaneo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ¡NUEVO!: BOTÓN PARA ESCANEAR
                    Button(
                        onClick = {
                            if (!escaneando) {
                                escaneando = true
                                progresoEscaneo = 0
                                // Ejecuta el bucle del 1% al 100% en segundo plano
                                coroutineScope.launch {
                                    for (i in 1..100) {
                                        progresoEscaneo = i
                                        delay(40) // Ajusta el tiempo (40ms por número = aprox 4 segundos totales)
                                    }
                                    escaneando = false
                                }
                            }
                        },
                        enabled = !escaneando, // Deshabilitar el botón mientras escanea para evitar bugs
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (progresoEscaneo == 100) "Volver a Escanear" else "Escanear",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ¡NUEVO!: MENSAJE DINÁMICO DE PROGRESO DEBAJO DEL BOTÓN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (escaneando) {
                            Text(
                                text = "Escaneando: $progresoEscaneo%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (progresoEscaneo == 100) {
                            Text(
                                text = "¡Escaneo completado con éxito! (100%)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF2E7D32), // Color verde de éxito
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Listo para escanear",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // TARJETA 2: Sensores (Acelerómetro y Giroscopio)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(painter = painterResource(id = R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acelerometro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("+2.3m/s^2 X, +2.3m/s^2 Y, +2.3m/s^2 Z", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Giroscopio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("30° X, 50° Y, 80° Z", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

// COMPONENTE DE CÁMARA (Se mantiene intacto)
@Composable
fun VistaCamara(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (exc: Exception) {
                    Log.e("CameraX", "Fallo al iniciar la cámara", exc)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}