package com.mps.payment.pull.col.config

import com.mps.payment.pull.col.model.PaymentPartner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import javax.sql.DataSource
import org.springframework.boot.jdbc.DataSourceBuilder

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder

import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import java.util.HashMap

import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter











/**
 * @author jorge Espinosa
 */
@Configuration
@EnableResourceServer
class ResourceServerConfig : ResourceServerConfigurerAdapter() {

    @Autowired
    private lateinit var env: Environment

    override fun configure(security: ResourceServerSecurityConfigurer) {
        security.resourceId(RESOURCE_ID)
    }
    companion object {
        private const val RESOURCE_ID = "woo-resource"
    }

    override fun configure(http: HttpSecurity) {
        http
                .antMatcher("/woo/**")
                .authorizeRequests()
                .antMatchers("/woo/**").access("#oauth2.hasScope('woo.read')")
    }

    /**@Bean
    fun dataSource(): DataSource? {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.url = env.getProperty("jdbc.url")
        dataSource.username = env.getProperty("jdbc.user")
        dataSource.password = env.getProperty("jdbc.pass")
        return dataSource
    }**/

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    fun primaryDataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.url = env.getProperty("pull.datasource.url")
        dataSource.username = env.getProperty("pull.datasource.username")
        dataSource.password = env.getProperty("pull.datasource.password")
        return dataSource
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.coresource")
    fun secondaryDataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.url = env.getProperty("core.datasource.url")
        dataSource.username = env.getProperty("core.datasource.username")
        dataSource.password = env.getProperty("core.datasource.password")
        return dataSource
    }

    @Bean(name=["entityManagerFactory"])
    fun pullEntityManager(): LocalContainerEntityManagerFactoryBean? {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = primaryDataSource()
        em.setPackagesToScan(
                *arrayOf("com.mps.payment.pull.col.model"))
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        val properties = HashMap<String, Any?>()
        properties["hibernate.hbm2ddl.auto"] = env.getProperty("hibernate.hbm2ddl.auto")
        properties["hibernate.dialect"] = env.getProperty("hibernate.dialect")
        em.setJpaPropertyMap(properties)
        return em
    }
}



