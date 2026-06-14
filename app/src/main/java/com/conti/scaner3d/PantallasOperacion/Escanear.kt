package com.conti.scaner3d.PantallasOperacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.sqrt

// Estructuras de datos
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

    // Permisos
    var tienePermisoCamara by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedirPermisoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tienePermisoCamara = it }

    // ESTADOS DE CONFIGURACIÓN
    var faseEscaneo by remember { mutableStateOf("CONFIGURACION") } 
    var precisionAngular by remember { mutableIntStateOf(20) }
    var resolucionSecantes by remember { mutableIntStateOf(100) }
    var escalaHorizontal by remember { mutableFloatStateOf(1.0f) }
    var escalaVertical by remember { mutableFloatStateOf(1.0f) }
    
    var colorFondoRef by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var umbralTolerancia by remember { mutableFloatStateOf(60f) }
    
    // ESTADOS DE CAPTURA
    var anguloActual by remember { mutableIntStateOf(0) }
    val muestrasCapturadas = remember { mutableStateListOf<MuestraAngulo>() }
    var procesandoImagen by remember { mutableStateOf(false) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    
    // ESTADO DE REVISIÓN
    var ultimoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Manejo del botón atrás nativo
    BackHandler(faseEscaneo != "CONFIGURACION") {
        when (faseEscaneo) {
            "CALIBRACION" -> faseEscaneo = "CONFIGURACION"
            "CAPTURA" -> {
                if (anguloActual == 0) faseEscaneo = "CALIBRACION"
                else {
                    // Confirmación para salir?
                    faseEscaneo = "CONFIGURACION"
                    muestrasCapturadas.clear()
                    anguloActual = 0
                }
            }
            "REVISION" -> faseEscaneo = "CAPTURA"
            "FINALIZADO" -> faseEscaneo = "CONFIGURACION"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner 3D Profesional", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    if (faseEscaneo != "CONFIGURACION") {
                        IconButton(onClick = { 
                             if (faseEscaneo == "CAPTURA" && anguloActual > 0) {
                                 faseEscaneo = "CONFIGURACION"
                                 muestrasCapturadas.clear()
                                 anguloActual = 0
                             } else if (faseEscaneo == "REVISION") {
                                 faseEscaneo = "CAPTURA"
                             } else if (faseEscaneo == "CALIBRACION") {
                                 faseEscaneo = "CONFIGURACION"
                             } else {
                                 faseEscaneo = "CONFIGURACION"
                             }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor(0xFF1976D2), titleContentColor = ComposeColor.White, navigationIconContentColor = ComposeColor.White)
            )
        },
        bottomBar = {
            if (faseEscaneo == "CONFIGURACION" || faseEscaneo == "FINALIZADO") {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
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
                        eHorizontal = escalaHorizontal,
                        onEHorizontalChange = { escalaHorizontal = it },
                        eVertical = escalaVertical,
                        onEVerticalChange = { escalaVertical = it },
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
                                Toast.makeText(context, "Error calibración", Toast.LENGTH_SHORT).show()
                                procesandoImagen = false
                            })
                        },
                        onImageCaptureReady = { imageCaptureUseCase = it }
                    )
                }
                "CAPTURA" -> {
                    CapturaPorAngulos(
                        anguloActual = anguloActual,
                        totalFotos = 360 / precisionAngular,
                        precision = precisionAngular,
                        procesando = procesandoImagen,
                        tienePermiso = tienePermisoCamara,
                        onCapture = {
                            procesandoImagen = true
                            capturarOriginal(context, imageCaptureUseCase, onResult = { bitmap ->
                                ultimoBitmap = bitmap
                                faseEscaneo = "REVISION"
                                procesandoImagen = false
                            }, onError = {
                                Toast.makeText(context, "Error captura", Toast.LENGTH_SHORT).show()
                                procesandoImagen = false
                            })
                        },
                        onCancel = {
                            faseEscaneo = "CONFIGURACION"
                            muestrasCapturadas.clear()
                            anguloActual = 0
                        },
                        onImageCaptureReady = { imageCaptureUseCase = it }
                    )
                }
                "REVISION" -> {
                    RevisionPipeline(
                        bitmap = ultimoBitmap!!,
                        colorFondo = colorFondoRef!!,
                        umbral = umbralTolerancia,
                        onUmbralChange = { umbralTolerancia = it },
                        resolucion = resolucionSecantes,
                        angulo = anguloActual,
                        onRetake = { faseEscaneo = "CAPTURA" },
                        onConfirm = { muestra ->
                            muestrasCapturadas.add(muestra)
                            if (anguloActual + precisionAngular >= 360) {
                                coroutineScope.launch {
                                    procesandoImagen = true
                                    guardarResultadoFinal(context, muestrasCapturadas, precisionAngular, resolucionSecantes, escalaHorizontal, escalaVertical, escaneoDao)
                                    faseEscaneo = "FINALIZADO"
                                    procesandoImagen = false
                                }
                            } else {
                                anguloActual += precisionAngular
                                faseEscaneo = "CAPTURA"
                            }
                        }
                    )
                }
                "FINALIZADO" -> {
                    ResultadoFinal(onReset = {
                        faseEscaneo = "CONFIGURACION"
                        muestrasCapturadas.clear()
                        anguloActual = 0
                    }, onVerModelo = { onNavigate("Historial") })
                }
            }
        }
    }
}

@Composable
fun ConfiguracionInicial(
    precision: Int, onPrecisionChange: (Int) -> Unit, 
    resolucion: Int, onResolucionChange: (Int) -> Unit,
    eHorizontal: Float, onEHorizontalChange: (Float) -> Unit,
    eVertical: Float, onEVerticalChange: (Float) -> Unit,
    onStart: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("Ajustes de Escaneo", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ComposeColor(0xFF1976D2))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Precisión Angular: $precision°", fontWeight = FontWeight.Medium)
            Slider(value = precision.toFloat(), onValueChange = { onPrecisionChange(it.toInt()) }, valueRange = 5f..90f, steps = 16)
            
            Text("Resolución Vertical: $resolucion líneas", fontWeight = FontWeight.Medium)
            Slider(value = resolucion.toFloat(), onValueChange = { onResolucionChange(it.toInt()) }, valueRange = 20f..300f)
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Escala Horizontal: ${String.format("%.2f", eHorizontal)}", fontWeight = FontWeight.Medium)
            Slider(value = eHorizontal, onValueChange = onEHorizontalChange, valueRange = 0.5f..3.0f)
            
            Text("Escala Vertical (Separación): ${String.format("%.2f", eVertical)}", fontWeight = FontWeight.Medium)
            Slider(value = eVertical, onValueChange = onEVerticalChange, valueRange = 0.5f..3.0f)
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Siguiente: Calibrar Fondo", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun RevisionPipeline(
    bitmap: Bitmap,
    colorFondo: Triple<Int, Int, Int>,
    umbral: Float,
    onUmbralChange: (Float) -> Unit,
    resolucion: Int,
    angulo: Int,
    onRetake: () -> Unit,
    onConfirm: (MuestraAngulo) -> Unit
) {
    var viewMode by remember { mutableIntStateOf(2) } // Por defecto mostrar Muestreo para validar rápido
    
    val result = remember(umbral, resolucion) {
        procesarPipeline(bitmap, colorFondo, umbral, resolucion, angulo)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Calibración del Pipeline ($angulo°)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val modes = listOf("Original", "Fondo", "Muestreo")
                    modes.forEachIndexed { index, name ->
                        FilterChip(
                            selected = viewMode == index,
                            onClick = { viewMode = index },
                            label = { Text(name, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(ComposeColor.Black)) {
                    val displayBitmap = when(viewMode) {
                        0 -> bitmap
                        1 -> result.bitmapFondo
                        else -> result.bitmapContorno
                    }
                    Image(bitmap = displayBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    
                    if (viewMode == 2) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            result.muestra.lineas.forEach { linea ->
                                val y = linea.yRelativo * size.height
                                drawLine(ComposeColor.Cyan.copy(alpha = 0.8f), Offset(linea.inicioX * size.width, y), Offset((linea.inicioX + linea.longitud) * size.width, y), 2f)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Umbral de Tolerancia: ${umbral.toInt()}", fontWeight = FontWeight.Bold, color = ComposeColor(0xFF1976D2))
                Slider(value = umbral, onValueChange = onUmbralChange, valueRange = 0f..300f) // Rango extendido a 300
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { 
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Repetir") 
                    }
                    Button(onClick = { onConfirm(result.muestra) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { 
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Confirmar") 
                    }
                }
            }
        }
    }
}

@Composable
fun CapturaPorAngulos(anguloActual: Int, totalFotos: Int, precision: Int, procesando: Boolean, tienePermiso: Boolean, onCapture: () -> Unit, onCancel: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    val fotoNumero = (anguloActual / precision) + 1
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PASO $fotoNumero DE $totalFotos", fontWeight = FontWeight.ExtraBold, color = ComposeColor(0xFF1976D2), fontSize = 14.sp)
            Text("Gira el objeto a: $anguloActual°", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(12.dp)).background(ComposeColor.Black)) {
                if (tienePermiso) VistaCamara(modifier = Modifier.fillMaxSize(), onImageCaptureReady = onImageCaptureReady)
                if (procesando) {
                    Box(modifier = Modifier.fillMaxSize().background(ComposeColor.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ComposeColor.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onCapture, enabled = !procesando, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.width(8.dp))
                Text("CAPTURAR", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
                Text("Cancelar Escaneo", color = ComposeColor.Red)
            }
        }
    }
}

@Composable
fun VistaCamara(modifier: Modifier = Modifier, onImageCaptureReady: (ImageCapture) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    Box(modifier = modifier) {
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
                } catch (e: Exception) { Log.e("Camera", "Error", e) }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.fillMaxSize())
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            // Eje central de giro (Rojo brillante)
            drawLine(ComposeColor.Red, Offset(centerX, 0f), Offset(centerX, size.height), 3f)
            
            // Guías de encuadre
            val guideColor = ComposeColor.White.copy(alpha = 0.7f)
            val padding = 60f
            // Superior
            drawLine(guideColor, Offset(centerX - 100, padding), Offset(centerX + 100, padding), 4f)
            // Inferior
            drawLine(guideColor, Offset(centerX - 100, size.height - padding), Offset(centerX + 100, size.height - padding), 4f)
            
            // Texto de ayuda
            // No podemos dibujar texto fácilmente en Canvas sin NativeCanvas, lo omitimos para mantener simplicidad
        }
        Text("ALINEA EL EJE CENTRAL", modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).background(ComposeColor.Black.copy(0.5f), RoundedCornerShape(4.dp)).padding(4.dp), color = ComposeColor.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

data class PipelineResult(val bitmapFondo: Bitmap, val bitmapContorno: Bitmap, val muestra: MuestraAngulo)

fun procesarPipeline(bitmap: Bitmap, colorFondo: Triple<Int, Int, Int>, umbral: Float, n: Int, angulo: Int): PipelineResult {
    val width = bitmap.width
    val height = bitmap.height
    val (refR, refG, refB) = colorFondo
    
    val fondoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val contornoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val lineas = mutableListOf<MuestraLinea>()

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val fondoPixels = IntArray(width * height)
    val contornoPixels = IntArray(width * height)

    val stepY = (height / n).coerceAtLeast(1)

    for (y in 0 until height) {
        var xMin = -1
        var xMax = -1
        for (x in 0 until width) {
            val idx = y * width + x
            val p = pixels[idx]
            val dr = (Color.red(p) - refR).toDouble()
            val dg = (Color.green(p) - refG).toDouble()
            val db = (Color.blue(p) - refB).toDouble()
            val diff = sqrt(dr*dr + dg*dg + db*db)

            if (diff > umbral) {
                fondoPixels[idx] = p 
                if (xMin == -1) xMin = x
                xMax = x
            } else {
                fondoPixels[idx] = Color.BLACK
            }
        }
        if (xMin != -1 && xMax != -1) {
            for (x in xMin..xMax) { contornoPixels[y * width + x] = Color.WHITE }
            if (y % stepY == 0) {
                lineas.add(MuestraLinea(y.toFloat() / height, xMin.toFloat() / width, (xMax - xMin).toFloat() / width))
            }
        }
    }
    fondoBitmap.setPixels(fondoPixels, 0, width, 0, 0, width, height)
    contornoBitmap.setPixels(contornoPixels, 0, width, 0, 0, width, height)
    return PipelineResult(fondoBitmap, contornoBitmap, MuestraAngulo(angulo, lineas))
}

suspend fun guardarResultadoFinal(
    context: Context, 
    muestras: List<MuestraAngulo>, 
    precision: Int, 
    resolucion: Int, 
    eH: Float, 
    eV: Float, 
    escaneoDao: EscaneoDao
) = withContext(Dispatchers.IO) {
    try {
        val jsonRoot = JSONObject().apply {
            put("precision_angular", precision)
            put("resolucion_secantes_n", resolucion)
            put("escala_horizontal", String.format("%.2f", eH).toDouble())
            put("escala_vertical", String.format("%.2f", eV).toDouble())
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

// ... Resto de funciones (capturarOriginal, capturarYCalibrarColor, ResultadoFinal, CalibracionFondo) se mantienen con UI mejorada ...

@Composable
fun CalibracionFondo(procesando: Boolean, tienePermiso: Boolean, onCalibrate: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CALIBRACIÓN", fontWeight = FontWeight.ExtraBold, color = ComposeColor(0xFF1976D2))
            Text("Apunta al fondo vacío sin el objeto", fontSize = 12.sp, color = ComposeColor.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(ComposeColor.Black)) {
                if (tienePermiso) {
                    VistaCamara(modifier = Modifier.fillMaxSize(), onImageCaptureReady = onImageCaptureReady)
                    Box(modifier = Modifier.size(24.dp).border(3.dp, ComposeColor.White, CircleShape).align(Alignment.Center))
                }
                if (procesando) {
                    Box(modifier = Modifier.fillMaxSize().background(ComposeColor.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ComposeColor.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCalibrate, enabled = !procesando, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("CAPTURAR COLOR FONDO")
            }
        }
    }
}

@Composable
fun ResultadoFinal(onReset: () -> Unit, onVerModelo: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudDone, null, tint = ComposeColor(0xFF4CAF50), modifier = Modifier.size(120.dp))
        Text("¡PROCESO FINALIZADO!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = ComposeColor(0xFF1976D2))
        Text("El modelo ha sido exportado a JSON.", textAlign = TextAlign.Center, color = ComposeColor.Gray)
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onVerModelo, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Text("VER EN HISTORIAL")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Text("NUEVO ESCANEO")
        }
    }
}

fun capturarOriginal(context: Context, imageCapture: ImageCapture?, onResult: (Bitmap) -> Unit, onError: () -> Unit) {
    if (imageCapture == null) { onError(); return }
    val file = File(context.cacheDir, "original.jpg")
    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { onResult(it) } ?: onError()
        }
        override fun onError(exc: ImageCaptureException) = onError()
    })
}

fun capturarYCalibrarColor(context: Context, imageCapture: ImageCapture?, onResult: (Triple<Int, Int, Int>) -> Unit, onError: () -> Unit) {
    if (imageCapture == null) { onError(); return }
    val file = File(context.cacheDir, "calibration.jpg")
    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
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
