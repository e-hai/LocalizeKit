
package com.kit.localize.helper

import com.kit.localize.ATTRIBUTE_NAME_STRING
import com.kit.localize.ELEMENT_NAME_RESOURCES
import com.kit.localize.ELEMENT_NAME_STRING
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.*

object WordHelper {

    private const val TAG = "WordHelper"

    /**
     * 将Excel中读取的字符串数据合并到项目原本的字符串数据中。
     * 如果项目中不存在相同的字符串，则追加；如果存在，则覆盖。
     *
     * @param excelData 从Excel中读取的字符串数据，格式为<语言目录，<name，word>>
     * @param resData 从项目中读取的字符串数据，格式为<语言目录，<name，word>>
     * @return 合并后的字符串数据列表
     */
    fun mergeLocaleData(
        excelData: List<Locale>,
        resData: List<Locale>
    ): List<Locale> {
        // 创建一个映射来快速查找 resData 中的 Locale 对象
        val resLocaleMap = resData.associateBy { it.localeCode }

        val mergeData = mutableListOf<Locale>()

        // 遍历 excelData 中的每个 Locale 对象
        for (excelLocale in excelData) {
            // 查找 resData 中是否存在相同的多语言文件
            val resLocale = resLocaleMap[excelLocale.localeCode]

            if (resLocale != null) {
                // 如果存在，则合并 stringItems
                val mergedStringItems = resLocale.stringItems.toMutableList()

                for (excelStringItem in excelLocale.stringItems) {
                    // 查找是否存在相同的 name
                    val existingIndex =
                        mergedStringItems.indexOfFirst { it.name == excelStringItem.name }
                    if (existingIndex != -1) {
                        // 如果存在，则覆盖
                        mergedStringItems[existingIndex] = excelStringItem
                    } else {
                        // 如果不存在，则追加
                        mergedStringItems.add(excelStringItem)
                    }
                }

                // 创建一个新的 Locale 对象并添加到 mergeData
                mergeData.add(resLocale.copy(stringItems = mergedStringItems))
            } else {
                // 如果不存在，则追加整个 Locale 对象
                mergeData.add(excelLocale)
            }
        }

        return mergeData
    }

    /**
     * 解析项目中的多语言内容，返回一个包含多个Locale对象的列表。
     * 每个Locale对象包含一种语言的所有字符串键值对。
     *
     * @param resDir 包含多语言文件的目录
     * @param stringsFileName 字符串文件名（如 strings.xml）
     * @return 包含多个Locale对象的列表
     */
    fun getLocalesFromRes(
        resDir: File,
        stringsFileName: String
    ): List<Locale> {
        val saxReader = SAXReader() // XML解析器

        val localeList = mutableListOf<Locale>()
        // 遍历res文件夹下所有文件
        resDir.listFiles()
            ?.filter { dir ->
                // 找出包含strings.xml的文件夹
                val stringFile = File(dir, stringsFileName)
                stringFile.exists()
            }?.forEach { dir ->
                val stringItems = mutableListOf<StringItem>()
                val stringFile = File(dir, stringsFileName)
                val doc = saxReader.read(stringFile)
                val root = doc.rootElement
                // 解析<resources>节点
                if (root.name == ELEMENT_NAME_RESOURCES) {
                    val iterator = root.elementIterator()
                    while (iterator.hasNext()) {
                        val elementString = iterator.next()
                        // 解析<string>节点
                        if (elementString.name == ELEMENT_NAME_STRING) {
                            val name = elementString.attribute(ATTRIBUTE_NAME_STRING).text
                            val text = elementString.text
                            stringItems.add(StringItem(name, text))
                        }
                    }
                }
                localeList.add(Locale(dir.name, stringItems))
            }
        return localeList
    }

    /**
     * 将Excel中读取的字符串数据导入到项目中。
     *
     * @param localeList 从Excel中读取的字符串数据列表
     * @param resDir 包含多语言文件的目录
     * @param fileName 字符串文件名（如 strings.xml）
     */
    fun writeLocalesToProject(
        localeList: List<Locale>,
        resDir: File,
        fileName: String
    ) {
        localeList.forEach { locale ->
            val stringItems = locale.stringItems
            val stringFile = File(resDir, "${locale.localeCode}/${fileName}")

            if (stringFile.exists()) {
                // 修改原本的DOM
                val saxReader = SAXReader()
                val doc = saxReader.read(stringFile)
                val root = doc.rootElement
                val nodeMap = linkedMapOf<String, Element>()
                if (root.name == ELEMENT_NAME_RESOURCES) {
                    val iterator = root.elementIterator()
                    while (iterator.hasNext()) {
                        val elementString = iterator.next()
                        if (elementString.name == ELEMENT_NAME_STRING) {
                            val name = elementString.attribute(ATTRIBUTE_NAME_STRING).text
                            nodeMap[name] = elementString
                        }
                    }
                }
                // 原strings.xml中存在的name，则覆盖text，不存在则新增
                stringItems.forEach { (name, word) ->
                    val node = nodeMap[name]
                    if (node == null) {
                        root.addElement(ELEMENT_NAME_STRING)
                            .addAttribute(ATTRIBUTE_NAME_STRING, name)
                            .addText(word)
                    } else {
                        node.text = word
                    }
                }
                outputStringFile(doc, stringFile)
            } else {
                // 创建该多语言文件夹和文件
                val localeDir = File(resDir, locale.localeCode)
                localeDir.mkdirs()
                stringFile.createNewFile()
                val doc = DocumentHelper.createDocument()
                val root = doc.addElement(ELEMENT_NAME_RESOURCES)
                stringItems.forEach { (name, text) ->
                    root.addElement(ELEMENT_NAME_STRING)
                        .addAttribute(ATTRIBUTE_NAME_STRING, name)
                        .addText(text)
                }
                outputStringFile(doc, stringFile)
            }
        }
    }

    /**
     * 转义字符串中的单双引号，以确保在XML中正确显示。
     * 通过在单双引号前加反斜杠进行转义。
     *
     * @param text 需要转义的字符串
     * @return 转义后的字符串
     */
    fun escapeText(text: String): String {
        var last = '0'
        val builder = text.toCharArray().fold(StringBuilder()) { acc, char ->
            val piece = when (char) {
                '"' -> if (last != '\\') "\\\"" else char
                '\'' -> if (last != '\\') "\\\'" else char
                else -> char
            }
            acc.append(piece)
            last = char
            acc
        }
        return builder.toString()
    }

    /**
     * 输出字符串文件到指定位置。
     * 移除原本的换行符节点，以避免输出时多出换行符。
     *
     * @param doc XML文档对象
     * @param file 输出文件路径
     */
    private fun outputStringFile(doc: Document, file: File) {
        // 遍历所有节点，移除掉原本的换行符节点，否则输出时会因为newlines多出换行符
        val root = doc.rootElement
        if (root.name == ELEMENT_NAME_RESOURCES) {
            val iterator = root.nodeIterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element.nodeType == org.dom4j.Node.TEXT_NODE) {
                    if (element.text.isBlank()) {
                        iterator.remove()
                    }
                }
            }
        }
        // 输出
        val format = OutputFormat()
        format.encoding = "utf-8"
        format.setIndentSize(4)
        format.isNewLineAfterDeclaration = false
        format.isNewlines = true
        format.lineSeparator = System.getProperty("line.separator")
        file.outputStream().use { os ->
            val writer = XMLWriter(os, format)
            // 是否将字符转义
            writer.isEscapeText = false
            writer.write(doc)
            writer.flush()
            writer.close()
        }
    }
}