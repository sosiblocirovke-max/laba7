package com.example.lab7.ui.users

sealed interface UserDetailScreenSignal {
    data object OfflineWithCache : UserDetailScreenSignal
}
