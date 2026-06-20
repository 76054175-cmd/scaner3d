package com.conti.scaner3d.PantallasOperacion

import android.content.Context
import android.net.Uri
import android.content.Intent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

    // Colores consistentes
    val primaryBlue = Color(0xFF0D47A1)
    val lightBlue = Color(0xFFE3F2FD)

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
            CenterAlignedTopAppBar(
                title = { Text("MIS MODELOS 3D", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importar .conti", tint = primaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val items = listOf("Inicio", "Escanear", "Historial")
                val icons = listOf(Icons.Default.Home, Icons.Default.PhotoCamera, Icons.Default.History)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(icons[items.indexOf(item)], contentDescription = item) },
                        label = { Text(item, fontWeight = FontWeight.Medium) },
                        selected = item == "Historial",
                        onClick = { if (item != "Historial") onNavigate(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryBlue,
                            indicatorColor = lightBlue
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, lightBlue.copy(alpha = 0.3f))))
                .padding(innerPadding)
        ) {
            if (historyModels.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay modelos guardados todavía.", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyModels) { model ->
                        ModelCard(
                            model = model,
                            onView = { onNavigate("VisualizarModelo/${model.imagenUri}") },
                            onExport = {
                                coroutineScope.launch {
                                    val exportedFile = exportarArchivoConti(context, model)
                                    if (exportedFile != null) {
                                        compartirArchivoConti(context, exportedFile)
                                    }
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
    val primaryBlue = Color(0xFF0D47A1)
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.clickable { onView() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(primaryBlue.copy(alpha = 0.05f), primaryBlue.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = primaryBlue
                )
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = model.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                Text(
                    text = model.fecha,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).background(Color(0xFFFFEBEE), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

suspend fun exportarArchivoConti(context: Context, model: Escaneo3D): File? = withContext(Dispatchers.IO) {
    try {
        val originalFile = File(model.imagenUri)
        if (!originalFile.exists()) return@withContext null
        
        val exportName = model.nombre.replace(" ", "_") + ".conti"
        val exportsDir = File(context.getExternalFilesDir(null), "Exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        
        val exportFile = File(exportsDir, exportName)
        originalFile.copyTo(exportFile, overwrite = true)
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Preparado para compartir: ${exportFile.name}", Toast.LENGTH_SHORT).show()
        }
        exportFile
    } catch (e: Exception) {
        Log.e("Historial", "Error export", e)
        null
    }
}

fun compartirArchivoConti(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Modelo Conti"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
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
