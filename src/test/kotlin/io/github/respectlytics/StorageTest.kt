package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageTest {
    
    @Test
    fun `test setString and getString`() {
        val storage = Storage()
        
        storage.setString("test_key", "test_value")
        assertEquals("test_value", storage.getString("test_key"))
    }
    
    @Test
    fun `test getString for non-existent key returns null`() {
        val storage = Storage()
        
        assertNull(storage.getString("non_existent_key"))
    }
    
    @Test
    fun `test overwrite existing value`() {
        val storage = Storage()
        
        storage.setString("key", "value1")
        assertEquals("value1", storage.getString("key"))
        
        storage.setString("key", "value2")
        assertEquals("value2", storage.getString("key"))
    }
    
    @Test
    fun `test remove key`() {
        val storage = Storage()
        
        storage.setString("key", "value")
        assertEquals("value", storage.getString("key"))
        
        storage.remove("key")
        assertNull(storage.getString("key"))
    }
    
    @Test
    fun `test remove non-existent key does not throw`() {
        val storage = Storage()
        
        storage.remove("non_existent_key")
        assertNull(storage.getString("non_existent_key"))
    }
    
    @Test
    fun `test clear removes all keys`() {
        val storage = Storage()
        
        storage.setString("key1", "value1")
        storage.setString("key2", "value2")
        storage.setString("key3", "value3")
        
        assertEquals("value1", storage.getString("key1"))
        assertEquals("value2", storage.getString("key2"))
        assertEquals("value3", storage.getString("key3"))
        
        storage.clear()
        
        assertNull(storage.getString("key1"))
        assertNull(storage.getString("key2"))
        assertNull(storage.getString("key3"))
    }
    
    @Test
    fun `test multiple storage instances are independent`() {
        val storage1 = Storage()
        val storage2 = Storage()
        
        storage1.setString("key", "value1")
        storage2.setString("key", "value2")
        
        assertEquals("value1", storage1.getString("key"))
        assertEquals("value2", storage2.getString("key"))
    }
    
    @Test
    fun `test empty string value`() {
        val storage = Storage()
        
        storage.setString("key", "")
        assertEquals("", storage.getString("key"))
    }
    
    @Test
    fun `test special characters in value`() {
        val storage = Storage()
        
        val specialValue = "value with spaces, symbols: !@#$%^&*(), and unicode: 你好 🎉"
        storage.setString("key", specialValue)
        assertEquals(specialValue, storage.getString("key"))
    }
    
    @Test
    fun `test long value`() {
        val storage = Storage()
        
        val longValue = "a".repeat(10000)
        storage.setString("key", longValue)
        assertEquals(longValue, storage.getString("key"))
    }
}
