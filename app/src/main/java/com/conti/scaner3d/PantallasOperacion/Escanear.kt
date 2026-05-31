package com.conti.scaner3d.PantallasOperacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import com.conti.scaner3d.baseDatosLocal.Escaneo3D
import com.conti.scaner3d.baseDatosLocal.EscaneoDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearScreen(
    escaneoDao: EscaneoDao,
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
        if (!tienePermisoCamara) pedirPermisoLauncher.launch(Manifest.permission.CAMERA)
    }

    // Controladores de estado
    var escaneando by remember { mutableStateOf(false) }
    var progresoEscaneo by remember { mutableIntStateOf(0) }
    var pantallaCompleta by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    // Sensores
    val maxPuntosGraphed = 50
    val historialAcelerometro = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
    val historialGiroscopio = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
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
                        historialAcelerometro.add(Triple(accX, accY, accZ))
                        if (historialAcelerometro.size > maxPuntosGraphed) historialAcelerometro.removeAt(0)
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        gyroX = event.values[0]
                        gyroY = event.values[1]
                        gyroZ = event.values[2]
                        historialGiroscopio.add(Triple(gyroX, gyroY, gyroZ))
                        if (historialGiroscopio.size > maxPuntosGraphed) historialGiroscopio.removeAt(0)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        acelerometro?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        giroscopio?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    if (pantallaCompleta) {
        // --- VISTA PANTALLA COMPLETA ---
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (tienePermisoCamara) {
                VistaCamara(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptureReady = { imageCaptureUseCase = it }
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ESCANEANDO ENTORNO...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progresoEscaneo / 100f }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), color = Color(0xFF1976D2), trackColor = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$progresoEscaneo%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    } else {
        // --- VISTA NORMAL ---
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
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
                                if (item != "Escanear") onNavigate(item)
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
                modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (tienePermisoCamara) {
                            VistaCamara(
                                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                                onImageCaptureReady = { imageCaptureUseCase = it }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.DarkGray))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (!escaneando && imageCaptureUseCase != null) {
                                    escaneando = true
                                    pantallaCompleta = true
                                    progresoEscaneo = 0
                                    coroutineScope.launch {
                                        for (i in 1..100) {
                                            progresoEscaneo = i
                                            delay(30)
                                        }

                                        // 100% ALCANZADO: TOMAR Y GUARDAR LA FOTO
                                        val photoFile = File(context.cacheDir, "escaneo_${System.currentTimeMillis()}.jpg")
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                        imageCaptureUseCase?.takePicture(
                                            outputOptions,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                    val savedUri = Uri.fromFile(photoFile).toString()
                                                    coroutineScope.launch {
                                                        val fechaActual = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(Date())
                                                        val nuevoEscaneo = Escaneo3D(
                                                            nombre = "Modelo Escaneado",
                                                            fecha = fechaActual,
                                                            imagenUri = savedUri // Guardamos la ruta de la foto en ROOM
                                                        )
                                                        escaneoDao.insertar(nuevoEscaneo)
                                                        Toast.makeText(context, "Escaneo guardado con imagen", Toast.LENGTH_SHORT).show()
                                                        escaneando = false
                                                        pantallaCompleta = false
                                                    }
                                                }
                                                override fun onError(exc: ImageCaptureException) {
                                                    coroutineScope.launch {
                                                        Toast.makeText(context, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                                                        escaneando = false
                                                        pantallaCompleta = false
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else if (imageCaptureUseCase == null) {
                                    Toast.makeText(context, "La cámara aún se está inicializando", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Iniciar Escaneo 3D", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Gráficos de Sensores
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Acelerómetro", fontWeight = FontWeight.Bold)
                        GraficoSensorNativo(puntos = historialAcelerometro, rangoMax = 15f, maxPuntos = maxPuntosGraphed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Giroscopio", fontWeight = FontWeight.Bold)
                        GraficoSensorNativo(puntos = historialGiroscopio, rangoMax = 6f, maxPuntos = maxPuntosGraphed)
                    }
                }
            }
        }
    }
}

@Composable
fun GraficoSensorNativo(puntos: List<Triple<Float, Float, Float>>, rangoMax: Float, maxPuntos: Int) {
    Canvas(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF9F9F9))) {
        val anchoTotal = size.width
        val altoTotal = size.height
        val centroY = altoTotal / 2f
        val escalaY = (altoTotal / 2f) / rangoMax
        val distancia = anchoTotal / (maxPuntos - 1)

        drawLine(Color(0xFFE0E0E0), Offset(0f, centroY), Offset(anchoTotal, centroY), 2f)
        if (puntos.size > 1) {
            for (i in 0 until puntos.size - 1) {
                val x1 = i * distancia
                val x2 = (i + 1) * distancia
                drawLine(Color(0xFFE53935), Offset(x1, centroY - (puntos[i].first * escalaY)), Offset(x2, centroY - (puntos[i + 1].first * escalaY)), 4f)
                drawLine(Color(0xFF4CAF50), Offset(x1, centroY - (puntos[i].second * escalaY)), Offset(x2, centroY - (puntos[i + 1].second * escalaY)), 4f)
                drawLine(Color(0xFF1E88E5), Offset(x1, centroY - (puntos[i].third * escalaY)), Offset(x2, centroY - (puntos[i + 1].third * escalaY)), 4f)
            }
        }
    }
}

@Composable
fun VistaCamara(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // Construir capturador de imágenes de CameraX
                val imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    onImageCaptureReady(imageCapture)
                } catch (exc: Exception) {
                    Log.e("CameraX", "Error", exc)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}