package com.mcal.common.data

import android.annotation.TargetApi
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceScheme {

   object Main {
      @TargetApi(Build.VERSION_CODES.S)
      val UI_MONET = booleanPreferencesKey("ui_monet")
      val UI_THEME = booleanPreferencesKey("night_mode")
      val UI_LANGUAGE = stringPreferencesKey("Language")
      val NET_SERVER = stringPreferencesKey("domain_hk")
      val OTHER_GARBAGE_LMT = intPreferencesKey("garbage_limit")
      val OTHER_RENAME_KEY = booleanPreferencesKey("FileRenameOption")
   }

   object Compiler {
      val DECODE_MULTIRES = booleanPreferencesKey("fixMultiRes")
      val BUILD_AAPT_RULES = booleanPreferencesKey("ae_aapt_rules")
      val BUILD_USE_AAPT2 = booleanPreferencesKey("aapt2")
      val BUILD_JSON_CONFIG = booleanPreferencesKey("apktool_use_json")
      val SIGNING_ENABLED = booleanPreferencesKey("signing_enabled")
      val SIGNING_CUSTOM_ON = booleanPreferencesKey("signing_on")
      val SIGNING_PASS = stringPreferencesKey("signing_pass")
      val SIGNING_KEY_ALIAS = stringPreferencesKey("signing_key_alias")
      val SIGNING_KEY_PASS = stringPreferencesKey("signing_key_password")
      val SIGNING_VERSION = intPreferencesKey("signing_version")
      val FRAMEWORKS_INSTALLED = booleanPreferencesKey("frameworks_installed")
      val DECODE_ASSETS = booleanPreferencesKey("decode_assets")
      val DECODE_RESOURCES = booleanPreferencesKey("decode_resources")
      val DECODE_CLASSES = booleanPreferencesKey("decode_classes")
      val CHECK_EXISTS_FILES = booleanPreferencesKey("apktool_check_exists_files")
   }

   object Editor {
      val FONT_SIZE = intPreferencesKey("editor_font_size")
      val WORDWRAP = booleanPreferencesKey("editor_wordwrap")
      val SHOW_LINE_NUMBERS = booleanPreferencesKey("editor_line_number")
      val PIN_LINE_NUMBER = booleanPreferencesKey("editor_pin_line_number")
      val SHOW_UNPRINTABLE = booleanPreferencesKey("editor_printable_characters")
      val MAGNIFIER = booleanPreferencesKey("editor_magnifier")
      val USE_ICU_LIB = booleanPreferencesKey("editor_use_icu_library")
      val IGNORE_CASE = booleanPreferencesKey("isIgnoreCase")
      val USE_REGEX = booleanPreferencesKey("isUseRegex")
   }

   object Misc {
      val HISTORY_RESOURCE = stringPreferencesKey("res_keywords")
      val HISTORY_FILES = stringPreferencesKey("mf_keywords")
      val HISTORY_MANIFEST = stringPreferencesKey("string_keywords")
      val WEB_LANGUAGE = stringPreferencesKey("webview_language")

   }

}