package com.nowayhome.housecheck.storage

import com.nowayhome.housecheck.domain.DocumentType
import com.nowayhome.housecheck.security.HouseCheckCryptoService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

data class StoredDocument(
    val storageKey: String,
)

interface HouseCheckDocumentStorage {
    fun store(houseCheckId: UUID, documentType: DocumentType, file: MultipartFile): StoredDocument

    fun delete(storageKey: String)
}

@Component
class FileSystemHouseCheckDocumentStorage(
    @Value("\${housecheck.storage.root:\${java.io.tmpdir}/no-way-home/housecheck}") rootPath: String,
    private val houseCheckCryptoService: HouseCheckCryptoService,
) : HouseCheckDocumentStorage {
    private val root: Path = Paths.get(rootPath).toAbsolutePath().normalize().also { Files.createDirectories(it) }

    override fun store(houseCheckId: UUID, documentType: DocumentType, file: MultipartFile): StoredDocument {
        val storageKey = "${houseCheckId}/${documentType.name.lowercase()}-${UUID.randomUUID()}.bin"
        val targetPath = root.resolve(storageKey).normalize()
        Files.createDirectories(targetPath.parent)
        Files.write(targetPath, houseCheckCryptoService.encryptBytes(file.bytes))
        return StoredDocument(storageKey = storageKey)
    }

    override fun delete(storageKey: String) {
        Files.deleteIfExists(root.resolve(storageKey).normalize())
    }
}
