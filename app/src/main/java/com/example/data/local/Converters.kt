package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Option
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private val optionListType = Types.newParameterizedType(List::class.java, Option::class.java)
    private val optionListAdapter = moshi.adapter<List<Option>>(optionListType)
    
    private val intListType = Types.newParameterizedType(List::class.java, Integer::class.java)
    private val intListAdapter = moshi.adapter<List<Int>>(intListType)

    @TypeConverter
    fun fromOptionList(options: List<Option>?): String? {
        return options?.let { optionListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toOptionList(json: String?): List<Option>? {
        return json?.let { optionListAdapter.fromJson(it) }
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return list?.let { intListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? {
        return json?.let { intListAdapter.fromJson(it) }
    }
}
