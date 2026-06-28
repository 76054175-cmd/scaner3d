package com.conti.scaner3d.PantallasOperacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

data class Punto3D(val x: Float, val y: Float, val z: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizarModeloScreen(
    jsonPath: String,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var autoRotar by remember { mutableStateOf(true) }
    var velocidadRotacion by remember { mutableFloatStateOf(1.0f) }
    var colorLineas by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var mostrarAjustes by remember { mutableStateOf(false) }

    val escala = 400f
    val surfaceColor = if (isDarkMode) Color(0xFF151D2A) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    val lineas3D = remember(jsonPath) {
        cargarLineasDesdeJSON(jsonPath)
    }

    LaunchedEffect(autoRotar, velocidadRotacion) {
        if (autoRotar) {
            while (true) {
                rotY += velocidadRotacion
                kotlinx.coroutines.delay(16)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VISUALIZADOR 3D", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                        Text("Explora la nube de puntos digitalizada", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0A111E), Color(0xFF030712)),
                        center = Offset.Unspecified
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { autoRotar = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotY += dragAmount.x * 0.4f
                            rotX -= dragAmount.y * 0.4f
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centroX = size.width / 2
                val centroY = size.height / 2

                lineas3D.forEach { (p1, p2) ->
                    val proj1 = proyectar(p1, rotX, rotY, centroX, centroY, escala)
                    val proj2 = proyectar(p2, rotX, rotY, centroX, centroY, escala)

                    drawLine(
                        color = colorLineas.copy(alpha = 0.7f),
                        start = proj1,
                        end = proj2,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, colorLineas.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "X: ${rotX.toInt() % 360}°  •  Y: ${rotY.toInt() % 360}°",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            AnimatedVisibility(
                visible = !mostrarAjustes,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .border(1.dp, colorLineas.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { autoRotar = !autoRotar }) {
                            Icon(
                                imageVector = if (autoRotar) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = colorLineas
                            )
                        }
                        IconButton(
                            onClick = {
                                rotX = 0f
                                rotY = 0f
                                autoRotar = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { mostrarAjustes = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = mostrarAjustes,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.92f)),
                    modifier = Modifier.border(1.dp, colorLineas.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AJUSTES DE VISUALIZACIÓN",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = colorLineas,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = { mostrarAjustes = false },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.Gray.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Giro Automático", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = textColor, fontSize = 13.sp)
                            Switch(
                                checked = autoRotar,
                                onCheckedChange = { autoRotar = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorLineas,
                                    checkedTrackColor = colorLineas.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Velocidad", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), color = textColor, fontSize = 13.sp)
                            Slider(
                                value = velocidadRotacion,
                                onValueChange = { velocidadRotacion = it },
                                valueRange = 0.1f..5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = colorLineas,
                                    activeTrackColor = colorLineas,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Color del Holograma", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val coloresNeon = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF00E676),
                                Color(0xFFFFEA00),
                                Color(0xFFFF1744),
                                Color(0xFFD500F9),
                                Color.White
                            )
                            coloresNeon.forEach { color ->
                                val activo = colorLineas == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (activo) 3.dp else 1.dp,
                                            color = if (activo) textColor else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { colorLineas = color }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun cargarLineasDesdeJSON(path: String): List<Pair<Punto3D, Punto3D>> {
    val file = File(path)
    if (!file.exists()) return emptyList()

    val lineas = mutableListOf<Pair<Punto3D, Punto3D>>()
    try {
        val json = JSONObject(file.readText())
        val muestras = json.getJSONArray("muestras")

        val escH = if (json.has("escala_horizontal")) json.getDouble("escala_horizontal").toFloat() else 1.0f
        val escV = if (json.has("escala_vertical")) json.getDouble("escala_vertical").toFloat() else 1.0f

        for (i in 0 until muestras.length()) {
            val muestra = muestras.getJSONObject(i)
            val anguloDeg = muestra.getInt("angulo")
            val anguloRad = Math.toRadians(anguloDeg.toDouble()).toFloat()
            val lineasArray = muestra.getJSONArray("lineas")

            for (j in 0 until lineasArray.length()) {
                val l = lineasArray.getJSONObject(j)
                val yRel = (l.getDouble("y_relativo").toFloat() - 0.5f) * escV
                val inicioX = (l.getDouble("inicio_x").toFloat() - 0.5f) * escH
                val longitud = l.getDouble("longitud").toFloat() * escH

                val x1Local = inicioX
                val x2Local = inicioX + longitud

                val p1 = Punto3D(
                    x = x1Local * cos(anguloRad),
                    y = -yRel,
                    z = x1Local * sin(anguloRad)
                )
                val p2 = Punto3D(
                    x = x2Local * cos(anguloRad),
                    y = -yRel,
                    z = x2Local * sin(anguloRad)
                )
                lineas.add(p1 to p2)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lineas
}

fun proyectar(punto: Punto3D, rotX: Float, rotY: Float, centroX: Float, centroY: Float, escala: Float): Offset {
    val radX = Math.toRadians(rotX.toDouble()).toFloat()
    val radY = Math.toRadians(rotY.toDouble()).toFloat()

    var x = punto.x * cos(radY) + punto.z * sin(radY)
    var z = -punto.x * sin(radY) + punto.z * cos(radY)
    var y = punto.y

    val tempY = y * cos(radX) - z * sin(radX)
    z = y * sin(radX) + z * cos(radX)
    y = tempY

    return Offset(
        x = centroX + (x * escala),
        y = centroY + (y * escala)
    )
}