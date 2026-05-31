package com.conti.scaner3d.PantallasOperacion

import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.conti.scaner3d.baseDatosLocal.Escaneo3D
import com.conti.scaner3d.baseDatosLocal.EscaneoDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    escaneoDao: EscaneoDao,
    onNavigate: (String) -> Unit = {}
) {
    val selectedItem = 2
    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.BookmarkBorder, Icons.Default.Person)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var historyModels by remember { mutableStateOf(listOf<Escaneo3D>()) }

    var mostrarDialog by remember { mutableStateOf(false) }
    var escaneoSeleccionado by remember { mutableStateOf<Escaneo3D?>(null) }
    var nombreEditado by remember { mutableStateOf("") }

    // Cargar historial al iniciar
    LaunchedEffect(Unit) {
        historyModels = escaneoDao.obtenerTodos()
    }

    // Cuadro de Diálogo para Editar/Eliminar
    if (mostrarDialog && escaneoSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialog = false },
            title = { Text("Gestionar Modelo", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nombreEditado,
                    onValueChange = { nombreEditado = it },
                    label = { Text("Nombre del Escaneo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val actualizado = escaneoSeleccionado!!.copy(nombre = nombreEditado)
                            escaneoDao.actualizar(actualizado)
                            historyModels = escaneoDao.obtenerTodos()
                            mostrarDialog = false
                            Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        escaneoDao.eliminar(escaneoSeleccionado!!)
                        historyModels = escaneoDao.obtenerTodos()
                        mostrarDialog = false
                        Toast.makeText(context, "Escaneo eliminado", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Eliminar", color = Color.Red) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanner 3D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(bottomNavIcons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            if (item != "Historial") onNavigate(item)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2), selectedTextColor = Color(0xFF1976D2),
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Historial", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Tienes ${historyModels.size} modelos guardados", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(24.dp))

            if (historyModels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay escaneos guardados.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()
                ) {
                    items(historyModels) { model ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    escaneoSeleccionado = model
                                    nombreEditado = model.nombre
                                    mostrarDialog = true
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)).background(Color.LightGray)) {
                                // --- RENDERIZADO DE LA FOTO REAL CAPTURADA ---
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                                    },
                                    update = { imageView ->
                                        try {
                                            imageView.setImageURI(Uri.parse(model.imagenUri))
                                        } catch (e: Exception) {
                                            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(Color.White))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(model.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(model.fecha, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}