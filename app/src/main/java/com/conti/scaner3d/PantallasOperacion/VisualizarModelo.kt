package com.conti.scaner3d.PantallasOperacion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val escala = 400f

    val lineas3D = remember(jsonPath) {
        cargarLineasDesdeJSON(jsonPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizador Wireframe 3D", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        rotY += dragAmount.x * 0.5f
                        rotX -= dragAmount.y * 0.5f
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centroX = size.width / 2
                val centroY = size.height / 2

                lineas3D.forEach { (p1, p2) ->
                    val proj1 = proyectar(p1, rotX, rotY, centroX, centroY, escala)
                    val proj2 = proyectar(p2, rotX, rotY, centroX, centroY, escala)

                    drawLine(
                        color = Color.Cyan.copy(alpha = 0.6f),
                        start = proj1,
                        end = proj2,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Usa un dedo para rotar el modelo", color = Color.White.copy(alpha = 0.7f))
                Text("Líneas detectadas: ${lineas3D.size}", color = Color.White.copy(alpha = 0.5f))
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
        
        for (i in 0 until muestras.length()) {
            val muestra = muestras.getJSONObject(i)
            val anguloDeg = muestra.getInt("angulo")
            val anguloRad = Math.toRadians(anguloDeg.toDouble()).toFloat()
            val lineasArray = muestra.getJSONArray("lineas")
            
            for (j in 0 until lineasArray.length()) {
                val l = lineasArray.getJSONObject(j)
                val yRel = l.getDouble("y_relativo").toFloat() - 0.5f // Centrar Y
                val inicioX = l.getDouble("inicio_x").toFloat() - 0.5f // Centrar X respecto al eje
                val longitud = l.getDouble("longitud").toFloat()
                
                // Definimos el segmento en el plano local del ángulo
                val x1Local = inicioX
                val x2Local = inicioX + longitud
                
                // Rotamos el segmento 3D según el ángulo de captura
                // El segmento es horizontal en la foto, por lo que varía en el eje perpendicular a la vista
                val p1 = Punto3D(
                    x = x1Local * cos(anguloRad),
                    y = -yRel, // Invertir Y para coordenadas de pantalla
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
