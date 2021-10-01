package com.mps.payment.web.controller

import com.mps.common.dto.OrderDropDTO
import com.mps.common.dto.UpdateOrderRequest
import com.mps.payment.core.model.OrderViewConsolidate
import com.mps.payment.core.model.QueryParams
import com.mps.payment.core.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["order"])
class OrderController(private val orderService: OrderService) {


    @GetMapping("/dropseller")
    fun getDropOrdersPerMerchant(@RequestParam("merchantId") merchantId: UUID,
                              @RequestParam("durationInDays") duration: Int?,
                              @RequestParam("orderState") orderState: Int?,
                                 @RequestParam("contactNumber") contactNumber: Long?,
                                 @RequestParam("guideNumber") guideNumber: Int?,
                                 @RequestParam("pageNumber") pageNumber: Int?
    ): ResponseEntity<OrderViewConsolidate> {
        var queryParams = QueryParams(duration = duration ?: 30, state = orderState ?: 0,
                customerNumberContact = contactNumber,guideNumber = guideNumber,pageNumber = pageNumber?:1)
        val orders = orderService.getOrdersForDropshipperSeller(merchantId, queryParams)

        return if (orders.orders.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(orders)
        }
    }

    @GetMapping("/provider")
    fun getOrdersForProvider(@RequestParam("merchantId") merchantId: UUID,
                             @RequestParam("durationInDays") duration: Int?,
                             @RequestParam("orderState") orderState: Int?,
                             @RequestParam("contactNumber") contactNumber: Long?,
                             @RequestParam("guideNumber") guideNumber: Int?,
                             @RequestParam("pageNumber") pageNumber: Int?
    ): ResponseEntity<OrderViewConsolidate> {
        var queryParams = QueryParams(duration = duration ?: 30, state = orderState ?: 0,
                customerNumberContact = contactNumber, guideNumber = guideNumber, pageNumber = pageNumber ?: 1)
        val orders = orderService.getOrdersForProvider(merchantId, queryParams)

        return if (orders.orders.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(orders)
        }
    }

    @GetMapping("/customer/{id}")
    fun getOrderById(@PathVariable id: UUID
    ): ResponseEntity<OrderDropDTO> {
        val order = orderService.getOrderCompleteById(id)

        return if (order.isEmpty) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(order.get())
        }
    }

    @PutMapping("/public")
    fun updateOrder(@RequestBody request: UpdateOrderRequest): ResponseEntity<*> {
        val finalCustomer = orderService.updateOrder(request)
        return if(finalCustomer==null){
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error actualiando orden")
        }else{
            ResponseEntity.ok(finalCustomer)
        }
    }
}