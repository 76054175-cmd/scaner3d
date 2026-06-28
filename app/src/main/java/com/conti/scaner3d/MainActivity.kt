package com.conti.scaner3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.conti.scaner3d.baseDatosLocal.AppDatabase
import com.conti.scaner3d.baseDatosLocal.Usuario
import com.conti.scaner3d.PantallaLogin.LoginScreen
import com.conti.scaner3d.PantallasOperacion.InicioScreen
import com.conti.scaner3d.PantallasOperacion.EscanearScreen
import com.conti.scaner3d.PantallasOperacion.HistorialScreen
import com.conti.scaner3d.PantallasOperacion.PerfilScreen
import com.conti.scaner3d.PantallasOperacion.VisualizarModeloScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "scaner3d-db"
        ).fallbackToDestructiveMigration().build()

        val usuarioDao = db.usuarioDao()
        val escaneoDao = db.escaneoDao()

        lifecycleScope.launch {
            try {
                if (usuarioDao.obtenerUsuarioPorNombre("admin") == null) {
                    usuarioDao.insertarUsuario(Usuario(usuario = "admin", contrasena = "1234"))
                }
            } catch (e: Exception) { }
        }

        setContent {
            var pantallaActual by remember { mutableStateOf("login") }
            var usuarioLogueado by remember { mutableStateOf("") }
            var jsonPathAVisualizar by remember { mutableStateOf("") }

            val systemTheme = isSystemInDarkTheme()
            var userThemePreference by remember { mutableStateOf<Boolean?>(null) }
            val isDarkMode = userThemePreference ?: systemTheme

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
                                isDarkMode = isDarkMode,
                                onLoginSuccess = { nombreUsuario: String ->
                                    usuarioLogueado = nombreUsuario
                                    pantallaActual = "Inicio"
                                }
                            )
                        }
                        "Inicio" -> {
                            InicioScreen(
                                usuario = usuarioLogueado,
                                usuarioDao = usuarioDao,
                                isDarkMode = isDarkMode,
                                onThemeChange = { nuevoEstado -> userThemePreference = nuevoEstado },
                                onNavigate = { nuevaPantalla -> pantallaActual = nuevaPantalla },
                                onNavigateToLogin = {
                                    usuarioLogueado = ""
                                    pantallaActual = "login"
                                },
                                onUsuarioActualizado = { nuevoNombre ->
                                    usuarioLogueado = nuevoNombre
                                }
                            )
                        }
                        "Escanear" -> {
                            EscanearScreen(
                                escaneoDao = escaneoDao,
                                isDarkMode = isDarkMode, // <--- AÑADIR ESTO
                                onNavigate = { nuevaPantalla: String -> pantallaActual = nuevaPantalla }
                            )
                        }
                        "Historial" -> {
                            HistorialScreen(
                                escaneoDao = escaneoDao,
                                isDarkMode = isDarkMode, // <--- AÑADIR ESTO
                                onNavigate = { nuevaPantalla: String ->
                                    if (nuevaPantalla.startsWith("VisualizarModelo/")) {
                                        jsonPathAVisualizar = nuevaPantalla.removePrefix("VisualizarModelo/")
                                        pantallaActual = "VisualizarModelo"
                                    } else {
                                        pantallaActual = nuevaPantalla
                                    }
                                }
                            )
                        }
                        "VisualizarModelo" -> {
                            VisualizarModeloScreen(
                                jsonPath = jsonPathAVisualizar,
                                isDarkMode = isDarkMode, // <--- AÑADIR ESTO
                                onBack = { pantallaActual = "Historial" }
                            )
                        }
                        "Perfil" -> {
                            PerfilScreen(
                                usuarioLogueado = usuarioLogueado,
                                usuarioDao = usuarioDao,
                                isDarkMode = isDarkMode, // <--- AÑADIR ESTO
                                onThemeChange = { nuevoEstado: Boolean -> userThemePreference = nuevoEstado },
                                onCerrarSesion = {
                                    usuarioLogueado = ""
                                    pantallaActual = "login"
                                },
                                onNavigate = { nuevaPantalla: String -> pantallaActual = nuevaPantalla },
                                onUsuarioActualizado = { nuevoNombre: String ->
                                    usuarioLogueado = nuevoNombre
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}