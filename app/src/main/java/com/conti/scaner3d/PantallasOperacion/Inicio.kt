package com.conti.scaner3d.PantallasOperacion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.conti.scaner3d.R

// Data class para representar un modelo 3D reciente
data class RecentModel(val id: Int, val name: String, val imageResId: Int)

@Composable
fun InicioScreen(
    usuario: String = "Usuario",
    onCerrarSesion: () -> Unit = {}
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(
        Icons.Default.Home,
        Icons.Default.Search,
        Icons.Default.Collections,
        Icons.Default.Person
    )

    // Datos de ejemplo para las tarjetas (usando el fondo por defecto)
    val recentModels = listOf(
        RecentModel(1, "Modelo 1", R.drawable.ic_launcher_background),
        RecentModel(2, "Modelo 2", R.drawable.ic_launcher_background),
        RecentModel(3, "Modelo 3", R.drawable.ic_launcher_background),
        RecentModel(4, "Modelo 4", R.drawable.ic_launcher_background),
        RecentModel(5, "Modelo 5", R.drawable.ic_launcher_background),
        RecentModel(6, "Modelo 6", R.drawable.ic_launcher_background)
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Círculo azul superior con inicial o ícono
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = Color.White
                    )
                }

                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(bottomNavIcons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            // Si hace clic en Perfil (index 3), cerramos sesión como prueba
                            if (item == "Perfil") {
                                onCerrarSesion()
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Panel contenedor blanco redondeado de la imagen
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.95f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Un tono suave para resaltar
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Hi $usuario,",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buscador limpio sin configuraciones conflictivas de color
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        placeholder = { Text("Buscar modelos...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grilla de Modelos
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(recentModels) { model ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = model.imageResId),
                                        contentDescription = model.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Texto encima del modelo
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Text(
                                            text = model.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}