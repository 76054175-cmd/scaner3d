package com.conti.scaner3d.PantallasOperacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

data class MuestraLinea(val yRelativo: Float, val inicioX: Float, val longitud: Float)
data class MuestraAngulo(val angulo: Int, val lineas: List<MuestraLinea>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearScreen(
    escaneoDao: EscaneoDao,
    isDarkMode: Boolean,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val primaryBlue = Color(0xFF0D47A1)
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    var tienePermisoCamara by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val pedirPermisoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        tienePermisoCamara = permissions[Manifest.permission.CAMERA] ?: tienePermisoCamara
    }

    LaunchedEffect(Unit) {
        if (!tienePermisoCamara) {
            pedirPermisoLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    var faseEscaneo by remember { mutableStateOf("CONFIGURACION") }
    var precisionAngular by remember { mutableStateOf(20) }
    var resolucionSecantes by remember { mutableStateOf(120) }
    var escalaHorizontal by remember { mutableStateOf(1.0f) }
    var escalaVertical by remember { mutableStateOf(1.0f) }

    var modoAutomatico by remember { mutableStateOf(false) }
    var intervaloAutomatico by remember { mutableStateOf(3) }

    var colorFondoRef by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var umbralTolerancia by remember { mutableStateOf(60f) }

    var anguloActual by remember { mutableStateOf(0) }
    val muestrasCapturadas = remember { mutableStateListOf<MuestraAngulo>() }
    var procesandoImagen by remember { mutableStateOf(false) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    var ultimoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var cuentaRegresiva by remember { mutableStateOf(0) }
    var estaEnCuentaRegresiva by remember { mutableStateOf(false) }

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }

    fun sonarPitido() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    LaunchedEffect(estaEnCuentaRegresiva, cuentaRegresiva) {
        if (estaEnCuentaRegresiva && cuentaRegresiva > 0) {
            kotlinx.coroutines.delay(1000)
            cuentaRegresiva -= 1
            if (cuentaRegresiva == 0) {
                estaEnCuentaRegresiva = false
                procesandoImagen = true
                capturarOriginal(context, imageCaptureUseCase, onResult = { bitmap ->
                    ultimoBitmap = bitmap
                    faseEscaneo = "REVISION"
                    procesandoImagen = false
                }, onError = {
                    Toast.makeText(context, "Error captura automática", Toast.LENGTH_SHORT).show()
                    procesandoImagen = false
                })
            }
        }
    }

    BackHandler(faseEscaneo != "CONFIGURACION") {
        when (faseEscaneo) {
            "CALIBRACION" -> faseEscaneo = "CONFIGURACION"
            "CAPTURA" -> {
                if (anguloActual == 0) faseEscaneo = "CALIBRACION"
                else {
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
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("Escáner 3D", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        bottomBar = {
            if (faseEscaneo == "CONFIGURACION" || faseEscaneo == "FINALIZADO") {
                NavigationBar(containerColor = surfaceColor) {
                    val items = listOf("Inicio", "Escanear", "Historial")
                    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.History)
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = item == "Escanear",
                            onClick = { if (item != "Escanear") onNavigate(item) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isDarkMode) Color.White else primaryBlue,
                                selectedTextColor = if (isDarkMode) Color.White else primaryBlue,
                                indicatorColor = if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFE3F2FD),
                                unselectedIconColor = if (isDarkMode) Color.LightGray else Color.Gray,
                                unselectedTextColor = if (isDarkMode) Color.LightGray else Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (faseEscaneo) {
                "CONFIGURACION" -> {
                    ConfiguracionInicial(
                        isDarkMode = isDarkMode,
                        precision = precisionAngular,
                        onPrecisionChange = { precisionAngular = it },
                        resolucion = resolucionSecantes,
                        onResolucionChange = { resolucionSecantes = it },
                        eHorizontal = escalaHorizontal,
                        onEHorizontalChange = { escalaHorizontal = it },
                        eVertical = escalaVertical,
                        onEVerticalChange = { escalaVertical = it },
                        modoAuto = modoAutomatico,
                        onModoAutoChange = { modoAutomatico = it },
                        intervalo = intervaloAutomatico,
                        onIntervaloChange = { intervaloAutomatico = it },
                        onStart = { faseEscaneo = "CALIBRACION" }
                    )
                }
                "CALIBRACION" -> {
                    CalibracionFondo(
                        isDarkMode = isDarkMode,
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
                        isDarkMode = isDarkMode,
                        anguloActual = anguloActual,
                        totalFotos = 360 / precisionAngular,
                        precision = precisionAngular,
                        procesando = procesandoImagen,
                        tienePermiso = tienePermisoCamara,
                        cuentaRegresiva = if (estaEnCuentaRegresiva) cuentaRegresiva else 0,
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
                            estaEnCuentaRegresiva = false
                        },
                        onImageCaptureReady = { imageCaptureUseCase = it }
                    )
                }
                "REVISION" -> {
                    RevisionPipeline(
                        isDarkMode = isDarkMode,
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
                                if (modoAutomatico) {
                                    faseEscaneo = "CAPTURA"
                                    cuentaRegresiva = intervaloAutomatico
                                    estaEnCuentaRegresiva = true
                                    sonarPitido()
                                } else {
                                    faseEscaneo = "CAPTURA"
                                }
                            }
                        }
                    )
                }
                "FINALIZADO" -> {
                    ResultadoFinal(
                        isDarkMode = isDarkMode,
                        onReset = {
                            faseEscaneo = "CONFIGURACION"
                            muestrasCapturadas.clear()
                            anguloActual = 0
                        },
                        onVerModelo = { onNavigate("Historial") }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfiguracionInicial(
    isDarkMode: Boolean,
    precision: Int, onPrecisionChange: (Int) -> Unit,
    resolucion: Int, onResolucionChange: (Int) -> Unit,
    eHorizontal: Float, onEHorizontalChange: (Float) -> Unit,
    eVertical: Float, onEVerticalChange: (Float) -> Unit,
    modoAuto: Boolean, onModoAutoChange: (Boolean) -> Unit,
    intervalo: Int, onIntervaloChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray
    val primaryBlue = Color(0xFF0D47A1)

    var calidadSeleccionada by remember { mutableStateOf("Medio") }
    var mostrarAjustesAvanzados by remember { mutableStateOf(false) }

    LaunchedEffect(calidadSeleccionada) {
        when (calidadSeleccionada) {
            "Bajo" -> {
                onPrecisionChange(30)
                onResolucionChange(60)
            }
            "Medio" -> {
                onPrecisionChange(18)
                onResolucionChange(120)
            }
            "Alto" -> {
                onPrecisionChange(10)
                onResolucionChange(200)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calidad de Escaneo",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = if (isDarkMode) Color.White else primaryBlue
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val calidades = listOf("Bajo", "Medio", "Alto")
            val descripciones = listOf("12 fotos\nRápido", "20 fotos\nBalanceado", "36 fotos\nDetallado")
            val iconos = listOf(Icons.Default.Speed, Icons.Default.Balance, Icons.Default.HighQuality)

            calidades.forEachIndexed { index, calidad ->
                val seleccionado = calidadSeleccionada == calidad
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { calidadSeleccionada = calidad }
                        .border(
                            width = if (seleccionado) 3.dp else 1.dp,
                            color = if (seleccionado) primaryBlue else secondaryTextColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (seleccionado) primaryBlue.copy(alpha = 0.08f) else surfaceColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = iconos[index],
                            contentDescription = null,
                            tint = if (seleccionado) primaryBlue else secondaryTextColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = calidad,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descripciones[index],
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarAjustesAvanzados = !mostrarAjustesAvanzados },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = primaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ajustes Avanzados",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                    Icon(
                        imageVector = if (mostrarAjustesAvanzados) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = secondaryTextColor
                    )
                }

                AnimatedVisibility(
                    visible = mostrarAjustesAvanzados,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Escala Horizontal: ${String.format(Locale.US, "%.2f", eHorizontal)}", fontSize = 12.sp, color = textColor)
                        Slider(value = eHorizontal, onValueChange = onEHorizontalChange, valueRange = 0.5f..3.0f)

                        Text("Escala Vertical: ${String.format(Locale.US, "%.2f", eVertical)}", fontSize = 12.sp, color = textColor)
                        Slider(value = eVertical, onValueChange = onEVerticalChange, valueRange = 0.5f..3.0f)

                        HorizontalDivider(color = secondaryTextColor.copy(alpha = 0.15f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Captura Automática", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp, color = textColor)
                            Switch(checked = modoAuto, onCheckedChange = onModoAutoChange)
                        }

                        if (modoAuto) {
                            Text("Intervalo: $intervalo seg", fontSize = 12.sp, color = textColor)
                            Slider(value = intervalo.toFloat(), onValueChange = { onIntervaloChange(it.toInt()) }, valueRange = 2f..10f, steps = 8)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text("COMENZAR CALIBRACIÓN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun RevisionPipeline(
    isDarkMode: Boolean,
    bitmap: Bitmap,
    colorFondo: Triple<Int, Int, Int>,
    umbral: Float,
    onUmbralChange: (Float) -> Unit,
    resolucion: Int,
    angulo: Int,
    onRetake: () -> Unit,
    onConfirm: (MuestraAngulo) -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray
    val primaryBlue = Color(0xFF0D47A1)

    var viewMode by remember { mutableIntStateOf(2) }

    val scaledBitmap = remember(bitmap) {
        val maxDim = 250
        if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (w, h) = if (aspectRatio > 1) {
                maxDim to (maxDim / aspectRatio).toInt()
            } else {
                (maxDim * aspectRatio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else {
            bitmap
        }
    }

    var result by remember { mutableStateOf<PipelineResult?>(null) }

    LaunchedEffect(scaledBitmap, umbral, resolucion) {
        val calculated = withContext(Dispatchers.Default) {
            procesarPipeline(scaledBitmap, colorFondo, umbral, resolucion, angulo)
        }
        result = calculated
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Calibración de Silueta",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = textColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf("Original", "Aislado", "Silueta")
                    modes.forEachIndexed { index, name ->
                        val seleccionado = viewMode == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (seleccionado) primaryBlue else Color.Transparent)
                                .clickable { viewMode = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (seleccionado) Color.White else secondaryTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, secondaryTextColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    result?.let { currentResult ->
                        val displayBitmap = when (viewMode) {
                            0 -> scaledBitmap
                            1 -> currentResult.bitmapFondo
                            else -> currentResult.bitmapContorno
                        }
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        if (viewMode == 2) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                currentResult.muestra.lineas.forEach { linea ->
                                    val y = linea.yRelativo * size.height
                                    drawLine(
                                        color = Color.Cyan,
                                        start = Offset(linea.inicioX * size.width, y),
                                        end = Offset((linea.inicioX + linea.longitud) * size.width, y),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }
                    } ?: CircularProgressIndicator(color = primaryBlue)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sensibilidad de Filtro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            text = "${umbral.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = primaryBlue
                        )
                    }
                    Text(
                        text = "Ajusta para aislar el objeto del fondo verde o azul",
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )
                    Slider(
                        value = umbral,
                        onValueChange = onUmbralChange,
                        valueRange = 0f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryBlue,
                            activeTrackColor = primaryBlue,
                            inactiveTrackColor = secondaryTextColor.copy(alpha = 0.2f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Repetir", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { result?.let { onConfirm(it.muestra) } },
                        enabled = result != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CapturaPorAngulos(isDarkMode: Boolean, anguloActual: Int, totalFotos: Int, precision: Int, procesando: Boolean, tienePermiso: Boolean, cuentaRegresiva: Int, onCapture: () -> Unit, onCancel: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val primaryBlue = Color(0xFF0D47A1)

    val fotoNumero = (anguloActual / precision) + 1
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = surfaceColor), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PASO $fotoNumero DE $totalFotos", fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else primaryBlue, fontSize = 14.sp)
            Text("Gira el objeto a: $anguloActual°", fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor)
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                if (tienePermiso) VistaCamara(modifier = Modifier.fillMaxSize(), onImageCaptureReady = onImageCaptureReady)

                if (cuentaRegresiva > 0) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ROTAR AHORA", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("$cuentaRegresiva", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (procesando) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onCapture, enabled = !procesando && cuentaRegresiva == 0, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (cuentaRegresiva > 0) "ESPERANDO..." else "CAPTURAR", fontWeight = FontWeight.Bold, color = Color.White)
            }
            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
                Text("Cancelar Escaneo", color = Color.Red)
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
            drawLine(Color.Red, Offset(centerX, 0f), Offset(centerX, size.height), 3f)
            val guideColor = Color.White.copy(alpha = 0.7f)
            val padding = 60f
            drawLine(guideColor, Offset(centerX - 100, padding), Offset(centerX + 100, padding), 4f)
            drawLine(guideColor, Offset(centerX - 100, size.height - padding), Offset(centerX + 100, size.height - padding), 4f)
        }
        Text("ALINEA EL EJE CENTRAL", modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp)).padding(4.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            val dr = (android.graphics.Color.red(p) - refR).toDouble()
            val dg = (android.graphics.Color.green(p) - refG).toDouble()
            val db = (android.graphics.Color.blue(p) - refB).toDouble()
            val diff = sqrt(dr*dr + dg*dg + db*db)

            if (diff > umbral) {
                fondoPixels[idx] = p
                if (xMin == -1) xMin = x
                xMax = x
            } else {
                fondoPixels[idx] = android.graphics.Color.BLACK
            }
        }
        if (xMin != -1 && xMax != -1) {
            for (x in xMin..xMax) { contornoPixels[y * width + x] = android.graphics.Color.WHITE }
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

@Composable
fun CalibracionFondo(isDarkMode: Boolean, procesando: Boolean, tienePermiso: Boolean, onCalibrate: () -> Unit, onImageCaptureReady: (ImageCapture) -> Unit) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val primaryBlue = Color(0xFF0D47A1)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = surfaceColor), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CALIBRACIÓN", fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else primaryBlue)
            Text("Apunta al fondo vacío sin el objeto", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                if (tienePermiso) {
                    VistaCamara(modifier = Modifier.fillMaxSize(), onImageCaptureReady = onImageCaptureReady)
                    Box(modifier = Modifier.size(24.dp).border(3.dp, Color.White, CircleShape).align(Alignment.Center))
                }
                if (procesando) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCalibrate, enabled = !procesando, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                Text("CAPTURAR COLOR FONDO", color = Color.White)
            }
        }
    }
}

@Composable
fun ResultadoFinal(isDarkMode: Boolean, onReset: () -> Unit, onVerModelo: () -> Unit) {
    val textColor = if (isDarkMode) Color.White else Color(0xFF1976D2)
    val primaryBlue = Color(0xFF0D47A1)

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudDone, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(120.dp))
        Text("¡PROCESO FINALIZADO!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor)
        Text("El modelo ha sido exportado a JSON.", textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onVerModelo, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
            Text("VER EN HISTORIAL", color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Text("NUEVO ESCANEO", color = if (isDarkMode) Color.White else primaryBlue)
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
                onResult(Triple(android.graphics.Color.red(pixel), android.graphics.Color.green(pixel), android.graphics.Color.blue(pixel)))
            } else onError()
        }
        override fun onError(exc: ImageCaptureException) = onError()
    })
}