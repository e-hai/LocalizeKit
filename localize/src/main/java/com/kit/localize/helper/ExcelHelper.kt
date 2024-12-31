package com.kit.localize.helper

import com.kit.localize.logger.Log
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 使用的语言编码标准是BCP 47 (RFC 5646)
 */
object ExcelHelper {

    private val TAG = ExcelHelper.javaClass.simpleName

    /**
     * 从Excel文件中读取多语言数据，返回一个包含多个Locale对象的列表。
     * 每个Locale对象包含一种语言的所有字符串键值对。
     *
     * @param excelFile Excel文件路径
     * @return 包含多个Locale对象的列表
     */
    fun getLocalesFromExcelSheet(excelFile: File): List<Locale> {
        val inputStream = FileInputStream(excelFile)
        val excelWBook = XSSFWorkbook(inputStream)
        val localeCodes = mutableListOf<String>()
        val stringKeys = mutableListOf<String>()
        val localeList = mutableListOf<Locale>()
        // 读取excel文档中的第一个工作表
        excelWBook.first().let {
            // 获取工作簿
            val excelWSheet = excelWBook.getSheet(it.sheetName)
            // 整个表的总行数
            val rowCount = excelWSheet.lastRowNum - excelWSheet.firstRowNum + 1
            // 第一行是多语言的代号，第一列是key，所以第一行的第一列的item可以为空或表示该列的含义的描述
            val firstRow = excelWSheet.getRow(0)
            // 总列数通过有多少种语言来决定
            val colCount = firstRow.lastCellNum - firstRow.firstCellNum

            // 遍历第一行所有的列：获取所有语言代号
            for (col in 0 until colCount) {
                val localeCode = getCellData(excelWBook, excelWSheet.sheetName, 0, col)
                localeCodes.add(localeCode)
            }

            // 遍历第一列所有的行：获取所有<string>的key
            for (row in 0 until rowCount) {
                val stringKey = getCellData(excelWBook, excelWSheet.sheetName, row, 0)
                stringKeys.add(stringKey)
            }

            // 遍历翻译的数据
            for (col in 1 until colCount) {
                val localeCode = localeCodes[col]
                val stringItems = mutableListOf<StringItem>()
                for (row in 1 until rowCount) {
                    val key = stringKeys[row]
                    val value = getCellData(excelWBook, excelWSheet.sheetName, row, col) // 获取文本
                    val finalValue = WordHelper.escapeText(value) // 对文本进行转义，防止在界面中显示异常
                    stringItems.add(StringItem(key, finalValue))
                }
                localeList.add(Locale(localeCode, stringItems))
            }
        }
        excelWBook.close()
        inputStream.close()
        return localeList
    }

    /**
     * 读取指定单元格的数据
     *
     * @param excelWBook Excel工作簿对象
     * @param sheetName 工作表名称
     * @param rowNum 行号
     * @param colNum 列号
     * @return 单元格内容字符串
     */
    private fun getCellData(
        excelWBook: XSSFWorkbook,
        sheetName: String,
        rowNum: Int,
        colNum: Int
    ): String {
        val excelWSheet = excelWBook.getSheet(sheetName)
        try {
            val cell = excelWSheet.getRow(rowNum).getCell(colNum)
            cell.run {
                val cellData: String = when (cellType) {
                    CellType.STRING -> {
                        stringCellValue
                    }

                    CellType.BLANK -> {
                        ""
                    }

                    CellType.FORMULA -> {
                        "${cellFormula.toString()}"
                    }

                    else -> {
                        throw RuntimeException("不支持的数据类型")
                    }
                }
                Log.d(
                    TAG,
                    "工作表[$sheetName]的第$rowNum 行第$colNum 列的值为：$cellData"
                )
                return cellData
            }
        } catch (e: Exception) {
            Log.d(
                TAG,
                "工作表[$sheetName]的第$rowNum 行第$colNum 列的值时异常：${e.message}"
            )
        }
        return ""
    }

    /**
     * 写入指定单元格的数据
     *
     * @param filePath Excel文件路径
     * @param sheetName 工作表名称
     * @param rowNum 行号
     * @param colNum 列号
     * @param resultValue 要写入的值
     */
    fun setCellData(
        filePath: String,
        sheetName: String,
        rowNum: Int,
        colNum: Int,
        resultValue: String
    ) {
        val inputStream = FileInputStream(filePath)
        val excelWBook = XSSFWorkbook(inputStream)
        val excelWSheet = excelWBook.getSheet(sheetName)
        try {
            // 获取 excel文件中的行对象
            val row = getRow(excelWSheet, rowNum)
            // 如果单元格为空，则返回 Null
            val cell = getCell(row, colNum)

            cell.setCellValue(resultValue)
        } catch (e: Exception) {
            Log.d(TAG, "设置Excel数据时发生错误-->$e")
        }

        try {
            val fileOut = FileOutputStream(filePath)
            excelWBook.write(fileOut)
            fileOut.flush()
            fileOut.close()

            excelWBook.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
        }
    }

    /**
     * 将多语言文案导出到Excel文件中
     *
     * @param filePath Excel文件路径
     * @param sheetName 工作表名称
     * @param resData 多语言数据列表
     */
    fun writeLocalesToExcel(
        filePath: String,
        sheetName: String,
        resData: List<Locale>
    ) {

        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }

        val excelWBook = XSSFWorkbook()
        val excelWSheet = excelWBook.createSheet(sheetName)

        // 把所有多语言文件中的strings的name放到一起，达到最大化的覆盖
        val stringNames = mutableSetOf<String>()
        resData.forEach { locale ->
            locale.stringItems.forEach {
                stringNames.add(it.name)
            }
        }

        val headRow = getRow(excelWSheet, 0)
        // 第一行第一列仅为描述，无实际意义
        val zeroCell = getCell(headRow, 0)
        zeroCell.setCellValue("strings-name")
        // 第一行第二列开始写入多语言的编码
        resData.forEachIndexed { index, locale ->
            val colNum = index + 1
            val localCell = getCell(headRow, colNum)
            localCell.setCellValue(locale.localeCode)
        }

        stringNames.forEachIndexed { nameIndex, name ->
            val rowNum = nameIndex + 1
            val stringRow = getRow(excelWSheet, rowNum)

            // 第0列写入多语言的name
            val nameCell = getCell(stringRow, 0)
            nameCell.setCellValue(name)

            // 第1列开始写入多语言的text
            resData.forEachIndexed { localeIndex, locale ->
                val colNum = localeIndex + 1
                val textCell = getCell(stringRow, colNum)
                val text = locale.stringItems.find { it.name == name }?.text ?: ""
                textCell.setCellValue(text)
            }
        }

        // 把数据保存到文件中
        try {
            val fileOut = FileOutputStream(filePath)
            excelWBook.write(fileOut)
            fileOut.flush()
            fileOut.close()
            excelWBook.close()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * 获取或创建指定行的对象
     *
     * @param excelWSheet 工作表对象
     * @param rowNum 行号
     * @return 行对象
     */
    private fun getRow(excelWSheet: XSSFSheet, rowNum: Int): XSSFRow {
        val row = if (excelWSheet.getRow(rowNum) != null) {
            excelWSheet.getRow(rowNum)
        } else {
            excelWSheet.createRow(rowNum)
        }
        return row
    }

    /**
     * 获取或创建指定单元格的对象
     *
     * @param row 行对象
     * @param colNum 列号
     * @return 单元格对象
     */
    private fun getCell(row: XSSFRow, colNum: Int): XSSFCell {
        return row.getCell(colNum, MissingCellPolicy.RETURN_BLANK_AS_NULL) ?: row.createCell(colNum)
    }
}