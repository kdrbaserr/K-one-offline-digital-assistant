package com.kone.assistant.stt

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class VoskModelSpec(
    val id: String = "vosk-model-small-tr-0.3",
    val version: String = "0.3",
    val assetPath: String = "models/vosk-model-small-tr-0.3.zip",
    val archiveBytes: Long = 36_855_784,
    val sha256: String = "8c8d07cec1bce31add14967c16891c84152cdf76d391e33c08278119b9ea96e5",
    val license: String = "Apache-2.0",
)

class VoskModelAssetInstaller(
    private val context: Context,
    private val spec: VoskModelSpec = VoskModelSpec(),
) {
    fun install(): File {
        val modelsDirectory = File(context.filesDir, "models").apply { mkdirs() }
        val destination = File(modelsDirectory, spec.id)
        val marker = File(destination, ".asset-sha256")
        if (destination.isDirectory && marker.readTextOrNull() == spec.sha256) return destination

        val archive = File(context.cacheDir, "${spec.id}.zip.part")
        val staging = File(modelsDirectory, "${spec.id}.staging")
        archive.delete()
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            copyAndVerifyAsset(archive)
            unzipSafely(archive, staging)
            val extractedModel = File(staging, spec.id).takeIf { it.isDirectory } ?: staging
            File(extractedModel, ".asset-sha256").writeText(spec.sha256)
            destination.deleteRecursively()
            if (extractedModel == staging) {
                check(staging.renameTo(destination)) { "Model staging klasörü taşınamadı" }
            } else {
                check(extractedModel.renameTo(destination)) { "Çıkarılan model taşınamadı" }
                staging.deleteRecursively()
            }
            return destination
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        } finally {
            archive.delete()
        }
    }

    private fun copyAndVerifyAsset(output: File) {
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        context.assets.open(spec.assetPath).use { input ->
            FileOutputStream(output).use { stream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    stream.write(buffer, 0, count)
                    digest.update(buffer, 0, count)
                    bytes += count
                }
                stream.fd.sync()
            }
        }
        check(bytes == spec.archiveBytes) { "Model boyutu geçersiz: $bytes" }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual == spec.sha256) { "Model checksum geçersiz: $actual" }
    }

    private fun unzipSafely(archive: File, destination: File) {
        val root = destination.canonicalFile
        ZipInputStream(FileInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val output = File(destination, entry.name).canonicalFile
                check(output.path.startsWith(root.path + File.separator)) { "Güvensiz ZIP yolu: ${entry.name}" }
                if (entry.isDirectory) output.mkdirs() else {
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun File.readTextOrNull(): String? = runCatching { readText().trim() }.getOrNull()
}
