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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlin.math.pow
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
    var colorFondoRef by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var umbralTolerancia by remember { mutableFloatStateOf(50f) }
    
    // ESTADOS DE CAPTURA
    var anguloActual by remember { mutableIntStateOf(0) }
    val muestrasCapturadas = remember { mutableStateListOf<MuestraAngulo>() }
    var procesandoImagen by remember { mutableStateOf(false) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    
    // ESTADO DE REVISIÓN
    var ultimoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner 3D - Pipeline", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor(0xFF1976D2), titleContentColor = ComposeColor.White)
            )
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
                                    guardarResultadoFinal(context, muestrasCapturadas, precisionAngular, resolucionSecantes, escaneoDao)
                                    faseEscaneo = "FINALIZADO"
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
    var viewMode by remember { mutableIntStateOf(0) } // 0: Original, 1: Filtro Fondo, 2: Contorno/Muestreo
    
    // Procesar imágenes según umbral actual
    val result = remember(umbral, resolucion) {
        procesarPipeline(bitmap, colorFondo, umbral, resolucion, angulo)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Revisión Angulo $angulo°", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Selector de Vista
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = viewMode == 0, onClick = { viewMode = 0 }, label = { Text("Original") })
                    FilterChip(selected = viewMode == 1, onClick = { viewMode = 1 }, label = { Text("Fondo") })
                    FilterChip(selected = viewMode == 2, onClick = { viewMode = 2 }, label = { Text("Muestreo") })
                }

                Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(ComposeColor.Black)) {
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
                                drawLine(ComposeColor.Cyan, Offset(linea.inicioX * size.width, y), Offset((linea.inicioX + linea.longitud) * size.width, y), 2f)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Calibración de Filtro", fontWeight = FontWeight.SemiBold)
                Text("Umbral de Tolerancia: ${umbral.toInt()}", fontSize = 12.sp)
                Slider(value = umbral, onValueChange = onUmbralChange, valueRange = 10f..150f)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("Repetir") }
                    Button(onClick = { onConfirm(result.muestra) }, modifier = Modifier.weight(1f)) { Text("Confirmar") }
                }
            }
        }
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

    // Paso de muestreo vertical basado en la resolución n
    val stepY = (height / n).coerceAtLeast(1)

    for (y in 0 until height) {
        var xMin = -1
        var xMax = -1
        
        for (x in 0 until width) {
            val idx = y * width + x
            val p = pixels[idx]
            
            // FILTRO 1: Distancia euclidiana de color respecto al fondo calibrado
            val dr = (Color.red(p) - refR).toDouble()
            val dg = (Color.green(p) - refG).toDouble()
            val db = (Color.blue(p) - refB).toDouble()
            val diff = sqrt(dr*dr + dg*dg + db*db)

            if (diff > umbral) {
                // Es parte del objeto
                fondoPixels[idx] = p 
                if (xMin == -1) xMin = x
                xMax = x
            } else {
                // Es fondo
                fondoPixels[idx] = Color.BLACK
            }
        }
        
        // FILTRO 2: Crear Silueta Sólida (ignora detalles internos)
        // Si encontramos un inicio y un fin, rellenamos todo el medio en el mapa de contorno
        if (xMin != -1 && xMax != -1) {
            for (x in xMin..xMax) {
                contornoPixels[y * width + x] = Color.WHITE
            }
            
            // MUESTREO: Solo si toca procesar esta línea según n
            if (y % stepY == 0) {
                lineas.add(MuestraLinea(
                    yRelativo = y.toFloat() / height,
                    inicioX = xMin.toFloat() / width,
                    longitud = (xMax - xMin).toFloat() / width
                ))
            }
        }
    }

    fondoBitmap.setPixels(fondoPixels, 0, width, 0, 0, width, height)
    contornoBitmap.setPixels(contornoPixels, 0, width, 0, 0, width, height)

    return PipelineResult(fondoBitmap, contornoBitmap, MuestraAngulo(angulo, lineas))
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
        
        // Indicador de eje central
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            drawLine(ComposeColor.Red.copy(alpha = 0.5f), Offset(centerX, 0f), Offset(centerX, size.height), 2f)
            // Marcas de encuadre
            drawLine(ComposeColor.White, Offset(centerX - 40, size.height * 0.2f), Offset(centerX + 40, size.height * 0.2f), 4f)
            drawLine(ComposeColor.White, Offset(centerX - 40, size.height * 0.8f), Offset(centerX + 40, size.height * 0.8f), 4f)
        }
    }
}

// Funciones auxiliares simplificadas
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
fun CapturaPorAngulos(anguloActual: Int, totalFotos: Int, precision: Int, procesando: Boolean, tienePermiso: Boolean, onCapture: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    val fotoNumero = (anguloActual / precision) + 1
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ComposeColor.White)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Paso $fotoNumero de $totalFotos", fontWeight = FontWeight.Bold, color = ComposeColor(0xFF1976D2))
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
        Button(onClick = onVerModelo, modifier = Modifier.fillMaxWidth()) { Text("Ver en Historial") }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Nuevo Escaneo") }
    }
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
