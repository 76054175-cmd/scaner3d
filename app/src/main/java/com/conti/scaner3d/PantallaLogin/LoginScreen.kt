package com.conti.scaner3d.PantallaLogin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conti.scaner3d.baseDatosLocal.Usuario
import com.conti.scaner3d.baseDatosLocal.UsuarioDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    usuarioDao: UsuarioDao,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados para controlar lo que escribe el usuario
    var usuarioInput by remember { mutableStateOf("") }
    var contrasenaInput by remember { mutableStateOf("") }
    var confirmarContrasenaInput by remember { mutableStateOf("") }

    // Estados de control visual
    var esModoRegistro by remember { mutableStateOf(false) }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var confirmarContrasenaVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título superior adaptativo
            Text(
                text = if (esModoRegistro) "Crear Cuenta" else "Iniciar Sesión",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Text(
                text = if (esModoRegistro) "Regístrate para usar Scanner 3D" else "Introduce tus credenciales para continuar",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Campo de Texto: Usuario
            OutlinedTextField(
                value = usuarioInput,
                onValueChange = { usuarioInput = it },
                label = { Text("Usuario") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Texto: Contraseña
            OutlinedTextField(
                value = contrasenaInput,
                onValueChange = { contrasenaInput = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                        Icon(
                            imageVector = if (contrasenaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Campo de Texto Opcional: Confirmar Contraseña (Solo visible en Registro)
            if (esModoRegistro) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmarContrasenaInput,
                    onValueChange = { confirmarContrasenaInput = it },
                    label = { Text("Confirmar Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmarContrasenaVisible = !confirmarContrasenaVisible }) {
                            Icon(
                                imageVector = if (confirmarContrasenaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (confirmarContrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Acción Dinámico (Ingresar o Registrarse)
            Button(
                onClick = {
                    if (usuarioInput.isEmpty() || contrasenaInput.isEmpty()) {
                        Toast.makeText(context, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (esModoRegistro) {
                        // ---- ACCIÓN: REGISTRAR UN NUEVO USUARIO ----
                        if (contrasenaInput != confirmarContrasenaInput) {
                            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                // Se crea la entidad con id = 0 para que Room lo autogenere
                                val nuevoUsuario = Usuario(usuario = usuarioInput, contrasena = contrasenaInput)
                                usuarioDao.insertarUsuario(nuevoUsuario)

                                Toast.makeText(context, "¡Usuario registrado con éxito!", Toast.LENGTH_LONG).show()

                                // Limpiamos campos y volvemos al modo de login normal
                                confirmarContrasenaInput = ""
                                esModoRegistro = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al registrar: El usuario podría ya existir", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // ---- ACCIÓN: INICIAR SESIÓN (Usa tu método exacto del DAO) ----
                        coroutineScope.launch {
                            val user = usuarioDao.login(usuarioInput, contrasenaInput)

                            if (user != null) {
                                onLoginSuccess(user.usuario)
                            } else {
                                Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text(
                    text = if (esModoRegistro) "Registrarse" else "Ingresar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Texto inferior para cambiar entre Iniciar Sesión y Registrarse
            TextButton(
                onClick = {
                    esModoRegistro = !esModoRegistro
                    // Limpiamos los campos al cambiar de modo para que quede limpio
                    usuarioInput = ""
                    contrasenaInput = ""
                    confirmarContrasenaInput = ""
                }
            ) {
                Text(
                    text = if (esModoRegistro) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate aquí",
                    color = Color(0xFF1976D2),
                    fontSize = 14.sp
                )
            }
        }
    }
}