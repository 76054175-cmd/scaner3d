package com.conti.scaner3d.baseDatosLocal

import androidx.room.*

// 1. ENTITY: Define cómo se verá tu tabla en SQLite
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuario: String,
    val contrasena: String
)

// 2. DAO (Data Access Object): Aquí escribes tus sentencias SQL
@Dao
interface UsuarioDao {
    // Consulta para validar el login. Devuelve el usuario si existe, o null si falla.
    @Query("SELECT * FROM usuarios WHERE usuario = :user AND contrasena = :pass")
    suspend fun login(user: String, pass: String): Usuario?

    // Método para insertar un usuario de prueba
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: Usuario)
}

// 3. DATABASE: El motor que une todo
@Database(entities = [Usuario::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
}