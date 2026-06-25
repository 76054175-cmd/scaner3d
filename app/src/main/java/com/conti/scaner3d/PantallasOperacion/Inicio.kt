package com.conti.scaner3d.PantallasOperacion

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    usuario: String = "Usuario",
    correo: String = "usuario@conti.com",
    profileImageUri: Uri? = null,
    onNavigate: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit
) {
    var showProfileSheet by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }

    var displayUsuario by remember { mutableStateOf(usuario) }
    var displayCorreo by remember { mutableStateOf(correo) }
    var displayImageUri by remember { mutableStateOf(profileImageUri) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var tempUsuario by remember { mutableStateOf(displayUsuario) }
    var tempCorreo by remember { mutableStateOf(displayCorreo) }
    var tempPassword by remember { mutableStateOf("") }
    var tempImageUri by remember { mutableStateOf(displayImageUri) }
    var tempBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val primaryBlue = Color(0xFF0D47A1)
    val secondaryBlue = Color(0xFF1976D2)
    val lightBlue = if (isDarkTheme) Color(0xFF1E3A5F) else Color(0xFFE3F2FD)
    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val surfaceColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) tempImageUri = uri }
    )

    LaunchedEffect(showProfileSheet) {
        if (!showProfileSheet) {
            isEditingProfile = false
        }
    }

    LaunchedEffect(displayImageUri) {
        displayImageUri?.let { uri ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    bitmap = ImageDecoder.decodeBitmap(source).asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
                }
            } catch (e: Exception) {
                bitmap = null
            }
        }
    }

    LaunchedEffect(tempImageUri) {
        tempImageUri?.let { uri ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    tempBitmap = ImageDecoder.decodeBitmap(source).asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    tempBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
                }
            } catch (e: Exception) {
                tempBitmap = null
            }
        }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SCANNER 3D",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = if (isDarkTheme) Color.White else primaryBlue
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showProfileSheet = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(lightBlue)
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color.White else primaryBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor,
                tonalElevation = 8.dp
            ) {
                val items = listOf("Inicio", "Escanear", "Historial")
                val icons = listOf(Icons.Default.Home, Icons.Default.PhotoCamera, Icons.Default.History)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(icons[items.indexOf(item)], contentDescription = item) },
                        label = { Text(item, fontWeight = FontWeight.Medium) },
                        selected = item == "Inicio",
                        onClick = { if (item != "Inicio") onNavigate(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDarkTheme) Color.White else primaryBlue,
                            selectedTextColor = if (isDarkTheme) Color.White else primaryBlue,
                            indicatorColor = lightBlue,
                            unselectedIconColor = secondaryTextColor,
                            unselectedTextColor = secondaryTextColor
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(surfaceColor, lightBlue.copy(alpha = 0.5f))
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = surfaceColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(lightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = if (isDarkTheme) Color.White else primaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "¡Hola, $displayUsuario!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = "Hoy es un buen día para capturar algo nuevo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onNavigate("Escanear") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("NUEVO ESCANEO", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedCard(
                        onClick = { onNavigate("Historial") },
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color.DarkGray else Color.LightGray)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = secondaryBlue)
                            Text("Historial", fontWeight = FontWeight.Bold, color = secondaryBlue)
                        }
                    }

                    OutlinedCard(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color.DarkGray else Color.LightGray)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFBC02D))
                            Text("Consejos", fontWeight = FontWeight.Bold, color = secondaryTextColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Scanner 3D v1.0 • Conti Corp",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }

    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState,
            containerColor = surfaceColor,
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditingProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isEditingProfile = false
                                tempUsuario = displayUsuario
                                tempCorreo = displayCorreo
                                tempImageUri = displayImageUri
                                tempPassword = ""
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = textColor)
                        }
                        Text(
                            text = "EDITAR PERFIL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(lightBlue)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tempBitmap != null) {
                            Image(
                                bitmap = tempBitmap!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = if (isDarkTheme) Color.White else primaryBlue
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tempUsuario,
                        onValueChange = { tempUsuario = it },
                        label = { Text("Nombre de Usuario") },
                        leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null, tint = secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            focusedLabelColor = primaryBlue,
                            unfocusedBorderColor = secondaryTextColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    OutlinedTextField(
                        value = tempCorreo,
                        onValueChange = { tempCorreo = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            focusedLabelColor = primaryBlue,
                            unfocusedBorderColor = secondaryTextColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Nueva Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = secondaryTextColor) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = secondaryTextColor)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            focusedLabelColor = primaryBlue,
                            unfocusedBorderColor = secondaryTextColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    Button(
                        onClick = {
                            displayUsuario = tempUsuario
                            displayCorreo = tempCorreo
                            displayImageUri = tempImageUri
                            isEditingProfile = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        Text(
                            text = "GUARDAR CAMBIOS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(primaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayUsuario,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = displayCorreo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = lightBlue)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = textColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Modo Oscuro", color = textColor, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryBlue,
                                checkedTrackColor = lightBlue
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            tempUsuario = displayUsuario
                            tempCorreo = displayCorreo
                            tempImageUri = displayImageUri
                            tempPassword = ""
                            isEditingProfile = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkTheme) Color.White else primaryBlue),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color.White else primaryBlue)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Perfil")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showProfileSheet = false
                                    onNavigateToLogin()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión", color = Color.White)
                    }
                }
            }
        }
    }
}