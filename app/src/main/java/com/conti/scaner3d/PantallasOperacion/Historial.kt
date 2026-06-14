package com.conti.scaner3d.PantallasOperacion

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conti.scaner3d.baseDatosLocal.Escaneo3D
import com.conti.scaner3d.baseDatosLocal.EscaneoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    escaneoDao: EscaneoDao,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var historyModels by remember { mutableStateOf(listOf<Escaneo3D>()) }

    // Función para recargar
    val cargarDatos = {
        coroutineScope.launch {
            historyModels = escaneoDao.obtenerTodos()
        }
    }

    LaunchedEffect(Unit) { cargarDatos() }

    // Launcher para IMPORTAR (.conti)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val success = importarArchivoConti(context, it, escaneoDao)
                if (success) {
                    Toast.makeText(context, "Modelo .conti importado con éxito", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(context, "Error al importar archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Modelos 3D", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importar .conti")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2), titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val items = listOf("Inicio", "Escanear", "Historial")
                val icons = listOf(Icons.Default.Home, Icons.Default.PhotoCamera, Icons.Default.History)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(icons[items.indexOf(item)], contentDescription = item) },
                        label = { Text(item) },
                        selected = item == "Historial",
                        onClick = { if (item != "Historial") onNavigate(item) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            if (historyModels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay modelos guardados.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyModels) { model ->
                        ModelCard(
                            model = model,
                            onView = { onNavigate("VisualizarModelo/${model.imagenUri}") },
                            onExport = {
                                coroutineScope.launch {
                                    exportarArchivoConti(context, model)
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    escaneoDao.eliminar(model)
                                    cargarDatos()
                                    Toast.makeText(context, "Modelo eliminado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelCard(model: Escaneo3D, onView: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onView() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1976D2).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF1976D2))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(model.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(model.fecha, fontSize = 10.sp, color = Color.Gray)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Exportar", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

suspend fun exportarArchivoConti(context: Context, model: Escaneo3D) = withContext(Dispatchers.IO) {
    try {
        val originalFile = File(model.imagenUri)
        if (!originalFile.exists()) return@withContext
        
        val exportName = model.nombre.replace(" ", "_") + ".conti"
        val downloadsDir = File(context.getExternalFilesDir(null), "Exports")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        
        val exportFile = File(downloadsDir, exportName)
        originalFile.copyTo(exportFile, overwrite = true)
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Exportado a: ${exportFile.name}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Log.e("Historial", "Error export", e)
    }
}

suspend fun importarArchivoConti(context: Context, uri: Uri, dao: EscaneoDao): Boolean = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
        val fileName = "imported_${System.currentTimeMillis()}.json"
        val destFile = File(context.filesDir, fileName)
        
        FileOutputStream(destFile).use { output ->
            inputStream.copyTo(output)
        }
        
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        dao.insertar(Escaneo3D(nombre = "Importado .conti", fecha = fecha, imagenUri = destFile.absolutePath))
        true
    } catch (e: Exception) {
        Log.e("Historial", "Error import", e)
        false
    }
}
