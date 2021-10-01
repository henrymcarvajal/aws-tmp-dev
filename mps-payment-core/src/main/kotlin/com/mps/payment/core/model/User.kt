package com.mps.payment.core.model

import com.mps.payment.core.model.interfaces.AuditableEntity
import com.mps.payment.core.model.interfaces.DisableableEntity

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

@Entity
@Table(name = "mps_user")
data class User(
        @Id @GeneratedValue var id: UUID?,
        var name: String,
        private var username: String,
        private var password: String?,
        var roles: String,
        @Column(name = "account_non_locked") var accountNonLocked: Boolean=true,
        @Column(name = "failed_attempt") var failedAttempts: Int = 0,
        @Column(name = "lock_time") var lockTime: LocalDateTime?= null
) : UserDetails, DisableableEntity, AuditableEntity {

    //var phone: String? = null

    /*var acceptedTermsAndConditionsAt: Date? = null
    var termsAndConditionsVersion: String? = null*/
    var confirmed = false

    override var disabled = false
    @Column(name = "created_at")
    override var createdAt: LocalDateTime = LocalDateTime.now()
    @Column(name = "last_updated")
    override var updatedAt: LocalDateTime? = null

    override fun getAuthorities(): Collection<GrantedAuthority?> {
        return roles.split(",").stream().map { role: String? -> SimpleGrantedAuthority(role) }.collect(Collectors.toList())
    }

    override fun getPassword(): String? {
        return password
    }

    override fun isAccountNonLocked(): Boolean {
        return this.accountNonLocked
    }

    fun updatePassword(password: String) {
        this.password = password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return !disabled
    }
}

data class UserDto(
        var id: UUID?,
        var name: String,
        var username: String,
        var password: String?,
        var roles: String
)
const val TOKEN_NULL ="id no puede estar vacio"
data class PasswordDTO(
        @get:NotBlank(message = TOKEN_NULL) var id: String,
        @get:Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_FORMAT) @get:NotBlank(message = PASSWORD_NOT_NULL) var password:String
)

fun UserDto.toEntity(): User {
    return User(this.id, this.name, this.username, this.password, this.roles)
}

fun User.toDto(): UserDto {
    return UserDto(this.id, this.name, this.username, "", this.roles)
}