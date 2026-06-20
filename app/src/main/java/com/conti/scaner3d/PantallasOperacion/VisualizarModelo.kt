package com.conti.scaner3d.PantallasOperacion

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
    onBack: () -> Unit
) {
    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var autoRotar by remember { mutableStateOf(true) }
    var velocidadRotacion by remember { mutableFloatStateOf(1.0f) }
    var colorLineas by remember { mutableStateOf(Color.Cyan) }
    var mostrarAjustes by remember { mutableStateOf(false) }
    
    val escala = 400f
    val primaryBlue = Color(0xFF0D47A1)

    val lineas3D = remember(jsonPath) {
        cargarLineasDesdeJSON(jsonPath)
    }

    // Rotación automática
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
                        Text("Explora el modelo digital", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { mostrarAjustes = !mostrarAjustes },
                        modifier = Modifier.background(if (mostrarAjustes) primaryBlue.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            if (mostrarAjustes) Icons.Default.Close else Icons.Default.Settings, 
                            contentDescription = "Ajustes",
                            tint = if (mostrarAjustes) primaryBlue else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { autoRotar = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotY += dragAmount.x * 0.5f
                            rotX -= dragAmount.y * 0.5f
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
                        color = colorLineas.copy(alpha = 0.8f),
                        start = proj1,
                        end = proj2,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // HUD / Indicador de rotación
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    "Rotación: ${rotY.toInt()}°",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (mostrarAjustes) {
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("AJUSTES DE VISTA", fontWeight = FontWeight.Black, fontSize = 12.sp, color = primaryBlue)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-rotar", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = autoRotar, 
                                onCheckedChange = { autoRotar = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = primaryBlue)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Velocidad", fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                            Slider(
                                value = velocidadRotacion, 
                                onValueChange = { velocidadRotacion = it }, 
                                valueRange = 0.1f..5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = primaryBlue, activeTrackColor = primaryBlue)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Color del Neón", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val colores = listOf(Color.Cyan, Color(0xFF00E676), Color.Yellow, Color(0xFFFF5252), Color.White, Color(0xFFD1C4E9))
                            colores.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(if (colorLineas == color) 3.dp else 0.dp, primaryBlue, CircleShape)
                                        .clickable { colorLineas = color }
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Desliza para explorar • Pulsa Ajustes para personalizar",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
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
        
        // Cargar escalas (con fallback a 1.0 si no existen)
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

    // Rotación en Y
    var x = punto.x * cos(radY) + punto.z * sin(radY)
    var z = -punto.x * sin(radY) + punto.z * cos(radY)
    var y = punto.y

    // Rotación en X
    val tempY = y * cos(radX) - z * sin(radX)
    z = y * sin(radX) + z * cos(radX)
    y = tempY

    // Proyección simple (Ortográfica con escala)
    return Offset(
        x = centroX + (x * escala),
        y = centroY + (y * escala)
    )
}
