package com.mcal.common.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BridgeDataStore : PreferenceDataStore() {
    private var dataStoreImpl: DataStore<Preferences>? = null;

    fun attachDataStore(dataStore: DataStore<Preferences>){
        dataStoreImpl = dataStore
    }

    /**
     * Sets a [String] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see .getString
     */
    override fun putString(key: String?, value: String?) {
      value?.let {
        val savedString = stringPreferencesKey(key!!)
          CoroutineScope(Dispatchers.IO).launch {
              dataStoreImpl?.edit { prefs ->
                  prefs[savedString] = value
              }
          }
      }
    }

    /**
     * Sets a set of [String]s to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key    The name of the preference to modify
     * @param values The set of new values for the preference
     * @see .getStringSet
     */
    override fun putStringSet(key: String?, values: MutableSet<String>?) {
        //super.putStringSet(key, values)
    }

    /**
     * Sets an [Integer] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see .getInt
     */
    override fun putInt(key: String?, value: Int) {
        val savedString = intPreferencesKey(key!!)
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreImpl?.edit { prefs ->
                prefs[savedString] = value
            }
        }
    }

    /**
     * Sets a [Long] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see .getLong
     */
    override fun putLong(key: String?, value: Long) {
        val savedString = longPreferencesKey(key!!)
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreImpl?.edit { prefs ->
                prefs[savedString] = value
            }
        }
    }

    /**
     * Sets a [Float] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see .getFloat
     */
    override fun putFloat(key: String?, value: Float) {
        val savedString = floatPreferencesKey(key!!)
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreImpl?.edit { prefs ->
                prefs[savedString] = value
            }
        }
    }

    /**
     * Sets a [Boolean] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see .getBoolean
     */
    override fun putBoolean(key: String?, value: Boolean) {
        val savedString = booleanPreferencesKey(key!!)
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreImpl?.edit { prefs ->
                prefs[savedString] = value
            }
        }
    }

    /**
     * Retrieves a [String] value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see .putString
     */
    override fun getString(key: String?, defValue: String?): String? {
        var str: String?
        runBlocking {
            val readObj = stringPreferencesKey(key!!)
            val preferences = dataStoreImpl?.data?.first()
            str = preferences?.get(readObj) ?: defValue ?: ""
        }
        return str
    }

    /**
     * Retrieves a set of Strings from the data store.
     *
     * @param key       The name of the preference to retrieve
     * @param defValues Values to return if this preference does not exist in the storage
     * @return The values from the data store or the default return values
     * @see .putStringSet
     */
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        //return super.getStringSet(key, defValues)
        return null
    }

    /**
     * Retrieves a set of Strings from the data store.
     *
     * @param key       The name of the preference to retrieve
     * @param defValues Values to return if this preference does not exist in the storage
     * @return The values from the data store or the default return values
     * @see .putStringSet
     */
//    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
//        var str: Set<String>? = null
//        CoroutineScope(Dispatchers.Main).launch {
//            val readObj = stringSetPreferencesKey(key!!)
//
//            val preferences = dataStoreImpl?.data?.first()
//            str = preferences?.get(readObj) ?: defValues
//        }
//        return str
//    }

    /**
     * Retrieves an [Integer] value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see .putInt
     */
    override fun getInt(key: String?, defValue: Int): Int {
        var str: Int
        runBlocking {
            val readObj = intPreferencesKey(key!!)
            val preferences = dataStoreImpl?.data?.first()
            str = preferences?.get(readObj) ?: defValue
        }
        return str
    }

    /**
     * Retrieves a [Long] value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see .putLong
     */
    override fun getLong(key: String?, defValue: Long): Long {
        var str: Long
        runBlocking {
            val readObj = longPreferencesKey(key!!)
            val preferences = dataStoreImpl?.data?.first()
            str = preferences?.get(readObj) ?: defValue
        }
        return str
    }

    /**
     * Retrieves a [Float] value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see .putFloat
     */
    override fun getFloat(key: String?, defValue: Float): Float {
        var str: Float
        runBlocking {
            val readObj = floatPreferencesKey(key!!)
            val preferences = dataStoreImpl?.data?.first()
            str = preferences?.get(readObj) ?: defValue
        }
        return str
    }

    /**
     * Retrieves a [Boolean] value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see .getBoolean
     */
    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        var str: Boolean
        runBlocking {
            val readObj = booleanPreferencesKey(key!!)
            val preferences = dataStoreImpl?.data?.first()
            str = preferences?.get(readObj) ?: defValue
        }
        return str
    }


}