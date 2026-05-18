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

data class HistoryModel(val id: Int, val name: String, val date: String, val imageResId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onNavigate: (String) -> Unit = {}
) {
    val selectedItem = 2 // Índice de Historial
    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.BookmarkBorder, Icons.Default.Person)

    val historyModels = listOf(
        HistoryModel(1, "Modelo 1", "21 de Abril de 2026", R.drawable.ic_launcher_background),
        HistoryModel(2, "Modelo 2", "20 de Abril de 2026", R.drawable.ic_launcher_background),
        HistoryModel(3, "Modelo 3", "19 de Abril de 2026", R.drawable.ic_launcher_background),
        HistoryModel(4, "Modelo 4", "18 de Abril de 2026", R.drawable.ic_launcher_background)
    )

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
                            // ¡ESTA ES LA CORRECCIÓN!
                            if (item != "Historial") {
                                onNavigate(item)
                            }
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()
            ) {
                items(historyModels) { model ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)).background(Color.LightGray)) {
                            Image(painter = painterResource(id = model.imageResId), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(Color.White))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(model.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}