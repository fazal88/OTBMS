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
    singleOf(::FirestoreEventRepository) bind EventRepository::class
    singleOf(::FirestoreDonationBoxRepository) bind DonationBoxRepository::class
    singleOf(::FirestoreNotificationRepository) bind NotificationRepository::class
    
    factory { LoginViewModel(get(), get()) }
    factory { DashboardViewModel(get(), get(), get(), get(), get()) }
    factory { OnboardingViewModel(get(), get(), get()) }
    factory { AidDistributionViewModel(get(), get(), get(), get()) }
    factory { VerificationVisitViewModel(get(), get(), get(), get()) }
    factory { BeneficiaryListViewModel(get(), get()) }
    factory { BeneficiaryDetailViewModel(get(), get(), get(), get()) }
    factory { EmployeeManagementViewModel(get()) }
    factory { ApproveBeneficiaryViewModel(get(), get(), get()) }
    factory { VisitListViewModel(get()) }
    factory { AidListViewModel(get(), get()) }
    factory { ApprovalListViewModel(get()) }
    factory { EventListViewModel(get()) }
    factory { CreateEventViewModel(get(), get()) }
    factory { (eventId: String) -> EventDetailViewModel(eventId, get(), get(), get(), get()) }

    factory { DonationBoxListViewModel(get(), get(), get()) }
    factory { (boxId: String) -> DonationBoxDetailViewModel(boxId, get(), get(), get()) }
    factory { InstallDonationBoxViewModel(get(), get(), get()) }
    factory { (boxId: String) -> RecordCollectionViewModel(boxId, get(), get(), get()) }
    factory { (boxId: String) -> ReportIssueViewModel(boxId, get(), get(), get()) }
    factory { NotificationTopicsViewModel(get(), get()) }
}
