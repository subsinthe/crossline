package com.example.subsinthe.crossline

import android.support.v7.preference.EditTextPreference
import com.example.subsinthe.crossline.util.IMutableObservableValue

fun EditTextPreference.registerProperty(
    property: IMutableObservableValue<String>,
    validator: (String) -> Boolean
) {
    setText(property.value)
    setOnPreferenceChangeListener { _, newValue ->
        val new = newValue!!.toString()
        validator(new).also { valid ->
            if (valid)
                property.value = new
        }
    }
}
