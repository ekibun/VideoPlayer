package soko.ekibun.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object JsonUtil{
    val GSON = Gson()

    fun toJson(src: Any): String {
        return GSON.toJson(src)
    }

    inline fun <reified T> toEntity(json: String): T? {
        return toEntity(json, object : TypeToken<T>() {}.type)
    }

    fun <T> toEntity(json: String, type: Type): T? {
        return try{
            GSON.fromJson(json, type)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    fun <T> toEntity(json: String, clazz: Class<T>): T? {
        return try{
            GSON.fromJson(json, clazz)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }
}