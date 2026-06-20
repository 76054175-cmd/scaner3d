package com.conti.scaner3d.PantallaLogin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conti.scaner3d.R
import com.conti.scaner3d.baseDatosLocal.Usuario
import com.conti.scaner3d.baseDatosLocal.UsuarioDao
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    usuarioDao: UsuarioDao,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }

    // Colores modernos
    val primaryBlue = Color(0xFF0D47A1)
    val secondaryBlue = Color(0xFF1976D2)
    val lightBlue = Color(0xFFE3F2FD)

    // Google Sign-In options
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            coroutineScope.launch {
                try {
                    auth.signInWithCredential(credential).await()
                    onLoginSuccess(account.displayName ?: account.email ?: "Google User")
                } catch (e: Exception) {
                    Toast.makeText(context, "Error en Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Error en Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    var usuarioInput by remember { mutableStateOf("") }
    var contrasenaInput by remember { mutableStateOf("") }
    var confirmarContrasenaInput by remember { mutableStateOf("") }

    var esModoRegistro by remember { mutableStateOf(false) }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var confirmarContrasenaVisible by remember { mutableStateOf(false) }

    var mostrarAlertaError by remember { mutableStateOf(false) }
    var mensajeAlertaError by remember { mutableStateOf("") }

    if (mostrarAlertaError) {
        AlertDialog(
            onDismissRequest = { mostrarAlertaError = false },
            confirmButton = {
                TextButton(onClick = { mostrarAlertaError = false }) {
                    Text("Aceptar", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Atención", fontWeight = FontWeight.Bold) },
            text = { Text(mensajeAlertaError) }
        )
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, lightBlue)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo o Icono
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(secondaryBlue.copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = primaryBlue,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (esModoRegistro) "Crea tu cuenta" else "Bienvenido",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryBlue
                )

                Text(
                    text = if (esModoRegistro) "Únete a la revolución del escaneo 3D" else "Ingresa tus datos para continuar",
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = usuarioInput,
                            onValueChange = { usuarioInput = it },
                            label = { Text("Usuario") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryBlue,
                                focusedLabelColor = primaryBlue
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = contrasenaInput,
                            onValueChange = { contrasenaInput = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                            trailingIcon = {
                                IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                                    Icon(
                                        imageVector = if (contrasenaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryBlue,
                                focusedLabelColor = primaryBlue
                            )
                        )

                        if (esModoRegistro) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmarContrasenaInput,
                                onValueChange = { confirmarContrasenaInput = it },
                                label = { Text("Confirmar Contraseña") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmarContrasenaVisible = !confirmarContrasenaVisible }) {
                                        Icon(
                                            imageVector = if (confirmarContrasenaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (confirmarContrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryBlue,
                                    focusedLabelColor = primaryBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (usuarioInput.isEmpty() || contrasenaInput.isEmpty()) {
                                    mensajeAlertaError = "Por favor, llena todos los campos obligatorios."
                                    mostrarAlertaError = true
                                    return@Button
                                }

                                if (esModoRegistro) {
                                    if (contrasenaInput != confirmarContrasenaInput) {
                                        mensajeAlertaError = "Las contraseñas no coinciden."
                                        mostrarAlertaError = true
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        try {
                                            val nuevoUsuario = Usuario(usuario = usuarioInput, contrasena = contrasenaInput)
                                            usuarioDao.insertarUsuario(nuevoUsuario)
                                            Toast.makeText(context, "¡Usuario registrado!", Toast.LENGTH_LONG).show()
                                            esModoRegistro = false
                                        } catch (e: Exception) {
                                            mensajeAlertaError = "Error al registrar usuario."
                                            mostrarAlertaError = true
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        val user = usuarioDao.login(usuarioInput, contrasenaInput)
                                        if (user != null) {
                                            onLoginSuccess(user.usuario)
                                        } else {
                                            mensajeAlertaError = "Credenciales incorrectas."
                                            mostrarAlertaError = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                        ) {
                            Text(
                                text = if (esModoRegistro) "CREAR CUENTA" else "ENTRAR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    Text(" O ", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = {
                        launcher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (esModoRegistro) "Regístrate con Google" else "Entrar con Google",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        esModoRegistro = !esModoRegistro
                        usuarioInput = ""
                        contrasenaInput = ""
                        confirmarContrasenaInput = ""
                        focusManager.clearFocus()
                    }
                ) {
                    Text(
                        text = if (esModoRegistro) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate aquí",
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
