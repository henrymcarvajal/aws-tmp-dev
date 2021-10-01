package com.mps.payment.core.enum

enum class PaymentStateEnum(val state:Int){
    INITIAL(1),
    AGREED(2),
    PAID(3),
    SHIPPED(4),
    DISPUTED(5),
    CLOSED(6),
    RECEIVED(7),
    TRANSFERRED(8)
}

enum class BankEnum(val state:String){
    BANCO_AV_VILLAS("1"),
    BANCO_BBVA_COLOMBIA("2"),
    BANCO_CAJA_SOCIAL("3"),
    BANCO_DAVIVIENDA("4"),
    BANCO_DE_BOGOTA("5"),
    BANCO_DE_OCCIDENTE("6"),
    BANCO_ITAU("7"),
    BANCOLOMBIA("8"),
    SCOTIABANK_COLPATRIA("9")
}

enum class AccountTypeEnum(val state:String){
    AHORROS("1"),
    CORRIENTE("2")
}

enum class OrderStatus(val state:Int){
    FAILED(1),
    TO_DISPATCH(2),
    PAYMENT_PENDING(4),
    DELIVERED(6),
    TRANSFERRED(5),
    ON_DELIVERY(3),
    RETURN(7),
    CANCELLED(8),
    NOTICE(9),
    TO_BE_CONFIRMED(10)
}

enum class PaymentMethod(val method:String){
    MPS("MPS"),
    COD("COD"),
    ONLINE("ONLINE")
}
enum class category(val state: Int){
    PETS(1),
    TECHNOLOGY(2),
    HOME(3),
    KIDS(4),
    LIFE_STYLE(5),
    OTHER(6)
}