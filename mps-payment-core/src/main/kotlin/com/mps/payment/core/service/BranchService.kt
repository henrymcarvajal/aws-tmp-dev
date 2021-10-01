package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.client.entregalo.EntregaloClient
import com.mps.payment.core.client.entregalo.payload.CreateBranchRequestInput
import com.mps.payment.core.controller.CreateBranchRequest
import com.mps.payment.core.controller.UpdateBranchRequest
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.model.Branch
import com.mps.payment.core.model.BranchDTO
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.BranchRepository
import com.mps.payment.core.repository.criteria.GenericSpecification
import com.mps.payment.core.repository.criteria.SearchCriteria
import com.mps.payment.core.repository.criteria.SearchOperation
import com.mps.payment.core.util.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class BranchService(
    private val branchRepository: BranchRepository,
    private val merchantService: MerchantService,
    private val entregaloClient: EntregaloClient,
    private val emailSender: EmailSender
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createBranch(createBranchRequest: CreateBranchRequest, userName: String): GenericResponse<*> {
        val searchCriteria = ArrayList<SearchCriteria>()
        val addressSearchCriteria = SearchCriteria(Branch.ADDRESS, createBranchRequest.branch.address, SearchOperation.EQUAL)
        val daneCodeSearchCriteria = SearchCriteria(Branch.DANE_CODE, createBranchRequest.branch.daneCodeId, SearchOperation.EQUAL)
        searchCriteria.add(addressSearchCriteria)
        searchCriteria.add(daneCodeSearchCriteria)
        val branches = findByCriteria(searchCriteria, userName)
        if (branches.isNotEmpty()) {
            log.error("createBranch: address already exists (address=${createBranchRequest.branch.address}, daneCodeId=${createBranchRequest.branch.daneCodeId})")
            return GenericResponse.ErrorResponse("createBranch: address already exists (address=${createBranchRequest.branch.address}, daneCode=${createBranchRequest.branch.daneCodeId})")
        }
        val request =
            CreateBranchRequestInput(createBranchRequest.branch.name,createBranchRequest.branch.address,createBranchRequest.contact.name,
                createBranchRequest.contact.phone.toLong(),createBranchRequest.contact.email,createBranchRequest.contact.identification)
        val response =
            entregaloClient.saveBranch(request)

        val merchantList = merchantService.findByEmail(userName)

        val branchDTO =
            BranchDTO(
                null,
                name= createBranchRequest.branch.name,
                createBranchRequest.branch.address,
                createBranchRequest.branch.daneCodeId,
                merchantList[0].id,
                createBranchRequest.contact.name,
                createBranchRequest.contact.identification,
                createBranchRequest.contact.email,
                createBranchRequest.contact.phone.toLong(),
                branchCode = response.branchCode
            )
        emailSender.sendEmailWithTemplate(receiver = "operativo@mipagoseguro.co", templateName = TEMPLATE_EMAIL_PLANE_TEXT,
            title = "Sucursal Creada exitosamente", o = mapOf(CONST_MESSAGE to "el comercio ${merchantList[0].name} ha creado la sucursal ${branchDTO.name} con codigo ${branchDTO.branchCode}"))

        return try {

            val branch = branchRepository.save(branchDTO.toEntity())
            GenericResponse.SuccessResponse(branch.toDTO())

        } catch (e: Exception) {
            log.error("createBranch: unexpected error creating branch (exception=${ExceptionUtils.toString(e)})")
            GenericResponse.ErrorResponse("Unexpected error creating branch (exception=${ExceptionUtils.toString(e)})")
        }
    }

    fun updateBranch(updateBranchRequest: UpdateBranchRequest, userName: String): GenericResponse<*> {
        val merchantList = merchantService.findByEmail(userName)
        val merchantIdInSession = merchantList[0].id
        val branchList = branchRepository.findByMerchantIdAndDisabledFalseOrderByCreationDateAsc(merchantList[0].id!!)
        if (branchList.isEmpty()) {
            log.error("updateBranch: branches do not exist for merchant (merchantId=${merchantIdInSession}")
            return GenericResponse.ErrorResponse("updateBranch: branches do not exist for merchant (merchantId=${merchantIdInSession}")
        }

        val branch = branchList.firstOrNull {
            it.id == updateBranchRequest.id
        }

        if (branch != null) {
            if (branch.merchantId != merchantIdInSession) {
                log.error("updateBranch: user cannot update this branch, it is not the owner (branchId=${branch.id})")
                return GenericResponse.ErrorResponse("User cannot update this branch, it is not the owner")
            }
            branch.address = updateBranchRequest.branch.address
            branch.daneCodeId = updateBranchRequest.branch.daneCodeId
            branch.modificationDate = LocalDateTime.now()

            return try {
                branchRepository.save(branch)
                GenericResponse.SuccessResponse(branch.toDTO())
            } catch (e: Exception) {
                log.error("updateBranch: unexpected error updating branch aa (exception=${ExceptionUtils.toString(e)})")
                GenericResponse.ErrorResponse("Unexpected error updating branch bb (exception=${ExceptionUtils.toString(e)}")
            }
        } else {
            log.error("updateBranch: branch does not exist for authenticated merchant (branchId=${updateBranchRequest.id}, merchant userName=${userName}")
            return GenericResponse.ErrorResponse("updateBranch: branches do not exist (merchantId=${userName}")
        }
    }

    fun removeBranchesById(ids: List<UUID>, userName: String): GenericResponse<*> {
        val branchList = branchRepository.findAllById(ids.toMutableList())

        return if (branchList.isEmpty()) {
            log.error("removeBranchesById: error finding branches by id (ids=$ids)")
            GenericResponse.ErrorResponse("Error finding branches by id (ids=$ids)")
        } else {
            val merchantList = merchantService.findByEmail(userName)
            val merchantIdInSession = merchantList[0].id

            val branches = arrayListOf<Branch>()
            branchList.forEach {
                if (it.merchantId == merchantIdInSession) {
                    it.disabled = true
                    it.deletionDate = LocalDateTime.now()
                    branches.add(it)
                }
            }
            try {
                branchRepository.saveAll(branches)
                GenericResponse.SuccessResponse("Branches deleted successfully")
            } catch (e: Exception) {
                log.error("updateBranch: unexpected error updating branch (exception=${ExceptionUtils.toString(e)})")
                GenericResponse.ErrorResponse("Unexpected error updating branch (exception=${ExceptionUtils.toString(e)})")
            }
        }
    }

    fun findById(id: UUID) = branchRepository.findById(id)

    fun findByMerchantId(merchantId: UUID, userName: String): List<Branch> {
        val searchCriteria = mutableListOf<SearchCriteria>()

        val searchCriteriaMerchantId = SearchCriteria(Branch.MERCHANT_ID, merchantId, SearchOperation.EQUAL)
        searchCriteria.add(searchCriteriaMerchantId)

        addDefaultPolicyOnDisabledObjects(searchCriteria)

        return findByCriteria(searchCriteria, userName)
    }

    fun findByCriteria(searchCriteriaItems: List<SearchCriteria>, userName: String): List<Branch> {
        val merchantInSession = merchantService.findByEmail(userName)
        if (merchantInSession.isEmpty()) {
            GenericResponse.ErrorResponse("getBranchesByCriteria: User does not exist (userName=${userName})")
        }
        val genericSpecification = GenericSpecification<Branch>()
        searchCriteriaItems.forEach {
            genericSpecification.add(SearchCriteria(it.key, it.value, it.operation))
        }
        val merchantIdInSession = merchantInSession[0].id
        return branchRepository.findAll(genericSpecification)
    }

    fun addDefaultPolicyOnDisabledObjects(searchCriteria: MutableList<SearchCriteria>) {
        val searchCriteriaEnabledNotNull = SearchCriteria(Branch.DISABLED, true, SearchOperation.EXISTENT)
        searchCriteria.add(searchCriteriaEnabledNotNull)

        val searchCriteriaEnabledTrue = SearchCriteria(Branch.DISABLED, false, SearchOperation.EQUAL)
        searchCriteria.add(searchCriteriaEnabledTrue)
    }

    fun findByBranchCode(branchCode:Int) = branchRepository.findByBranchCodeAndDisabledFalse(branchCode)


}