package com.mps.payment.web.config

import com.mps.payment.core.security.jwt.JwtSecurityConfigurer
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.web.WebResponseWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy


@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfiguration(
    private val jwtTokenProvider: JwtTokenProvider,
    private val responseWriter: WebResponseWriter
) : WebSecurityConfigurerAdapter() {

    @Bean
    @Throws(Exception::class)
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }


    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        println("Configuring security...")
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/customer/order/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/dropshippingsale/public/view/checkout/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/label/public/multiple/order/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/label/public/order/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/logistic/cities").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/merchant/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.PUT, "/order/public/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/product/public/**").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.GET, "/payment/public/**").permitAll() //TODO evaluate payment/merchant security

        http.authorizeRequests().antMatchers(HttpMethod.POST, "/checkout/order").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.POST, "/dispersion").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.POST, "/payment/agree").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.POST, "/product/payment").permitAll()
        http.authorizeRequests().antMatchers(HttpMethod.POST, "/user/recovery-password").permitAll()


        http.csrf().disable().authorizeRequests().antMatchers(HttpMethod.PATCH, "/payment/public/updateState").permitAll()
            .antMatchers(HttpMethod.OPTIONS, "/payment/public/updateState").permitAll()
        http
            .httpBasic().disable()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/user/confirm/**").permitAll()
            .antMatchers("/auth/signin").permitAll()
            .antMatchers(HttpMethod.PATCH, "/payment/delayClose").permitAll()
            .antMatchers(HttpMethod.POST, "/merchant").permitAll()
            .antMatchers(HttpMethod.POST, "/merchant/landing").permitAll()
            .antMatchers(HttpMethod.POST, "/user/password").permitAll()
            .antMatchers(HttpMethod.POST, "/user/recovery-password").permitAll()
            .antMatchers(HttpMethod.OPTIONS, "**").permitAll()
            .anyRequest().authenticated()
            .and()
            .apply(JwtSecurityConfigurer(jwtTokenProvider, responseWriter))
        println("Security configured")
    }

    @Throws(Exception::class)
    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(
            "/v2/api-docs",
            "/configuration/ui",
            "/swagger-resources/**",
            "/configuration/security",
            "/swagger-ui.html",
            "/payment/public/updateState",
            "/webjars/**"
        )
            .antMatchers(HttpMethod.OPTIONS, "/**")
    }
}