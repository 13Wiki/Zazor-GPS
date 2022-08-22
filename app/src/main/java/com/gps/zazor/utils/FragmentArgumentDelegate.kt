package com.gps.zazor.utils

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
/**
 * Class which simplify work with [Fragment] arguments
 */
class FragmentArgumentDelegate<T : Any> : ReadWriteProperty<Fragment, T?> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(thisRef: Fragment, property: KProperty<*>): T? =
        thisRef.arguments?.get(getExtra(property.name, thisRef::class.java)) as? T
    /**
     * Sets the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @param value the value to set.
     */
    @Suppress("UNCHECKED_CAST")
    override operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        val args = thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }
        val key = getExtra(property.name, thisRef::class.java)
        value?.let {
            when (it) {
                is Int -> args.putInt(key, it)
                is Long -> args.putLong(key, it)
                is CharSequence -> args.putCharSequence(key, it)
                is String -> args.putString(key, it)
                is Float -> args.putFloat(key, it)
                is Double -> args.putDouble(key, it)
                is Char -> args.putChar(key, it)
                is Short -> args.putShort(key, it)
                is Boolean -> args.putBoolean(key, it)
                is Serializable -> args.putSerializable(key, it)
                is Bundle -> args.putBundle(key, it)
                is Parcelable -> args.putParcelable(key, it)
                is Array<*> -> when {
                    it.isArrayOf<CharSequence>() -> args.putCharSequenceArray(key, it as Array<CharSequence>)
                    it.isArrayOf<String>() -> args.putStringArray(key, it as Array<String>)
                    it.isArrayOf<Parcelable>() -> args.putParcelableArray(key, it as Array<Parcelable>)
                    else -> propertyNotSupport(it, property)
                }
                is IntArray -> args.putIntArray(key, it)
                is LongArray -> args.putLongArray(key, it)
                is FloatArray -> args.putFloatArray(key, it)
                is DoubleArray -> args.putDoubleArray(key, it)
                is CharArray -> args.putCharArray(key, it)
                is ShortArray -> args.putShortArray(key, it)
                is BooleanArray -> args.putBooleanArray(key, it)
                else -> propertyNotSupport(it, property)
            }
        } ?: args.remove(key)
    }
    @JvmOverloads
    private fun <T> getExtra(extraName: String,
                     clazz: Class<T>? = null,
                     packageName: String = ""): String {
        return "${packageName}_${clazz?.simpleName ?: ""}_EXTRA_$extraName"
    }
    private fun propertyNotSupport(it: T, property: KProperty<*>): Nothing =
        throw IllegalStateException("Type ${it.javaClass.canonicalName} of property ${property.name} is not supported")
}