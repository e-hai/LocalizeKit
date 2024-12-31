package com.kit.localize


const val FILE_NAME_STRINGS = "strings.xml"
const val PATH_STRINGS = "src/main/res/values/${FILE_NAME_STRINGS}"
const val PATH_RES = "src/main/res"

// 导入导出时的excel路径
const val OUTPUT_EXCEL_FILE = "./localize/output.xlsx"
const val INPUT_EXCEL_FILE = "./localize/input.xlsx"

// 需要导入导出的项目目录
const val EXPORT_PROJECT_PATH = "./"
const val IMPORT_PROJECT_PATH = "./"

// 第一列存放string的name，作为第二表头
const val ELEMENT_NAME_RESOURCES = "resources"
const val ELEMENT_NAME_STRING = "string"
const val ATTRIBUTE_NAME_STRING = "name"