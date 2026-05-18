package com.conti.scaner3d.PantallasOperacion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conti.scaner3d.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit = {},
    onReturnHome: () -> Unit = {},
    onNavigate: (String) -> Unit = {} // <-- ¡ESTA ES LA CORRECCIÓN!
) {
    val selectedItem = 3 // Índice de Perfil
    var darkModeEnabled by remember { mutableStateOf(false) }

    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.BookmarkBorder, Icons.Default.Person)

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
                            // ¡CORRECCIÓN DE NAVEGACIÓN!
                            if (item != "Perfil") {
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
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(painter = painterResource(id = R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.size(120.dp).clip(CircleShape).border(2.dp, Color.LightGray, CircleShape), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Nelson Luis Sota", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Left 4 Dead 2 • Programador", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            Text("Profile Settings", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            ProfileOptionItem(Icons.Default.AccountCircle, "Informacion Personal")
            ProfileOptionItem(Icons.Default.NotificationsNone, "Notificaciones")
            ProfileOptionItem(Icons.Default.FavoriteBorder, "Lista de Deseos")

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Brightness4, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Dark Mode", modifier = Modifier.weight(1f), fontSize = 18.sp)
                Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ArrowCircleRight, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(onClick = onCerrarSesion, contentPadding = PaddingValues(0.dp)) {
                    Text("Log Out", fontSize = 18.sp, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onReturnHome, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                Text("Return to Home", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileOptionItem(icon: ImageVector, title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 18.sp)
    }
}