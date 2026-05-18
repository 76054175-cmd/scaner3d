package com.conti.scaner3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// 1. IMPORTANTE: Añadimos las herramientas de estado de Compose
import androidx.compose.runtime.*

// Importamos tus paquetes
import com.conti.scaner3d.baseDatosLocal.AppDatabase
import com.conti.scaner3d.baseDatosLocal.Usuario
import com.conti.scaner3d.PantallaLogin.LoginScreen
// 2. IMPORTANTE: Importamos la pantalla de Inicio desde su paquete
import com.conti.scaner3d.PantallasOperacion.InicioScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Construir la base de datos
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "scaner3d-db"
        ).build()

        val usuarioDao = db.usuarioDao()

        // 2. ¡TRUCO PARA PROBAR! Insertamos un usuario administrador si no existe.
        lifecycleScope.launch {
            usuarioDao.insertarUsuario(Usuario(usuario = "admin", contrasena = "1234"))
        }

        // 3. Mostrar la interfaz gráfica con el control de navegación
        setContent {
            // Variables para controlar la navegación y recordar al usuario
            var pantallaActual by remember { mutableStateOf("login") }
            var usuarioLogueado by remember { mutableStateOf("") }

            // Dependiendo del valor de 'pantallaActual', mostramos una u otra
            when (pantallaActual) {
                "login" -> {
                    LoginScreen(
                        usuarioDao = usuarioDao,
                        onLoginSuccess = { nombreUsuario ->
                            // Cuando el login es exitoso, guardamos el nombre y cambiamos de pantalla
                            usuarioLogueado = nombreUsuario
                            pantallaActual = "inicio"
                        }
                    )
                }
                "inicio" -> {
                    InicioScreen(
                        usuario = usuarioLogueado,
                        onCerrarSesion = {
                            // Si el usuario presiona cerrar sesión, vuelve a la pantalla de login
                            pantallaActual = "login"
                        }
                    )
                }
            }
        }
    }
}