package com.mps.common.util.img

import com.mps.common.dto.GenericResponse
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.ImageWriteParam


@Throws(IOException::class)
fun resizeImage(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
    val resizedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
    val graphics2D = resizedImage.createGraphics()
    graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
    graphics2D.dispose()
    return resizedImage
}

fun calculateNewHeight(originalImage: BufferedImage, targetWidth: Int): Int {
    val actualWith = originalImage.width.toFloat()
    val actualHeight = originalImage.height.toFloat()
    val relation = actualHeight / actualWith
    return targetWidth.times(relation).toInt()
}

@Throws(IOException::class)
fun convertMultiPartToFile(file: MultipartFile): File {
    val convFile = File(file.originalFilename)
    val fos = FileOutputStream(convFile)
    fos.write(file.bytes)
    fos.close()
    return convFile
}

fun compressImage(bufferedImage: BufferedImage, multipartFile: MultipartFile, imageReaders: ImageReader,
                  targetWith: Int, fileName:String=""): File {
    val newHeight = calculateNewHeight(bufferedImage, targetWith)
    val newImage = resizeImage(bufferedImage, targetWith, newHeight)
    val writer = ImageIO.getImageWriter(imageReaders)
    val compressedFile = if(fileName.isBlank()){
        File(multipartFile.originalFilename)
    }else{
        val format = imageReaders.formatName
        File("$fileName.$format")
    }
    val out = FileOutputStream(compressedFile)
    val ios = ImageIO.createImageOutputStream(out)
    writer.output = ios
    val param: ImageWriteParam = writer.defaultWriteParam
    if (param.canWriteCompressed()) {
        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
        param.compressionQuality = 0.199f
    }
    writer.write(null, IIOImage(newImage, null, null), param)
    out.close()
    ios.close()
    writer.dispose()
    return compressedFile
}