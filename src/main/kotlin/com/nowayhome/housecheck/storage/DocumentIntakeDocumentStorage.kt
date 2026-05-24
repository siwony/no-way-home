package com.nowayhome.housecheck.storage

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.security.HouseCheckCryptoService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

data class StoredIntakeDocument(
    val storageKey: String,
)

interface DocumentIntakeDocumentStorage {
    fun store(sessionId: UUID, documentType: DocumentIntakeDocumentType, fileName: String, bytes: ByteArray): StoredIntakeDocument

    fun delete(storageKey: String)
}

@Component
class FileSystemDocumentIntakeDocumentStorage(
    @Value("\${housecheck.storage.root:\${java.io.tmpdir}/no-way-home/housecheck}") rootPath: String,
    private val houseCheckCryptoService: HouseCheckCryptoService,
) : DocumentIntakeDocumentStorage {
    private val root: Path = Paths.get(rootPath).toAbsolutePath().normalize().also { Files.createDirectories(it) }

    override fun store(
        sessionId: UUID,
        documentType: DocumentIntakeDocumentType,
        fileName: String,
        bytes: ByteArray,
    ): StoredIntakeDocument {
        val extension = fileName.substringAfterLast('.', "bin").lowercase()
        val storageKey = "document-intakes/${sessionId}/${documentType.pathValue}-${UUID.randomUUID()}.$extension.bin"
        val targetPath = root.resolve(storageKey).normalize()
        Files.createDirectories(targetPath.parent)
        Files.write(targetPath, houseCheckCryptoService.encryptBytes(bytes))
        return StoredIntakeDocument(storageKey = storageKey)
    }

    override fun delete(storageKey: String) {
        Files.deleteIfExists(root.resolve(storageKey).normalize())
    }
}
