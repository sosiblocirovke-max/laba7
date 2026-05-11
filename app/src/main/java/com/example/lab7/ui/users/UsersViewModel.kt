package com.example.lab7.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lab7.data.repository.PostRepository
import com.example.lab7.data.repository.UserRepository
import com.example.lab7.domain.model.User
import com.example.lab7.ui.common.UiState
import com.example.lab7.util.isOfflineNetworkError
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UsersViewModel(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val remoteInitialized = MutableStateFlow(false)
    private val initRefreshError = MutableStateFlow<Throwable?>(null)
    private val listRefreshing = MutableStateFlow(false)

    private val _uiState = MutableStateFlow<UiState<UsersListContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<UsersListContent>> = _uiState.asStateFlow()

    private val _signals = MutableSharedFlow<UsersScreenSignal>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val signals: SharedFlow<UsersScreenSignal> = _signals.asSharedFlow()

    private var lastEmittedUsers: List<User> = emptyList()

    init {
        viewModelScope.launch {
            launch {
                combine(
                    userRepository.observeUsers(),
                    remoteInitialized,
                    initRefreshError,
                    listRefreshing
                ) { users, remoteDone, initErr, refreshing ->
                    lastEmittedUsers = users
                    when {
                        users.isNotEmpty() ->
                            UiState.Success(UsersListContent(users, isRefreshing = refreshing))

                        !remoteDone -> UiState.Loading

                        initErr != null ->
                            UiState.Error(
                                initErr.message.orEmpty().ifBlank { "Unknown error" },
                                initErr
                            )

                        else ->
                            UiState.Success(UsersListContent(users = emptyList(), isRefreshing = refreshing))
                    }
                }.collect { state ->
                    _uiState.value = state
                }
            }
            try {
                userRepository.refreshUsers().getOrThrow()
            } catch (e: Throwable) {
                when {
                    lastEmittedUsers.isNotEmpty() && e.isOfflineNetworkError() ->
                        _signals.tryEmit(UsersScreenSignal.OfflineWithCache)

                    lastEmittedUsers.isNotEmpty() -> Unit

                    else -> initRefreshError.value = e
                }
            } finally {
                remoteInitialized.value = true
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            listRefreshing.update { true }
            try {
                when (val current = _uiState.value) {
                    is UiState.Success -> {
                        _uiState.value = UiState.Success(
                            current.data.copy(isRefreshing = true)
                        )
                    }

                    else -> {
                        _uiState.value = UiState.Loading
                    }
                }
                try {
                    userRepository.refreshUsers().getOrThrow()
                } catch (e: Throwable) {
                    when {
                        lastEmittedUsers.isNotEmpty() && e.isOfflineNetworkError() -> {
                            _uiState.value = UiState.Success(
                                UsersListContent(lastEmittedUsers, isRefreshing = false)
                            )
                            _signals.tryEmit(UsersScreenSignal.OfflineWithCache)
                        }

                        lastEmittedUsers.isNotEmpty() -> {
                            _uiState.value = UiState.Success(
                                UsersListContent(lastEmittedUsers, isRefreshing = false)
                            )
                        }

                        else -> {
                            _uiState.value = UiState.Error(
                                e.message.orEmpty().ifBlank { "Unknown error" },
                                e
                            )
                        }
                    }
                }
            } finally {
                listRefreshing.value = false
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                userRepository.updateUser(user).getOrThrow()
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(
                    e.message.orEmpty().ifBlank { "Не удалось сохранить" },
                    e
                )
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            try {
                postRepository.deleteAllPostsForUser(userId).getOrThrow()
                userRepository.deleteUser(userId).getOrThrow()
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(
                    e.message.orEmpty().ifBlank { "Не удалось удалить" },
                    e
                )
            }
        }
    }
}

class UsersViewModelFactory(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UsersViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return UsersViewModel(userRepository, postRepository) as T
    }
}
