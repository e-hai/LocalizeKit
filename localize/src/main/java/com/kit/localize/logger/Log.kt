package com.kit.localize.logger

import java.text.SimpleDateFormat
import java.util.*

object Log {
    init {
        // 设置标准输出流的编码为 UTF-8
        System.setProperty("file.encoding", "UTF-8")
    }

    private val colorful = ColorfulString()
    private val date = Date()

    private fun Date.getNowTimeDetail(): String {
        val sdf = SimpleDateFormat("HH:mm:ss SSS", Locale.US)
        return sdf.format(this).plus("(${Thread.currentThread().id})")
    }

    fun d(tag: String = "", content: String) {
        println("${date.getNowTimeDetail()} D/$tag: $content")
    }

    fun i(tag: String = "", content: String) {
        println(colorful.renderUltramarine("${date.getNowTimeDetail()} I/$tag: $content"))
    }

    fun w(tag: String = "", content: String) {
        println(colorful.renderYellow("${date.getNowTimeDetail()} W/$tag: $content"))
    }

    fun e(tag: String = "", content: String) {
        println(colorful.renderRed("${date.getNowTimeDetail()} E/$tag: $content"))
    }
}