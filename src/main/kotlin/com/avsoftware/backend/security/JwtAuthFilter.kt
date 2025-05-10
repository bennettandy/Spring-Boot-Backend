package com.avsoftware.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    // injected JwtService
    private val jwtService: JwtService
): OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // get Authorization header from the request
        // Bearer <token>
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")){
            if (jwtService.validateAccessToken(authHeader)){
                val userId = jwtService.getUserIdFromToken(authHeader)

                // Create an Authentication Object with the user id
                val auth = UsernamePasswordAuthenticationToken( userId, null)
                // set to global security context
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        // complete rest of filter chain
        filterChain.doFilter(request, response)
    }
}