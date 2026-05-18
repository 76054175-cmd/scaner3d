package com.conti.scaner3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

// Importaciones de tu base de datos
import com.conti.scaner3d.baseDatosLocal.AppDatabase
import com.conti.scaner3d.baseDatosLocal.Usuario

// Importaciones de tus pantallas
import com.conti.scaner3d.PantallaLogin.LoginScreen
import com.conti.scaner3d.PantallasOperacion.InicioScreen
import com.conti.scaner3d.PantallasOperacion.EscanearScreen
import com.conti.scaner3d.PantallasOperacion.HistorialScreen
import com.conti.scaner3d.PantallasOperacion.PerfilScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de la Base de Datos
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "scaner3d-db"
        ).build()

        val usuarioDao = db.usuarioDao()

        lifecycleScope.launch {
            try {
                usuarioDao.insertarUsuario(Usuario(usuario = "admin", contrasena = "1234"))
            } catch (e: Exception) {
                // Ignorar si el usuario ya existe
            }
        }

        setContent {
            // Variables de estado
            var pantallaActual by remember { mutableStateOf("login") }
            var usuarioLogueado by remember { mutableStateOf("") }
            var isDarkMode by remember { mutableStateOf(false) }

            // Configurar el tema según el estado
            val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (pantallaActual) {
                        "login" -> {
                            LoginScreen(
                                usuarioDao = usuarioDao,
                                onLoginSuccess = { nombreUsuario: String ->
                                    usuarioLogueado = nombreUsuario
                                    pantallaActual = "Inicio"
                                }
                            )
                        }
                        "Inicio" -> {
                            InicioScreen(
                                onNavigate = { nuevaPantalla: String -> pantallaActual = nuevaPantalla }
                            )
                        }
                        "Escanear" -> {
                            EscanearScreen(
                                onNavigate = { nuevaPantalla: String -> pantallaActual = nuevaPantalla }
                            )
                        }
                        "Historial" -> {
                            HistorialScreen(
                                onNavigate = { nuevaPantalla: String -> pantallaActual = nuevaPantalla }
                            )
                        }
                        "Perfil" -> {
                            PerfilScreen(
                                isDarkMode = isDarkMode,
                                // SOLUCIÓN: Declaramos explícitamente que es Boolean
                                onThemeChange = { nuevoEstado: Boolean ->
                                    isDarkMode = nuevoEstado
                                },
                                onReturnHome = { pantallaActual = "Inicio" },
                                onCerrarSesion = {
                                    usuarioLogueado = ""
                                    pantallaActual = "login"
                                },
                                // SOLUCIÓN: Declaramos explícitamente que es String
                                onNavigate = { nuevaPantalla: String ->
                                    pantallaActual = nuevaPantalla
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}