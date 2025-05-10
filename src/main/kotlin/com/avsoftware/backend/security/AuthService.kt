package com.avsoftware.backend.security

import com.avsoftware.backend.db.model.RefreshToken
import com.avsoftware.backend.db.model.User
import com.avsoftware.backend.db.repository.RefreshTokenRepository
import com.avsoftware.backend.db.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    fun register(email: String, password: String): User {
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }

    @Transactional // execute within a single MongoDB transaction
    fun refresh(refreshToken: String): TokenPair {

        // is it a valid refresh token?
        if (!jwtService.validateRefreshToken(refreshToken)){
            throw BadCredentialsException("Invalid Refresh Token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            BadCredentialsException("Invalid Refresh Token.")
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(
            userId = user.id,
            hashedToken = hashed
        ) ?: throw IllegalArgumentException("Refresh token not recognised, (maybe used or expired)")

        // rotate the token

        // delete existing
        refreshTokenRepository.deleteByUserIdAndHashedToken(
            userId = user.id,
            hashedToken = hashed
        )

        // create new tokens
        val newAccessToken = jwtService.generateAccessToken(userId = userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId = userId)

        // store new refresh token
        storeRefreshToken(
            userId = user.id,
            rawRefreshToken = newRefreshToken
        )

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    fun login(email: String, password: String): TokenPair {
        // does the user exist?
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid Credentials.")

        // does the hashed password match
        if (!hashEncoder.matches(password, user.hashedPassword)){
            throw BadCredentialsException("Invalid Credentials.")
        }

        // User is Authenticated
        val newAccessToken = jwtService.generateAccessToken(userId = user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(userId = user.id.toHexString())

        // store refresh token in DB
        storeRefreshToken(userId = user.id, rawRefreshToken = newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String){
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)
        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                hashedToken = hashed,
                expiresAt = expiresAt,
                createdAt = Instant.now()
            )
        )
    }

    // can use simplified hash here as token is random and doesn't require salting to hash
    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}