package io.github.respectlytics

/**
 * Simple in-memory storage for SDK data.
 * This is a JVM-compatible version. For Android, this would use SharedPreferences.
 */
internal class Storage {
    private val data = mutableMapOf<String, String>()
    
    @Synchronized
    fun getString(key: String): String? = data[key]
    
    @Synchronized
    fun setString(key: String, value: String) {
        data[key] = value
    }
    
    @Synchronized
    fun remove(key: String) {
        data.remove(key)
    }
    
    @Synchronized
    fun clear() {
        data.clear()
    }
}
