package com.conti.scaner3d.baseDatosLocal

import androidx.room.*

// 1. ENTITY: Agregamos el campo "fotoUri" para guardar la ruta de la imagen
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var usuario: String,
    var contrasena: String,
    val fotoUri: String? = null // Guarda la ubicación de la foto de perfil
)

// 2. DAO: Agregamos funciones para buscar por nombre y actualizar
@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE usuario = :user AND contrasena = :pass")
    suspend fun login(user: String, pass: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE usuario = :user LIMIT 1")
    suspend fun obtenerUsuarioPorNombre(user: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: Usuario)

    @Update // Nueva función nativa para guardar cambios del perfil
    suspend fun actualizarUsuario(usuario: Usuario)
}

// 3. DATABASE: Subimos la versión a 2 debido al cambio de columnas
@Database(entities = [Usuario::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
}