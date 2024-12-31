package com.kit.localize

import com.kit.localize.helper.ExcelHelper
import com.kit.localize.helper.WordHelper
import com.kit.localize.logger.Log
import java.io.File


fun main() {
    // 从excel表中读取多语言数据
    val excelFile = File(INPUT_EXCEL_FILE)
    val localesFromExcel = ExcelHelper.getLocalesFromExcelSheet(excelFile)
    if (localesFromExcel.isEmpty()) {
        println("Excel 数据为空")
        return
    }

    //遍历所有文件夹，根据 strings 文件来判断是否属于需要输出多语言的Module模块，一般来说只有app模块才需要多语言
    val projectRoot = File(IMPORT_PROJECT_PATH)
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
    moduleDirList?.forEach {dir->
        val resDir = File(dir, PATH_RES)
        // 项目中读出的string map，<语言目录（如values-zh-rCN），<name，word>>
        // 收集的同时进行合并
        val localesFromRes = WordHelper.getLocalesFromRes(resDir,FILE_NAME_STRINGS)
        val finalLocales = WordHelper.mergeLocaleData(localesFromExcel, localesFromRes)
        WordHelper.writeLocalesToProject(finalLocales, resDir,FILE_NAME_STRINGS)
    }
}