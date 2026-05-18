package com.conti.scaner3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Importaciones de Compose para manejar estados
import androidx.compose.runtime.*

// Importaciones de tu Base de Datos
import com.conti.scaner3d.baseDatosLocal.AppDatabase
import com.conti.scaner3d.baseDatosLocal.Usuario

// Importaciones de TODAS tus pantallas
import com.conti.scaner3d.PantallaLogin.LoginScreen
import com.conti.scaner3d.PantallasOperacion.InicioScreen
import com.conti.scaner3d.PantallasOperacion.EscanearScreen
import com.conti.scaner3d.PantallasOperacion.HistorialScreen
import com.conti.scaner3d.PantallasOperacion.PerfilScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Construir la base de datos
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "scaner3d-db" // Nombre del archivo SQLite
        ).build()

        val usuarioDao = db.usuarioDao()

        // 2. ¡TRUCO PARA PROBAR! Insertamos un usuario administrador si no existe.
        lifecycleScope.launch {
            try {
                usuarioDao.insertarUsuario(Usuario(usuario = "admin", contrasena = "1234"))
            } catch (e: Exception) {
                // Si el usuario ya existe, Room podría lanzar un error dependiendo de cómo lo configuraste.
                // Lo capturamos para que no cierre la app.
            }
        }

        // 3. Mostrar la interfaz gráfica y manejar la navegación
        setContent {
            // Variables de estado para controlar la app
            var pantallaActual by remember { mutableStateOf("login") }
            var usuarioLogueado by remember { mutableStateOf("") }

            // Enrutador principal de la aplicación
            when (pantallaActual) {

                "login" -> {
                    LoginScreen(
                        usuarioDao = usuarioDao,
                        onLoginSuccess = { nombreUsuario ->
                            // Guardamos el nombre y vamos a Inicio
                            usuarioLogueado = nombreUsuario
                            pantallaActual = "Inicio"
                        }
                    )
                }

                "Inicio" -> {
                    InicioScreen(
                        usuario = usuarioLogueado,
                        onNavigate = { destino ->
                            pantallaActual = destino // Puede ser "Escanear", "Historial" o "Perfil"
                        }
                    )
                }

                "Escanear" -> {
                    EscanearScreen(
                        onNavigate = { destino ->
                            pantallaActual = destino
                        }
                    )
                }

                "Historial" -> {
                    HistorialScreen(
                        onNavigate = { destino ->
                            pantallaActual = destino
                        }
                    )
                }

                "Perfil" -> {
                    PerfilScreen(
                        onReturnHome = {
                            pantallaActual = "Inicio"
                        },
                        onCerrarSesion = {
                            usuarioLogueado = ""
                            pantallaActual = "login"
                        },
                        // <-- AGREGAR ESTA LÍNEA AQUÍ
                        onNavigate = { destino ->
                            pantallaActual = destino
                        }
                    )
                }
            }
        }
    }
}