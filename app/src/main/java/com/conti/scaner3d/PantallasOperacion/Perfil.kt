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
    onReturnHome: () -> Unit,
    onCerrarSesion: () -> Unit,
    onNavigate: (String) -> Unit,
    onUsuarioActualizado: (String) -> Unit
) {
    val selectedItem = 3
    val bottomNavItems = listOf("Inicio", "Escanear", "Historial", "Perfil")
    val bottomNavIcons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.BookmarkBorder, Icons.Default.Person)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Datos del usuario desde Room
    var usuarioObjeto by remember { mutableStateOf<Usuario?>(null) }

    // Estados de edición
    var esModoEdicion by remember { mutableStateOf(false) }
    var usuarioEditInput by remember { mutableStateOf("") }
    var contrasenaEditInput by remember { mutableStateOf("") }
    var fotoUriEdit by remember { mutableStateOf<String?>(null) }

    // Cargar datos en tiempo real al abrir la pantalla
    LaunchedEffect(usuarioLogueado) {
        val user = usuarioDao.obtenerUsuarioPorNombre(usuarioLogueado)
        if (user != null) {
            usuarioObjeto = user
            usuarioEditInput = user.usuario
            contrasenaEditInput = user.contrasena
            fotoUriEdit = user.fotoUri
        }
    }

    // Selector nativo de galería de fotos
    val selectorImagenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fotoUriEdit = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
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
                        onClick = { if (item != "Perfil") onNavigate(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2), selectedTextColor = Color(0xFF1976D2),
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent
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
                .verticalScroll(rememberScrollState()) // Añadido soporte de scroll por seguridad de espacio
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- FOTO DE PERFIL ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0))
                    .clickable(enabled = esModoEdicion) {
                        selectorImagenLauncher.launch("image/*")
                    },
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
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color.Gray)
                }

                if (esModoEdicion) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre de usuario siempre visible arriba
            Text(
                text = "@$usuarioLogueado",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // --- CONTROL DE VISTAS (MENÚ COMPLETO O FORMULARIO DE EDICIÓN) ---
            if (!esModoEdicion) {

                // 1. NUEVA OPCIÓN: EDITAR PERFIL
                OpcionMenuPerfil(
                    icon = Icons.Default.Edit,
                    title = "Editar Perfil",
                    onClick = { esModoEdicion = true }
                )

                // 2. OPCIÓN: INFORMACIÓN PERSONAL
                OpcionMenuPerfil(
                    icon = Icons.Default.Info,
                    title = "Información Personal",
                    onClick = {
                        Toast.makeText(context, "Información Personal seleccionada", Toast.LENGTH_SHORT).show()
                    }
                )

                // 3. OPCIÓN: NOTIFICACIONES
                OpcionMenuPerfil(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    onClick = {
                        Toast.makeText(context, "Ajustes de Notificaciones", Toast.LENGTH_SHORT).show()
                    }
                )

                // 4. OPCIÓN: LISTA DE DESEOS
                OpcionMenuPerfil(
                    icon = Icons.Default.Favorite,
                    title = "Lista de Deseos",
                    onClick = {
                        Toast.makeText(context, "Abriendo Lista de Deseos", Toast.LENGTH_SHORT).show()
                    }
                )

                // 5. OPCIÓN: MODO OSCURO (Mantiene el Switch original)
                OpcionMenuPerfil(
                    icon = Icons.Default.Settings,
                    title = "Modo Oscuro",
                    onClick = {},
                    trailingContent = {
                        Switch(checked = isDarkMode, onCheckedChange = onThemeChange)
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 6. OPCIÓN: CERRAR SESIÓN (Al fondo)
                OpcionMenuPerfil(
                    icon = Icons.Default.ExitToApp,
                    title = "Cerrar Sesión",
                    iconColor = Color.Red,
                    textColor = Color.Red,
                    showArrow = false,
                    onClick = onCerrarSesion
                )

            } else {
                // --- FORMULARIO DE EDICIÓN (Aparece únicamente al pulsar "Editar Perfil") ---
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Modificar Datos de la Cuenta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = usuarioEditInput,
                    onValueChange = { usuarioEditInput = it },
                    label = { Text("Nuevo nombre de usuario") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = contrasenaEditInput,
                    onValueChange = { contrasenaEditInput = it },
                    label = { Text("Nueva contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Botón Cancelar
                    OutlinedButton(
                        onClick = {
                            // Restaurar valores y salir
                            usuarioObjeto?.let {
                                usuarioEditInput = it.usuario
                                contrasenaEditInput = it.contrasena
                                fotoUriEdit = it.fotoUri
                            }
                            esModoEdicion = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Botón Guardar
                    Button(
                        onClick = {
                            if (usuarioEditInput.isEmpty() || contrasenaEditInput.isEmpty()) {
                                Toast.makeText(context, "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            coroutineScope.launch {
                                usuarioObjeto?.let { user ->
                                    val usuarioActualizado = user.copy(
                                        usuario = usuarioEditInput,
                                        contrasena = contrasenaEditInput,
                                        fotoUri = fotoUriEdit
                                    )
                                    usuarioDao.actualizarUsuario(usuarioActualizado)
                                    usuarioObjeto = usuarioActualizado

                                    onUsuarioActualizado(usuarioEditInput)
                                    Toast.makeText(context, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show()
                                    esModoEdicion = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- COMPOSABLE AUXILIAR PARA DISEÑAR LAS OPCIONES EN FORMA DE LISTA ---
@Composable
fun OpcionMenuPerfil(
    icon: ImageVector,
    title: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    iconColor: Color = Color(0xFF1976D2),
    showArrow: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (showArrow) {
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}