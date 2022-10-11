package com.mcal.common.utils

import java.lang.reflect.InvocationTargetException

object ReflectionHelper {
    fun createInstance(clsName: String, parameterTypes: Array<Class<*>?>, parameterValues: Array<Any?>): Any? {
        try {
            val c = Class.forName(clsName)
            val con = c.getConstructor(*parameterTypes)
            return con.newInstance(*parameterValues)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun invokeMethod(className: String, methodName: String, obj: Any?, parameterTypes: Array<Class<*>?>, parameterValues: Array<Any?>): Any? {
        try {
            val objClass = Class.forName(className)
            val method = objClass.getMethod(methodName, *parameterTypes)
            return method.invoke(obj, *parameterValues)
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun getStaticFieldObject(className: String, filedName: String): Any? {
        try {
            val objClass = Class.forName(className)
            val field = objClass.getDeclaredField(filedName)
            field.isAccessible = true
            return field[null]
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }
}