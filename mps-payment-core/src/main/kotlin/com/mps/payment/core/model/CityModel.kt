package com.mps.payment.core.model

//import com.mps.payment.core.client.entregalo.payload.CityDTO
import com.mps.payment.core.client.entregalo.payload.CityDTO
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class City(
        @Column(name = "last_updated") val lastUpdatedDate: LocalDateTime,
        @Column(name="dpto") var dpto:String,
        @Column(name="mpio") var mpio:String,
        @Id @Column(name="dane_code") var daneCode:String,
        @Column(name="against_delivery") var againstDelivery:String,
)

fun CityDTO.toEntity() = City(
        dpto= this.state, daneCode=this.code, mpio = city, lastUpdatedDate = LocalDateTime.now(),
        againstDelivery = this.againstDelivery
)

fun City.toDTO()= CityDTO(
        state=this.dpto, city = this.mpio, code = this.daneCode,cityExtended = null, againstDelivery = this.againstDelivery
)