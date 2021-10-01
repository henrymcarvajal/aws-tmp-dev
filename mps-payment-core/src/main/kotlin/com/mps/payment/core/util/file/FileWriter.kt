package com.mps.payment.core.util.file

import java.io.FileWriter
import java.io.IOException

object FileWriter {

    fun writeCsvFile(csvHeader: String, fileName: String, lines: List<String>) {
        var fileWriter: FileWriter? = null

        try {
            fileWriter = FileWriter("${fileName}.csv")
            fileWriter.append(csvHeader)
            fileWriter.append('\n')
            for (line in lines) {
                fileWriter.append(line)
            }
        } catch (e: Exception) {

        } finally {
            try {
                fileWriter!!.flush()
                fileWriter.close()
            } catch (e: IOException) {
                println("Flushing/closing error!")
                e.printStackTrace()
            }
        }
    }
}