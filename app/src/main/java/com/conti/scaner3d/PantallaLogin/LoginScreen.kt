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
fun LoginScreen(usuarioDao: UsuarioDao) {
    // Variables de estado para guardar lo que el usuario escribe
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    // Necesario para ejecutar funciones 'suspend' (base de datos) en Compose
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation() // Oculta la contraseña
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Cuando hacen clic, buscamos en la base de datos
            coroutineScope.launch {
                val userEncontrado = usuarioDao.login(username, password)

                if (userEncontrado != null) {
                    mensaje = "¡Bienvenido ${userEncontrado.usuario}!"
                } else {
                    mensaje = "Usuario o contraseña incorrectos"
                }
            }
        }) {
            Text("Ingresar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Muestra el resultado del login
        Text(text = mensaje, color = MaterialTheme.colorScheme.primary)
    }
}