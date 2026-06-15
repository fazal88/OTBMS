package com.olivetrust.charity.di

import com.olivetrust.charity.*
import com.olivetrust.charity.data.repository.*
import com.olivetrust.charity.domain.repository.*
import com.olivetrust.charity.ui.screens.*
import com.russhwolf.settings.Settings
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { Settings() }
    single { getDeviceInfo() }
    single { getLocationService() }
    singleOf(::FirestoreAuthRepository) bind AuthRepository::class
    singleOf(::FirestoreBeneficiaryRepository) bind BeneficiaryRepository::class
    singleOf(::FirestoreAidRepository) bind AidRepository::class
    singleOf(::FirestoreVisitRepository) bind VisitRepository::class
    singleOf(::FirestoreEmployeeRepository) bind EmployeeRepository::class
    singleOf(::FirestoreAuditRepository) bind AuditRepository::class
    
    factory { LoginViewModel(get(), get()) }
    factory { DashboardViewModel(get(), get(), get(), get()) }
    factory { OnboardingViewModel(get(), get(), get()) }
    factory { AidDistributionViewModel(get(), get(), get(), get()) }
    factory { VerificationVisitViewModel(get(), get(), get(), get()) }
    factory { BeneficiaryListViewModel(get()) }
    factory { BeneficiaryDetailViewModel(get(), get(), get(), get()) }
    factory { EmployeeManagementViewModel(get()) }
    factory { ApproveBeneficiaryViewModel(get(), get(), get()) }
    factory { VisitListViewModel(get()) }
    factory { AidListViewModel(get()) }
    factory { ApprovalListViewModel(get()) }
}
