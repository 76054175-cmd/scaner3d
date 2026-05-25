package com.conti.scaner3d.PantallasOperacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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

    // Variables para el simulador de progreso del escaneo
    var escaneando by remember { mutableStateOf(false) }
    var progresoEscaneo by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // ---------------------------------------------------------------
    // HISTORIAL EN TIEMPO REAL PARA LOS GRÁFICOS
    // ---------------------------------------------------------------
    val maxPuntosGraphed = 50 // Cuántos puntos se muestran horizontalmente en pantalla

    val historialAcelerometro = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
    val historialGiroscopio = remember { mutableStateListOf<Triple<Float, Float, Float>>() }

    // Variables numéricas de texto instantáneo
    var accX by remember { mutableStateOf(0f) }
    var accY by remember { mutableStateOf(0f) }
    var accZ by remember { mutableStateOf(0f) }

    var gyroX by remember { mutableStateOf(0f) }
    var gyroY by remember { mutableStateOf(0f) }
    var gyroZ by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val giroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        accX = event.values[0]
                        accY = event.values[1]
                        accZ = event.values[2]

                        // Añadir al historial y recortar excedente
                        historialAcelerometro.add(Triple(accX, accY, accZ))
                        if (historialAcelerometro.size > maxPuntosGraphed) {
                            historialAcelerometro.removeAt(0)
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        gyroX = event.values[0]
                        gyroY = event.values[1]
                        gyroZ = event.values[2]

                        // Añadir al historial y recortar excedente
                        historialGiroscopio.add(Triple(gyroX, gyroY, gyroZ))
                        if (historialGiroscopio.size > maxPuntosGraphed) {
                            historialGiroscopio.removeAt(0)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Registrar oyentes usando SENSOR_DELAY_UI para actualizaciones fluidas pero eficientes
        acelerometro?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        giroscopio?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    // ---------------------------------------------------------------

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

                    Text(
                        text = "Realizando Escaneo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!escaneando) {
                                escaneando = true
                                progresoEscaneo = 0
                                coroutineScope.launch {
                                    for (i in 1..100) {
                                        progresoEscaneo = i
                                        delay(40)
                                    }
                                    escaneando = false
                                }
                            }
                        },
                        enabled = !escaneando,
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
                                color = Color(0xFF2E7D32),
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

            // TARJETA 2: Gráficos de los Sensores en Tiempo Real
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // --- SECCIÓN 1: ACELERÓMETRO ---
                    Text("Acelerómetro (m/s²)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(
                        text = String.format("X: %.2f  |  Y: %.2f  |  Z: %.2f", accX, accY, accZ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Render del Gráfico Nativo del Acelerómetro
                    GraficoSensorNativo(puntos = historialAcelerometro, rangoMax = 15f, maxPuntos = maxPuntosGraphed)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 2: GIROSCOPIO ---
                    Text("Giroscopio (rad/s)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(
                        text = String.format("X: %.2f  |  Y: %.2f  |  Z: %.2f", gyroX, gyroY, gyroZ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Render del Gráfico Nativo del Giroscopio
                    GraficoSensorNativo(puntos = historialGiroscopio, rangoMax = 6f, maxPuntos = maxPuntosGraphed)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Leyenda identificadora de colores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("● Eje X (Rojo)   ", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("● Eje Y (Verde)   ", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("● Eje Z (Azul)", color = Color(0xFF1E88E5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// COMPOSABLE COMPLEMENTARIO: COMPONENTE DE DIBUJO DIRECTO POR CANVAS (SIN LIBRERÍAS)
// ---------------------------------------------------------------------------------
@Composable
fun GraficoSensorNativo(
    puntos: List<Triple<Float, Float, Float>>,
    rangoMax: Float,
    maxPuntos: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9F9F9))
    ) {
        val anchoTotal = size.width
        val altoTotal = size.height
        val centroY = altoTotal / 2f
        val distanciaEntrePuntos = anchoTotal / (maxPuntos - 1)
        val escalaY = (altoTotal / 2f) / rangoMax

        // Dibujar línea guía central (Cero absoluto)
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(0f, centroY),
            end = Offset(anchoTotal, centroY),
            strokeWidth = 2f
        )

        // Pintar líneas solo si hay suficiente historial acumulado para interconectarlos
        if (puntos.size > 1) {
            for (i in 0 until puntos.size - 1) {
                val x1 = i * distanciaEntrePuntos
                val x2 = (i + 1) * distanciaEntrePuntos

                // Trazo Eje X (Línea Roja)
                drawLine(
                    color = Color(0xFFE53935),
                    start = Offset(x1, centroY - (puntos[i].first * escalaY)),
                    end = Offset(x2, centroY - (puntos[i + 1].first * escalaY)),
                    strokeWidth = 4f
                )

                // Trazo Eje Y (Línea Verde)
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = Offset(x1, centroY - (puntos[i].second * escalaY)),
                    end = Offset(x2, centroY - (puntos[i + 1].second * escalaY)),
                    strokeWidth = 4f
                )

                // Trazo Eje Z (Línea Azul)
                drawLine(
                    color = Color(0xFF1E88E5),
                    start = Offset(x1, centroY - (puntos[i].third * escalaY)),
                    end = Offset(x2, centroY - (puntos[i + 1].third * escalaY)),
                    strokeWidth = 4f
                )
            }
        }
    }
}

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