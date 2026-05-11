package com.example.lab7.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lab7.data.repository.PostRepository
import com.example.lab7.data.repository.UserRepository
import com.example.lab7.domain.model.Post
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

class UserDetailViewModel(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val userId: Int
) : ViewModel() {

    private val initialFetchFinished = MutableStateFlow(false)
    private val fetchFailure = MutableStateFlow<Throwable?>(null)
    private val detailRefreshing = MutableStateFlow(false)

    private val _uiState = MutableStateFlow<UiState<UserDetailContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<UserDetailContent>> = _uiState.asStateFlow()

    private val _signals = MutableSharedFlow<UserDetailScreenSignal>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val signals: SharedFlow<UserDetailScreenSignal> = _signals.asSharedFlow()

    private var lastKnownUser: User? = null
    private var lastKnownPosts: List<Post> = emptyList()

    init {
        viewModelScope.launch {
            launch {
                combine(
                    userRepository.observeUserById(userId),
                    postRepository.observePostsByUser(userId),
                    initialFetchFinished,
                    fetchFailure,
                    detailRefreshing
                ) { user, posts, done, failure, refreshing ->
                    if (user != null) {
                        lastKnownUser = user
                        lastKnownPosts = posts
                    }
                    when {
                        user != null ->
                            UiState.Success(
                                UserDetailContent(user, posts, isRefreshing = refreshing)
                            )

                        !done -> UiState.Loading

                        failure != null ->
                            UiState.Error(
                                failure.message.orEmpty().ifBlank { "Не удалось загрузить пользователя" },
                                failure
                            )

                        else ->
                            UiState.Error("Пользователь не найден", null)
                    }
                }.collect { state ->
                    _uiState.value = state
                }
            }
            try {
                userRepository.getUserById(userId).getOrThrow()
                postRepository.getPostsByUser(userId).getOrThrow()
            } catch (e: Throwable) {
                when {
                    lastKnownUser != null && e.isOfflineNetworkError() ->
                        _signals.tryEmit(UserDetailScreenSignal.OfflineWithCache)

                    lastKnownUser != null -> Unit

                    else -> fetchFailure.value = e
                }
            } finally {
                initialFetchFinished.value = true
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            detailRefreshing.update { true }
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
                    userRepository.getUserById(userId).getOrThrow()
                    postRepository.getPostsByUser(userId).getOrThrow()
                } catch (e: Throwable) {
                    when {
                        lastKnownUser != null && e.isOfflineNetworkError() -> {
                            _uiState.value = UiState.Success(
                                UserDetailContent(
                                    user = lastKnownUser!!,
                                    posts = lastKnownPosts,
                                    isRefreshing = false
                                )
                            )
                            _signals.tryEmit(UserDetailScreenSignal.OfflineWithCache)
                        }

                        lastKnownUser != null -> {
                            _uiState.value = UiState.Success(
                                UserDetailContent(
                                    user = lastKnownUser!!,
                                    posts = lastKnownPosts,
                                    isRefreshing = false
                                )
                            )
                        }

                        else -> {
                            _uiState.value = UiState.Error(
                                e.message.orEmpty().ifBlank { "Не удалось обновить" },
                                e
                            )
                        }
                    }
                }
            } finally {
                detailRefreshing.value = false
            }
        }
    }

    fun updateUser(user: User) {
        require(user.id == userId) { "Неверный id пользователя" }
        viewModelScope.launch {
            try {
                userRepository.updateUser(user).getOrThrow()
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(
                    e.message.orEmpty().ifBlank { "Не удалось сохранить пользователя" },
                    e
                )
            }
        }
    }

    fun deleteCurrentUser(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                postRepository.deleteAllPostsForUser(userId).getOrThrow()
                userRepository.deleteUser(userId).getOrThrow()
                onDeleted()
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(
                    e.message.orEmpty().ifBlank { "Не удалось удалить" },
                    e
                )
            }
        }
    }

    fun updatePost(post: Post) {
        require(post.userId == userId) { "Пост не принадлежит этому пользователю" }
        viewModelScope.launch {
            try {
                postRepository.updatePost(post).getOrThrow()
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(
                    e.message.orEmpty().ifBlank { "Не удалось сохранить пост" },
                    e
                )
            }
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(postId).getOrThrow()
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(
                    e.message.orEmpty().ifBlank { "Не удалось удалить пост" },
                    e
                )
            }
        }
    }
}

class UserDetailViewModelFactory(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val userId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UserDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return UserDetailViewModel(userRepository, postRepository, userId) as T
    }
}
