package com.conti.scaner3d.baseDatosLocal

import androidx.room.*

// 1. ENTIDAD DE USUARIO ORIGINAL
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var usuario: String,
    var contrasena: String,
    val fotoUri: String? = null
)

// 2. NUEVA ENTIDAD PARA LOS ESCANEOS
@Entity(tableName = "escaneos")
data class Escaneo3D(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val fecha: String,
    val imagenUri: String // Guarda la ruta real de la foto en el teléfono
)

// 3. DAO DE USUARIO
@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE usuario = :user AND contrasena = :pass")
    suspend fun login(user: String, pass: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE usuario = :user LIMIT 1")
    suspend fun obtenerUsuarioPorNombre(user: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: Usuario)

    @Update
    suspend fun actualizarUsuario(usuario: Usuario)
}

// 4. NUEVO DAO PARA HISTORIAL DE ESCANEOS
@Dao
interface EscaneoDao {
    @Query("SELECT * FROM escaneos ORDER BY id DESC")
    suspend fun obtenerTodos(): List<Escaneo3D>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(escaneo: Escaneo3D)

    @Update
    suspend fun actualizar(escaneo: Escaneo3D)

    @Delete
    suspend fun eliminar(escaneo: Escaneo3D)
}

// 5. BASE DE DATOS PRINCIPAL (Versión 4)
@Database(entities = [Usuario::class, Escaneo3D::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun escaneoDao(): EscaneoDao
}