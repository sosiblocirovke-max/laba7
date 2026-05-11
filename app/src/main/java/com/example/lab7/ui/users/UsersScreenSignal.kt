package com.example.lab7.ui.users

sealed interface UsersScreenSignal {
    data object OfflineWithCache : UsersScreenSignal
}
