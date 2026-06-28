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
    isDarkMode: Boolean,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var historyModels by remember { mutableStateOf(listOf<Escaneo3D>()) }

    var consultaBusqueda by remember { mutableStateOf("") }
    var filtroFecha by remember { mutableStateOf("Todos") }

    var modeloARenombrar by remember { mutableStateOf<Escaneo3D?>(null) }
    var nuevoNombreInput by remember { mutableStateOf("") }

    val primaryBlue = Color(0xFF0D47A1)
    val lightBlue = if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFE3F2FD)
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray

    val cargarDatos = {
        coroutineScope.launch {
            historyModels = escaneoDao.obtenerTodos()
        }
    }

    LaunchedEffect(Unit) { cargarDatos() }

    val modelosFiltrados = remember(historyModels, consultaBusqueda, filtroFecha) {
        val formatoFechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val formatoEsteMes = SimpleDateFormat("/MM/yyyy", Locale.getDefault()).format(Date())

        historyModels.filter { model ->
            val coincideNombre = model.nombre.contains(consultaBusqueda, ignoreCase = true)
            val coincideFecha = when (filtroFecha) {
                "Hoy" -> model.fecha.startsWith(formatoFechaHoy)
                "Este Mes" -> model.fecha.contains(formatoEsteMes)
                else -> true
            }
            coincideNombre && coincideFecha
        }
    }

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

    if (modeloARenombrar != null) {
        AlertDialog(
            onDismissRequest = { modeloARenombrar = null },
            title = { Text("Renombrar Modelo", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nuevoNombreInput,
                    onValueChange = { nuevoNombreInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nuevoNombreInput.isNotBlank()) {
                            coroutineScope.launch {
                                val modeloActualizado = modeloARenombrar!!.copy(nombre = nuevoNombreInput)
                                escaneoDao.actualizar(modeloActualizado)
                                cargarDatos()
                                modeloARenombrar = null
                            }
                        }
                    }
                ) {
                    Text("Guardar", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { modeloARenombrar = null }) {
                    Text("Cancelar", color = secondaryTextColor)
                }
            },
            containerColor = surfaceColor
        )
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MIS MODELOS 3D", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importar .conti", tint = if (isDarkMode) Color.White else primaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = surfaceColor, tonalElevation = 8.dp) {
                val items = listOf("Inicio", "Escanear", "Historial")
                val icons = listOf(Icons.Default.Home, Icons.Default.PhotoCamera, Icons.Default.History)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(icons[items.indexOf(item)], contentDescription = item) },
                        label = { Text(item, fontWeight = FontWeight.Medium) },
                        selected = item == "Historial",
                        onClick = { if (item != "Historial") onNavigate(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDarkMode) Color.White else primaryBlue,
                            selectedTextColor = if (isDarkMode) Color.White else primaryBlue,
                            indicatorColor = lightBlue,
                            unselectedIconColor = if (isDarkMode) Color.LightGray else Color.Gray,
                            unselectedTextColor = if (isDarkMode) Color.LightGray else Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(surfaceColor, lightBlue.copy(alpha = 0.3f))))
        ) {
            OutlinedTextField(
                value = consultaBusqueda,
                onValueChange = { consultaBusqueda = it },
                placeholder = { Text("Buscar por nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryBlue) },
                trailingIcon = {
                    if (consultaBusqueda.isNotEmpty()) {
                        IconButton(onClick = { consultaBusqueda = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = secondaryTextColor)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val opciones = listOf("Todos", "Hoy", "Este Mes")
                opciones.forEach { opcion ->
                    val activo = filtroFecha == opcion
                    FilterChip(
                        selected = activo,
                        onClick = { filtroFecha = opcion },
                        label = { Text(opcion) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = surfaceColor,
                            labelColor = secondaryTextColor
                        )
                    )
                }
            }

            if (modelosFiltrados.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No se encontraron modelos guardados.", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(modelosFiltrados) { model ->
                        ModelCard(
                            model = model,
                            isDarkMode = isDarkMode,
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
                            },
                            onRename = {
                                modeloARenombrar = model
                                nuevoNombreInput = model.nombre
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelCard(model: Escaneo3D, isDarkMode: Boolean, onView: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit) {
    val primaryBlue = Color(0xFF0D47A1)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = surfaceColor)
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
                    tint = if (isDarkMode) Color.White else primaryBlue
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = model.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Renombrar",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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
                        modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9).copy(alpha = if (isDarkMode) 0.1f else 1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).background(Color(0xFFFFEBEE).copy(alpha = if (isDarkMode) 0.1f else 1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
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