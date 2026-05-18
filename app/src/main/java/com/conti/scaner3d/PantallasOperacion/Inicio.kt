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
import androidx.compose.ui.unit.sp
import com.conti.scaner3d.R

data class RecentModel(val id: Int, val name: String, val imageResId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    usuario: String = "Usuario",
    onNavigate: (String) -> Unit = {} // <-- Cambiamos onCerrarSesion por onNavigate
) {
    // El índice 0 corresponde a "Inicio"
    val selectedItem = 0
    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(
        Icons.Default.Home,
        Icons.Default.Search,
        Icons.Default.BookmarkBorder,
        Icons.Default.Person
    )

    // Datos de ejemplo para las tarjetas
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
            TopAppBar(
                title = { Text("") }, // Sin título como en tu diseño
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2)), // Azul de perfil
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menú hamburguesa */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                            // Solo navegamos si hacemos clic en una pestaña diferente a "Inicio"
                            if (item != "Inicio") {
                                onNavigate(item)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2),
                            selectedTextColor = Color(0xFF1976D2),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.95f),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp
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
                        color = Color.Black
                    )
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        placeholder = { Text("Buscar modelos...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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