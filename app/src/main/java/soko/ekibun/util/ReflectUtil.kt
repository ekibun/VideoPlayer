package soko.ekibun.util

import java.lang.reflect.Field
import java.util.ArrayList

object ReflectUtil {
    fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = ArrayList<Field>()
        var type = clazz
        do{
            fields.addAll(type.declaredFields)
            type = type.superclass?:break
        } while(true)
        return fields
    }
}