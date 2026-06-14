package com.conti.scaner3d.PantallasOperacion

import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.conti.scaner3d.baseDatosLocal.Usuario
import com.conti.scaner3d.baseDatosLocal.UsuarioDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    usuarioLogueado: String,
    usuarioDao: UsuarioDao,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onUsuarioActualizado: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var usuarioObjeto by remember { mutableStateOf<Usuario?>(null) }
    var esModoEdicion by remember { mutableStateOf(false) }
    var usuarioEditInput by remember { mutableStateOf("") }
    var contrasenaEditInput by remember { mutableStateOf("") }
    var fotoUriEdit by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(usuarioLogueado) {
        usuarioDao.obtenerUsuarioPorNombre(usuarioLogueado)?.let { user ->
            usuarioObjeto = user
            usuarioEditInput = user.usuario
            contrasenaEditInput = user.contrasena
            fotoUriEdit = user.fotoUri
        }
    }

    val selectorImagenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fotoUriEdit = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Cuenta", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2), titleContentColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val items = listOf("Inicio", "Escanear", "Historial", "Perfil")
                val icons = listOf(Icons.Default.Home, Icons.Default.PhotoCamera, Icons.Default.History, Icons.Default.Person)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(icons[items.indexOf(item)], contentDescription = item) },
                        label = { Text(item) },
                        selected = item == "Perfil",
                        onClick = { if (item != "Perfil") onNavigate(item) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFE0E0E0))
                    .clickable(enabled = esModoEdicion) { selectorImagenLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (!fotoUriEdit.isNullOrEmpty()) {
                    AndroidView(
                        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                        update = { imageView ->
                            try { imageView.setImageURI(Uri.parse(fotoUriEdit)) }
                            catch (e: Exception) { imageView.setImageResource(android.R.drawable.ic_menu_report_image) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(70.dp), tint = Color.Gray)
                }
                if (esModoEdicion) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("@$usuarioLogueado", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (!esModoEdicion) {
                OpcionMenuPerfil(Icons.Default.Edit, "Editar Datos del Perfil") { esModoEdicion = true }
                OpcionMenuPerfil(Icons.Default.Settings, "Modo Oscuro", trailingContent = {
                    Switch(checked = isDarkMode, onCheckedChange = onThemeChange)
                }) {}
                OpcionMenuPerfil(Icons.AutoMirrored.Filled.HelpOutline, "Ayuda y Soporte") {
                    Toast.makeText(context, "Soporte Conti 3D: soporte@conti.com", Toast.LENGTH_LONG).show()
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onCerrarSesion,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, null)
                    Spacer(Modifier.width(8.dp))
                    Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedTextField(
                    value = usuarioEditInput, onValueChange = { usuarioEditInput = it },
                    label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = contrasenaEditInput, onValueChange = { contrasenaEditInput = it },
                    label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { esModoEdicion = false }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = {
                        coroutineScope.launch {
                            usuarioObjeto?.let { user ->
                                val updated = user.copy(usuario = usuarioEditInput, contrasena = contrasenaEditInput, fotoUri = fotoUriEdit)
                                usuarioDao.actualizarUsuario(updated)
                                onUsuarioActualizado(usuarioEditInput)
                                esModoEdicion = false
                                Toast.makeText(context, "Perfil guardado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun OpcionMenuPerfil(icon: ImageVector, title: String, trailingContent: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, color = Color.Transparent) {
        Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF1976D2), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (trailingContent != null) trailingContent()
            else Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
