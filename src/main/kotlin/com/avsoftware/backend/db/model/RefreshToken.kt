package com.avsoftware.backend.db.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("refresh_tokens")
data class RefreshToken(
    val userId: ObjectId,
    @Indexed(expireAfter = "0s") // MongoDB automatically cleans up expired tokens
    val expiresAt: Instant,
    val hashedToken: String, // Secure
    val createdAt: Instant = Instant.now()
)
