package com.safehaven.affirmations.widget

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoodWidgetPreferencesTest {
    @Test
    fun load_returnsDefaultsWhenUnset() {
        val preferences = MoodWidgetPreferences(InMemorySharedPreferences())

        val config = preferences.load(appWidgetId = 42)

        assertEquals(MoodWidgetBackgroundStyle.WHITE, config.backgroundStyle)
        assertEquals(MoodWidgetConfig.DEFAULT_TRANSPARENCY, config.transparency, 0.001f)
    }

    @Test
    fun saveAndLoad_roundTripsValues() {
        val backing = InMemorySharedPreferences()
        val preferences = MoodWidgetPreferences(backing)
        val expected = MoodWidgetConfig(
            backgroundStyle = MoodWidgetBackgroundStyle.BLACK,
            transparency = 0.42f,
        )

        preferences.save(appWidgetId = 7, config = expected)

        assertEquals(expected, preferences.load(appWidgetId = 7))
        assertEquals(
            MoodWidgetBackgroundStyle.BLACK.name,
            backing.getString(MoodWidgetPreferences.keyBackgroundStyle(7), null),
        )
        assertEquals(0.42f, backing.getFloat(MoodWidgetPreferences.keyTransparency(7), -1f), 0.001f)
    }

    @Test
    fun load_clampsStoredTransparencyIntoRange() {
        val backing = InMemorySharedPreferences()
        backing.edit()
            .putString(MoodWidgetPreferences.keyBackgroundStyle(3), MoodWidgetBackgroundStyle.WHITE.name)
            .putFloat(MoodWidgetPreferences.keyTransparency(3), 1.5f)
            .apply()
        val preferences = MoodWidgetPreferences(backing)

        assertEquals(1f, preferences.load(appWidgetId = 3).transparency, 0.001f)
    }

    @Test
    fun remove_clearsStoredWidgetConfig() {
        val backing = InMemorySharedPreferences()
        val preferences = MoodWidgetPreferences(backing)
        preferences.save(
            appWidgetId = 9,
            config = MoodWidgetConfig(
                backgroundStyle = MoodWidgetBackgroundStyle.BLACK,
                transparency = 0.25f,
            ),
        )

        preferences.remove(appWidgetId = 9)

        assertNull(backing.getString(MoodWidgetPreferences.keyBackgroundStyle(9), null))
        assertEquals(
            MoodWidgetConfig.DEFAULT_TRANSPARENCY,
            preferences.load(appWidgetId = 9).transparency,
            0.001f,
        )
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String, defValue: String?): String? =
            values[key] as? String ?: defValue

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (values[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String, defValue: Int): Int =
            values[key] as? Int ?: defValue

        override fun getLong(key: String, defValue: Long): Long =
            values[key] as? Long ?: defValue

        override fun getFloat(key: String, defValue: Float): Float =
            values[key] as? Float ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun contains(key: String): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
                pending[key] = value
            }

            override fun putStringSet(
                key: String,
                values: MutableSet<String>?,
            ): SharedPreferences.Editor = apply {
                pending[key] = values
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
                pending[key] = value
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
                pending[key] = value
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
                pending[key] = value
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
                pending[key] = value
            }

            override fun remove(key: String): SharedPreferences.Editor = apply {
                removals += key
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearAll = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) {
                    values.clear()
                }
                removals.forEach(values::remove)
                values.putAll(pending)
                pending.clear()
                removals.clear()
                clearAll = false
            }
        }
    }
}
