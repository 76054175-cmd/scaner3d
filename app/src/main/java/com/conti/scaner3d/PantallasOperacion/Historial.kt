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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    // Estados para el Visor de Imagen y Edición
    var mostrarVisorImagen by remember { mutableStateOf(false) }
    var mostrarDialogoEdicion by remember { mutableStateOf(false) }
    var escaneoSeleccionado by remember { mutableStateOf<Escaneo3D?>(null) }
    var nombreEditado by remember { mutableStateOf("") }

    // Cargar historial al iniciar
    LaunchedEffect(Unit) {
        historyModels = escaneoDao.obtenerTodos()
    }

    // --- 1. VISOR DE IMAGEN A PANTALLA COMPLETA ---
    if (mostrarVisorImagen && escaneoSeleccionado != null) {
        Dialog(
            onDismissRequest = { mostrarVisorImagen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false) // Ocupa toda la pantalla
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra superior del Visor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { mostrarVisorImagen = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }

                        Text(
                            text = escaneoSeleccionado!!.nombre,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        )

                        Row {
                            IconButton(onClick = {
                                nombreEditado = escaneoSeleccionado!!.nombre
                                mostrarDialogoEdicion = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    escaneoDao.eliminar(escaneoSeleccionado!!)
                                    historyModels = escaneoDao.obtenerTodos()
                                    mostrarVisorImagen = false
                                    Toast.makeText(context, "Escaneo eliminado", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }

                    // Imagen del Contorno (Muestra la imagen procesada de la base de datos)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                            },
                            update = { imageView ->
                                try {
                                    imageView.setImageURI(Uri.parse(escaneoSeleccionado!!.imagenUri))
                                } catch (e: Exception) {
                                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Fecha inferior
                    Text(
                        text = "Escaneado el: ${escaneoSeleccionado!!.fecha}",
                        color = Color.LightGray,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    // --- 2. CUADRO DE DIÁLOGO PARA EDITAR NOMBRE ---
    if (mostrarDialogoEdicion && escaneoSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEdicion = false },
            title = { Text("Renombrar Modelo", fontWeight = FontWeight.Bold) },
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
                            escaneoSeleccionado = actualizado // Actualiza el título en el Visor
                            mostrarDialogoEdicion = false
                            Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEdicion = false }) { Text("Cancelar") }
            }
        )
    }

    // --- 3. PANTALLA PRINCIPAL (GRILLA DE HISTORIAL) ---
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
                                    // Al hacer clic, navega al Visualizador 3D usando la ruta del JSON
                                    onNavigate("VisualizarModelo/${model.imagenUri}")
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1976D2).copy(alpha = 0.1f))) {
                                // Icono representativo de 3D/Wireframe
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).align(Alignment.Center),
                                    tint = Color(0xFF1976D2)
                                )
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50))) // Indicador verde
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(model.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(model.fecha, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}