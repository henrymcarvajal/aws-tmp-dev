package com.mps.payment.core.client.s3

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.annotation.PostConstruct


@Service
class AmazonClient {

    private var s3Client: S3Client? = null

    @Value("\${aws.s3.bucket.url}")
    private val bucketUrl: String? = null

    @Value("\${aws.s3.folder}")
    private val folder: String? = null

    @Value("\${aws.s3.bucket.name}")
    private val bucketName: String? = null

    @Value("\${aws.access.key.id}")
    private val accessKey: String? = null

    @Value("\${aws.secret.access.key}")
    private val secretKey: String? = null

    @PostConstruct
    private fun initializeAmazonS3Client() {
        val awsCredentials = AwsBasicCredentials.create(accessKey, secretKey)
        s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build()
    }

    fun uploadFile(file: File, merchantId: String): String? {
        var fileUrl = ""
        try {
            val fileName = file.name
            val path = "${folder}${merchantId}/${fileName}"
            fileUrl = "${bucketUrl}${path}"
            uploadFileTos3bucket(path, file)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fileUrl
    }

    @Throws(IOException::class)
    fun convertByteToFile(file: MultipartFile,bytes:ByteArray): File {
        val convFile = File(file.originalFilename)
        val fos = FileOutputStream(convFile)
        fos.write(bytes)
        fos.close()
        return convFile
    }

    private fun generateFileName(multiPart: MultipartFile): String {
        return Date().getTime().toString() + "-" + multiPart.originalFilename!!.replace(" ", "_")
    }

    private fun uploadFileTos3bucket(key: String, file: File) {
        try {
            s3Client!!.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key)
                    .build(), RequestBody.fromFile(file)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteFileFromS3Bucket(key: String): String {
        s3Client!!.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build())
        return "Successfully deleted"
    }
}