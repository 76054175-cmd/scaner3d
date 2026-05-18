package com.conti.scaner3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Importamos tus paquetes
import com.conti.scaner3d.baseDatosLocal.AppDatabase
import com.conti.scaner3d.baseDatosLocal.Usuario
import com.conti.scaner3d.PantallaLogin.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Construir la base de datos
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "scaner3d-db" // Nombre del archivo SQLite que se guardará en el celular
        ).build()

        val usuarioDao = db.usuarioDao()

        // 2. ¡TRUCO PARA PROBAR! Insertamos un usuario administrador si no existe.
        // Se ejecuta en segundo plano.
        lifecycleScope.launch {
            usuarioDao.insertarUsuario(Usuario(usuario = "admin", contrasena = "1234"))
        }

        // 3. Mostrar la interfaz gráfica
        setContent {
            // Llamamos a tu pantalla y le inyectamos la conexión a BD
            LoginScreen(usuarioDao = usuarioDao)
        }
    }
}