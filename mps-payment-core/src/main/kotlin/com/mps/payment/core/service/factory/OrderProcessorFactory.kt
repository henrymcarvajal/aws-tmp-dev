package com.mps.payment.core.service.factory

import com.mps.payment.core.enum.PaymentMethod
import com.mps.payment.core.service.processor.CODProcessor
import com.mps.payment.core.service.processor.MPSProcessor
import com.mps.payment.core.service.processor.OrderProcessorTemplate
import com.mps.payment.core.service.processor.WarrantyProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OrderProcessorFactory {

    @Autowired
    private lateinit var codProcessor: CODProcessor

    @Autowired
    private lateinit var warrantyProcessor: WarrantyProcessor

    @Autowired
    private lateinit var mpsProcessor: MPSProcessor


    fun selectProcessor(paymentMethod:String):OrderProcessorTemplate?{
        return when(paymentMethod){
            PaymentMethod.MPS.method->mpsProcessor
            PaymentMethod.COD.method->codProcessor
            PaymentMethod.ONLINE.method->warrantyProcessor
            else -> null
        }
    }
}