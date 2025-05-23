package com.avsoftware.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}")  val jwtSecret: String
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    private val accessTokenValidityMs = 15 * 60 * 1000L

    val refreshTokenValidityMs = 30 * 24 * 60 * 60 * 1000L

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date( now.time + expiry)
        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateAccessToken(userId: String): String =
        generateToken(
            userId = userId,
            type = "access",
            expiry = accessTokenValidityMs
        )

    fun generateRefreshToken(userId: String): String =
        generateToken(
            userId = userId,
            type = "refresh",
            expiry = refreshTokenValidityMs
        )

    fun validateAccessToken( token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType: String = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken( token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType: String = claims["type"] as? String ?: return false
        return tokenType == "refresh"
    }

    // Authorization: Bearer <token>
    fun getUserIdFromToken(token: String): String {

        val claims = parseAllClaims(token) ?: throw IllegalArgumentException("Invalid Token")

        return claims.subject // subject is the userId
    }

    private fun parseAllClaims( token: String): Claims? {

        val rawToken = if (token.startsWith("Bearer ")){
            token.removePrefix("Bearer ")
        } else token

        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build().parseSignedClaims(rawToken)
                .payload
        } catch (e: Exception){
            logger.debug("Failed to parse JWT", e)
            null
        }
    }
}