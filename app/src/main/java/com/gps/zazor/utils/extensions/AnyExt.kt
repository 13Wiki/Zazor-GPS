package com.gps.zazor.utils.extensions

fun Any.getPrivateField(fieldName: String): Any? =
    try {
        javaClass.getDeclaredField(fieldName).apply {
            isAccessible = true
        }.get(this)
    } catch (exc: NoSuchFieldException) {
        null
    }

fun Any.callMethod(methodName: String): Any? {
    return try {
        this.javaClass.getMethod(methodName).invoke(this)
    } catch (e: Exception) {
        null
    }
}