package com.conti.scaner3d.PantallasOperacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.conti.scaner3d.baseDatosLocal.Escaneo3D
import com.conti.scaner3d.baseDatosLocal.EscaneoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Estructuras de datos para el muestreo
data class MuestraLinea(val yRelativo: Float, val inicioX: Float, val longitud: Float)
data class MuestraAngulo(val angulo: Int, val lineas: List<MuestraLinea>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearScreen(
    escaneoDao: EscaneoDao,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permisos
    var tienePermisoCamara by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedirPermisoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tienePermisoCamara = it }

    // ESTADOS DE CONFIGURACIÓN Y PROCESO
    var faseEscaneo by remember { mutableStateOf("CONFIGURACION") } // CONFIGURACION, CALIBRACION, CAPTURA, FINALIZADO
    var precisionAngular by remember { mutableIntStateOf(20) }
    var resolucionSecantes by remember { mutableIntStateOf(100) }
    var colorFondoRef by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    
    // ESTADOS DE CAPTURA
    var anguloActual by remember { mutableIntStateOf(0) }
    var muestrasCapturadas = remember { mutableStateListOf<MuestraAngulo>() }
    var procesandoImagen by remember { mutableStateOf(false) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    val totalFotos = 360 / precisionAngular

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner 3D - Silhouette", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor(0xFF1976D2), titleContentColor = ComposeColor.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = ComposeColor.White) {
                val items = listOf("Inicio", "Escanear", "Historial")
                val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.History)
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = item == "Escanear",
                        onClick = { if (item != "Escanear") onNavigate(item) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (faseEscaneo) {
                "CONFIGURACION" -> {
                    ConfiguracionInicial(
                        precision = precisionAngular,
                        onPrecisionChange = { precisionAngular = it },
                        resolucion = resolucionSecantes,
                        onResolucionChange = { resolucionSecantes = it },
                        onStart = { faseEscaneo = "CALIBRACION" }
                    )
                }
                "CALIBRACION" -> {
                    CalibracionFondo(
                        procesando = procesandoImagen,
                        tienePermiso = tienePermisoCamara,
                        onCalibrate = {
                            procesandoImagen = true
                            capturarYCalibrarColor(context, imageCaptureUseCase, onResult = { color ->
                                colorFondoRef = color
                                faseEscaneo = "CAPTURA"
                                procesandoImagen = false
                            }, onError = {
                                Toast.makeText(context, "Error en calibración", Toast.LENGTH_SHORT).show()
                                procesandoImagen = false
                            })
                        },
                        onImageCaptureReady = { imageCaptureUseCase = it }
                    )
                }
                "CAPTURA" -> {
                    CapturaPorAngulos(
                        anguloActual = anguloActual,
                        totalFotos = totalFotos,
                        precision = precisionAngular,
                        procesando = procesandoImagen,
                        tienePermiso = tienePermisoCamara,
                        colorReferencia = colorFondoRef,
                        onCapture = {
                            procesandoImagen = true
                            capturarYProcesar(
                                context, imageCaptureUseCase, anguloActual, resolucionSecantes, colorFondoRef!!,
                                onResult = { muestra ->
                                    muestrasCapturadas.add(muestra)
                                    if (anguloActual + precisionAngular >= 360) {
                                        coroutineScope.launch {
                                            guardarResultadoFinal(context, muestrasCapturadas, precisionAngular, resolucionSecantes, escaneoDao)
                                            faseEscaneo = "FINALIZADO"
                                            procesandoImagen = false
                                        }
                                    } else {
                                        anguloActual += precisionAngular
                                        procesandoImagen = false
                                    }
                                },
                                onError = {
                                    Toast.makeText(context, "Error en captura", Toast.LENGTH_SHORT).show()
                                    procesandoImagen = false
                                }
                            )
                        },
                        onImageCaptureReady = { imageCaptureUseCase = it }
                    )
                }
                "FINALIZADO" -> {
                    ResultadoFinal(onReset = {
                        faseEscaneo = "CONFIGURACION"
                        muestrasCapturadas.clear()
                        anguloActual = 0
                        colorFondoRef = null
                    }, onVerModelo = {
                        onNavigate("Historial") // O una ruta específica si se implementa
                    })
                }
            }
        }
    }
}

@Composable
fun ConfiguracionInicial(precision: Int, onPrecisionChange: (Int) -> Unit, resolucion: Int, onResolucionChange: (Int) -> Unit, onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("1. Configuración", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Precisión Angular: $precision°")
            Slider(value = precision.toFloat(), onValueChange = { onPrecisionChange(it.toInt()) }, valueRange = 5f..90f, steps = 16)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Resolución Vertical: $resolucion líneas")
            Slider(value = resolucion.toFloat(), onValueChange = { onResolucionChange(it.toInt()) }, valueRange = 20f..300f)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Siguiente: Calibrar Fondo")
            }
        }
    }
}

@Composable
fun CalibracionFondo(procesando: Boolean, tienePermiso: Boolean, onCalibrate: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("2. Calibración de Fondo", fontWeight = FontWeight.Bold, color = ComposeColor(0xFF1976D2))
            Text("Apunta al fondo vacío (sin el objeto)", fontSize = 14.sp, color = ComposeColor.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(ComposeColor.Black)) {
                if (tienePermiso) {
                    VistaCamara(modifier = Modifier.fillMaxSize(), onImageCaptureReady = onImageCaptureReady)
                    // Punto de mira central
                    Box(modifier = Modifier.size(20.dp).border(2.dp, ComposeColor.White, CircleShape).align(Alignment.Center))
                }
                if (procesando) {
                    Box(modifier = Modifier.fillMaxSize().background(ComposeColor.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ComposeColor.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCalibrate, enabled = !procesando, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Capturar Color de Fondo")
            }
        }
    }
}

@Composable
fun CapturaPorAngulos(anguloActual: Int, totalFotos: Int, precision: Int, procesando: Boolean, tienePermiso: Boolean, colorReferencia: Triple<Int, Int, Int>?, onCapture: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    val fotoNumero = (anguloActual / precision) + 1
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Paso $fotoNumero de $totalFotos", fontWeight = FontWeight.Bold, color = ComposeColor(0xFF1976D2))
                Spacer(modifier = Modifier.weight(1f))
                colorReferencia?.let { (r,g,b) ->
                    Box(modifier = Modifier.size(24.dp).background(ComposeColor(r, g, b), CircleShape).border(1.dp, ComposeColor.Gray, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fondo", fontSize = 10.sp)
                }
            }
            Text("Gira el objeto a: $anguloActual°", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(ComposeColor.Black)) {
                if (tienePermiso) VistaCamara(modifier = Modifier.fillMaxSize(), onImageCaptureReady = onImageCaptureReady)
                if (procesando) {
                    Box(modifier = Modifier.fillMaxSize().background(ComposeColor.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ComposeColor.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCapture, enabled = !procesando, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capturar Ángulo $anguloActual°")
            }
        }
    }
}

@Composable
fun ResultadoFinal(onReset: () -> Unit, onVerModelo: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ComposeColor(0xFF4CAF50), modifier = Modifier.size(100.dp))
        Text("¡Escaneo Completado!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onVerModelo, modifier = Modifier.fillMaxWidth()) {
            Text("Ver en Historial")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Nuevo Escaneo")
        }
    }
}

fun capturarYCalibrarColor(context: Context, imageCapture: ImageCapture?, onResult: (Triple<Int, Int, Int>) -> Unit, onError: () -> Unit) {
    if (imageCapture == null) { onError(); return }
    val file = File(context.cacheDir, "calibration.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                onResult(Triple(Color.red(pixel), Color.green(pixel), Color.blue(pixel)))
            } else onError()
        }
        override fun onError(exc: ImageCaptureException) = onError()
    })
}

fun capturarYProcesar(context: Context, imageCapture: ImageCapture?, angulo: Int, n: Int, colorFondo: Triple<Int, Int, Int>, onResult: (MuestraAngulo) -> Unit, onError: () -> Unit) {
    if (imageCapture == null) { onError(); return }
    val file = File(context.cacheDir, "temp_$angulo.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                onResult(extraerMuestrasSilueta(bitmap, angulo, n, colorFondo))
            } else onError()
        }
        override fun onError(exc: ImageCaptureException) = onError()
    })
}

fun extraerMuestrasSilueta(bitmap: Bitmap, angulo: Int, n: Int, colorFondo: Triple<Int, Int, Int>): MuestraAngulo {
    val (refR, refG, refB) = colorFondo
    val width = bitmap.width
    val height = bitmap.height
    val lineas = mutableListOf<MuestraLinea>()
    val umbralTolerancia = 50.0

    for (i in 0 until n) {
        val yRelativo = i.toFloat() / n
        val yPixel = (yRelativo * height).toInt().coerceIn(0, height - 1)
        var xInicio = -1
        var xFin = -1
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, yPixel)
            val diff = Math.sqrt(
                Math.pow((Color.red(pixel) - refR).toDouble(), 2.0) +
                Math.pow((Color.green(pixel) - refG).toDouble(), 2.0) +
                Math.pow((Color.blue(pixel) - refB).toDouble(), 2.0)
            )
            if (diff > umbralTolerancia) {
                if (xInicio == -1) xInicio = x
                xFin = x
            }
        }
        if (xInicio != -1) {
            lineas.add(MuestraLinea(yRelativo, xInicio.toFloat() / width, (xFin - xInicio).toFloat() / width))
        }
    }
    return MuestraAngulo(angulo, lineas)
}

suspend fun guardarResultadoFinal(context: Context, muestras: List<MuestraAngulo>, precision: Int, resolucion: Int, escaneoDao: EscaneoDao) = withContext(Dispatchers.IO) {
    try {
        val jsonRoot = JSONObject().apply {
            put("precision_angular", precision)
            put("resolucion_secantes_n", resolucion)
            val array = JSONArray()
            for (m in muestras) {
                val obj = JSONObject().apply {
                    put("angulo", m.angulo)
                    val lines = JSONArray()
                    for (l in m.lineas) {
                        lines.put(JSONObject().apply {
                            put("y_relativo", String.format("%.3f", l.yRelativo).toDouble())
                            put("inicio_x", String.format("%.3f", l.inicioX).toDouble())
                            put("longitud", String.format("%.3f", l.longitud).toDouble())
                        })
                    }
                    put("lineas", lines)
                }
                array.put(obj)
            }
            put("muestras", array)
        }
        val fileName = "escaneo_${System.currentTimeMillis()}.json"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { it.write(jsonRoot.toString().toByteArray()) }
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        escaneoDao.insertar(Escaneo3D(nombre = "Modelo 3D ($precision°)", fecha = fecha, imagenUri = file.absolutePath))
    } catch (e: Exception) { Log.e("Scanner3D", "Error", e) }
}

@Composable
fun VistaCamara(modifier: Modifier = Modifier, onImageCaptureReady: (ImageCapture) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                onImageCaptureReady(imageCapture)
            } catch (exc: Exception) { Log.e("CameraX", "Error", exc) }
        }, ContextCompat.getMainExecutor(ctx))
        previewView
    }, modifier = modifier)
}
