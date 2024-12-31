package com.kit.localize.helper

/**
 * strings中的一条数据：包含name和content
 * **/
data class StringItem(val name: String, var text: String)

/**
 * 本地化文件:语言编码（如zh-rCN，ja...）、编码下的内容列表(如一条内容为string name="app_name">LocalizeKit</string> )
 * **/
data class Locale(val localeCode: String, val stringItems: List<StringItem>)
