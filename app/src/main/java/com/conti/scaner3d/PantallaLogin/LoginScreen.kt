package com.conti.scaner3d.PantallaLogin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.conti.scaner3d.baseDatosLocal.UsuarioDao
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    usuarioDao: UsuarioDao,
    onLoginSuccess: (String) -> Unit // <-- Recibe la función para cambiar de pantalla
) {
    // Variables de estado para guardar lo que el usuario escribe
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    // ¡NUEVO!: Variable de estado para controlar si se muestra la alerta flotante
    var mostrarAlertaError by remember { mutableStateOf(false) }

    // Necesario para ejecutar funciones 'suspend' (base de datos) en Compose
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para el Usuario
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de texto para la Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation() // Oculta la contraseña con asteriscos
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para Ingresar
        Button(onClick = {
            // Buscamos en la base de datos en segundo plano
            coroutineScope.launch {
                val userEncontrado = usuarioDao.login(username, password)

                if (userEncontrado != null) {
                    mensaje = "¡Bienvenido ${userEncontrado.usuario}!"
                    // Activamos el cambio de pantalla pasándole el nombre del usuario logueado
                    onLoginSuccess(userEncontrado.usuario)
                } else {
                    // ¡NUEVO!: En lugar de solo cambiar el texto de 'mensaje', activamos la alerta
                    mostrarAlertaError = true
                }
            }
        }) {
            Text("Ingresar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Muestra los mensajes de bienvenida (si los hay)
        if (mensaje.isNotEmpty()) {
            Text(text = mensaje, color = MaterialTheme.colorScheme.primary)
        }
    }

    // ¡NUEVO!: Diseño y lógica de la Alerta Flotante
    if (mostrarAlertaError) {
        AlertDialog(
            onDismissRequest = {
                // Esto se ejecuta si el usuario toca fuera del cuadro flotante
                // Puedes dejarlo vacío si quieres obligarlo a presionar "Aceptar"
                mostrarAlertaError = false
            },
            title = {
                Text(text = "Datos Incorrectos")
            },
            text = {
                Text("El usuario o la contraseña ingresados son incorrectos. Por favor, verifica tus datos y vuelve a intentarlo.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Acciones al hacer clic en "Aceptar"
                        mostrarAlertaError = false // 1. Ocultar la alerta
                        username = ""              // 2. Limpiar campo de usuario
                        password = ""              // 3. Limpiar campo de contraseña
                        mensaje = ""               // 4. Limpiar cualquier texto de error residual
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}