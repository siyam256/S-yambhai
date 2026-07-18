package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mcq_builder_prefs", Context.MODE_PRIVATE)

    var brandName: String
        get() = prefs.getString("brand_name", "") ?: ""
        set(value) = prefs.edit().putString("brand_name", value).apply()

    var startNum: Int
        get() = prefs.getInt("start_num", 1)
        set(value) = prefs.edit().putInt("start_num", value).apply()

    var fontSize: Int
        get() = prefs.getInt("font_size", 14)
        set(value) = prefs.edit().putInt("font_size", value).apply()

    var watermarkPath: String?
        get() = prefs.getString("watermark_path", null)
        set(value) = prefs.edit().putString("watermark_path", value).apply()

    var isHeaderFooterEnabled: Boolean
        get() = prefs.getBoolean("hf_enabled", true)
        set(value) = prefs.edit().putBoolean("hf_enabled", value).apply()

    var headerLeft: String
        get() = prefs.getString("header_left", "ঢাবি 'ক' ভর্তি পরীক্ষা ২০২৫-২৬") ?: "ঢাবি 'ক' ভর্তি পরীক্ষা ২০২৫-২৬"
        set(value) = prefs.edit().putString("header_left", value).apply()

    var headerRight: String
        get() = prefs.getString("header_right", "প্রশ্ন-সমাধান") ?: "প্রশ্ন-সমাধান"
        set(value) = prefs.edit().putString("header_right", value).apply()

    var footerLeft: String
        get() = prefs.getString("footer_left", "উদ্ভাস একাডেমিক এন্ড এডমিশন কেয়ার") ?: "উদ্ভাস একাডেমিক এন্ড এডমিশন কেয়ার"
        set(value) = prefs.edit().putString("footer_left", value).apply()

    var footerRight: String
        get() = prefs.getString("footer_right", "পরিবর্তনের প্রত্যয়ে নিরন্তর পথচলা...") ?: "পরিবর্তনের প্রত্যয়ে নিরন্তর পথচলা..."
        set(value) = prefs.edit().putString("footer_right", value).apply()
}
