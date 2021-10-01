package com.mps.payment.core.security.jwt

import com.mps.payment.core.web.WebResponse
import com.mps.payment.core.web.WebResponseWriter
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class JwtTokenAuthenticationFilter(
        private val jwtTokenProvider: JwtTokenProvider,
        private val responseWriter: WebResponseWriter
) : GenericFilterBean() {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest, res: ServletResponse, filterChain: FilterChain) {
        try {
            val token = jwtTokenProvider.resolveToken((req as HttpServletRequest))
            if (token != null && jwtTokenProvider.validateToken(token)) {
                val auth = jwtTokenProvider.getAuthentication(token)
                if (auth != null) {
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
            filterChain.doFilter(req, res)
        } catch (ex: InvalidJwtAuthenticationException) {
            val webResponse = WebResponse("Invalid security credentials", "Invalid token",
                    ex.message!!, HttpStatus.FORBIDDEN.value());
            responseWriter.writeResponse(res, webResponse);
        }
    }

}