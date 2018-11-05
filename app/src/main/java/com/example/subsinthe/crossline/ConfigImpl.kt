package com.example.subsinthe.crossline

import android.content.Context
import android.content.SharedPreferences
import com.example.subsinthe.crossline.util.IObservableValue
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.Token

open class ConfigSection(context: Context, val _key: String) {
    val _storage = context.getSharedPreferences(_key, Context.MODE_PRIVATE)

    inline fun <reified T> pref(key: String, defaultValue: T): IObservableValue<T> =
        Preference.build(_storage, "$_key.$key", defaultValue)
}

@PublishedApi internal class Preference<T>(
    private val _impl: ObservableValue<T>,
    private val connection: Token
) : IObservableValue<T> {
    override var value
        get() = _impl.value
        set(value: T) { _impl.value = value }

    override fun subscribe(handler: (T) -> Unit) = _impl.subscribe(handler)

    companion object {
        inline fun <reified T> build(storage: SharedPreferences, key: String, default: T) =
            ObservableValue<T>(storage.get<T>(key, default)).let {
                Preference(it, it.subscribe { storage.put(key, it) })
            }
    }
}

@PublishedApi internal inline fun <reified T> SharedPreferences.put(key: String, value: T) {
    val e = edit()
    e.put(key, value)
    e.commit()
}

@PublishedApi internal inline fun <reified T> SharedPreferences.Editor.put(
    key: String,
    value: T
) = when (T::class) {
    Boolean::class -> putBoolean(key, value as Boolean)
    Float::class -> putFloat(key, value as Float)
    Int::class -> putInt(key, value as Int)
    Long::class -> putLong(key, value as Long)
    String::class -> putString(key, value as String)
    else -> throw IllegalArgumentException("Unexpected class ${T::class}")
}

@PublishedApi internal inline fun <reified T> SharedPreferences.get(
    key: String,
    default: T
) = when (T::class) {
    Boolean::class -> getBoolean(key, default as Boolean) as T
    Float::class -> getFloat(key, default as Float) as T
    Int::class -> getInt(key, default as Int) as T
    Long::class -> getLong(key, default as Long) as T
    String::class -> getString(key, default as String) as T
    else -> throw IllegalArgumentException("Unexpected class ${T::class}")
}
