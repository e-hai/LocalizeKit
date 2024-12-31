package com.kit.localize

import com.kit.localize.helper.ExcelHelper
import com.kit.localize.helper.WordHelper
import com.kit.localize.logger.Log
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File


fun main() {
    // 查找当前项目的模块
    val projectRoot = File(EXPORT_PROJECT_PATH)
    val moduleDirList = projectRoot.listFiles { file ->
        if (file.name.startsWith(".")) {
            println("过滤隐藏文件")
            return@listFiles false
        }
        if (!file.isDirectory) {
            println("过滤文件")
            return@listFiles false
        }
        val stringsFile = File(file.absolutePath, PATH_STRINGS)
        return@listFiles stringsFile.exists()
    }

    Log.i("ExportStrings2ExcelScript", moduleDirList?.map { it.name }.toString())
    moduleDirList?.forEachIndexed { _, dir ->
        val resDir = File(dir, PATH_RES)
        // 解析当前项目的多语言内容 <语言目录（如values-zh-rCN），<name，word>>
        val localesFromRes = WordHelper.getLocalesFromRes(resDir, FILE_NAME_STRINGS)
        if (localesFromRes.isEmpty()) {
            println("res中 strings数据为空")
            return@forEachIndexed
        }

        ExcelHelper.writeLocalesToExcel(OUTPUT_EXCEL_FILE, dir.name, localesFromRes)
    }

}