package com.example.lab7.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lab7.R
import com.example.lab7.domain.model.Post
import com.example.lab7.domain.model.User
import com.example.lab7.ui.common.UiState
import com.example.lab7.ui.theme.Lab7Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    viewModel: UserDetailViewModel,
    onBack: () -> Unit,
    onUserDeleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineCacheMessage = stringResource(R.string.snackbar_offline_cache)

    LaunchedEffect(viewModel) {
        viewModel.signals.collect { signal ->
            when (signal) {
                UserDetailScreenSignal.OfflineWithCache ->
                    snackbarHostState.showSnackbar(offlineCacheMessage)
            }
        }
    }

    var showEditUser by remember { mutableStateOf(false) }
    var showDeleteUser by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    val successUser = (state as? UiState.Success<UserDetailContent>)?.data?.user
    if (showEditUser && successUser != null) {
        EditUserDetailDialog(
            user = successUser,
            onDismiss = { showEditUser = false },
            onSave = {
                viewModel.updateUser(it)
                showEditUser = false
            }
        )
    }

    if (showDeleteUser) {
        AlertDialog(
            onDismissRequest = { showDeleteUser = false },
            title = { Text("Удалить пользователя?") },
            text = {
                Text(
                    "Пользователь и все его посты будут удалены только из локальной базы.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteUser = false
                        viewModel.deleteCurrentUser(onUserDeleted)
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteUser = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    postToEdit?.let { post ->
        EditPostDialog(
            post = post,
            onDismiss = { postToEdit = null },
            onSave = {
                viewModel.updatePost(it)
                postToEdit = null
            }
        )
    }

    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Удалить пост?") },
            text = { Text(post.title, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(post.id)
                        postToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    val topTitle = when (val s = state) {
        is UiState.Success -> s.data.user.name
        UiState.Loading -> "Загрузка…"
        is UiState.Error -> "Ошибка"
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(topTitle) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("назад")
                    }
                },
                actions = {
                    if (state is UiState.Success<*>) {
                        TextButton(onClick = { showEditUser = true }) {
                            Text("Изменить")
                        }
                        TextButton(onClick = { showDeleteUser = true }) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Обновить")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val s = state) {
            UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            is UiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { viewModel.refresh() }) {
                    Text(stringResource(R.string.error_retry))
                }
            }

            is UiState.Success -> {
                val user = s.data.user
                val posts = s.data.posts
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Телефон: ${user.phone.ifBlank { "—" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    text = "Сайт: ${user.website.ifBlank { "—" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Компания: ${user.companyName.ifBlank { "—" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Город: ${user.city.ifBlank { "—" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Посты",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(posts, key = { it.id }) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = post.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = post.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { postToEdit = post }) {
                                        Text("Изменить")
                                    }
                                    TextButton(onClick = { postToDelete = post }) {
                                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    }
                    if (s.data.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditUserDetailDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var name by remember(user.id) { mutableStateOf(user.name) }
    var username by remember(user.id) { mutableStateOf(user.username) }
    var email by remember(user.id) { mutableStateOf(user.email) }
    var city by remember(user.id) { mutableStateOf(user.city) }
    var phone by remember(user.id) { mutableStateOf(user.phone) }
    var website by remember(user.id) { mutableStateOf(user.website) }
    var company by remember(user.id) { mutableStateOf(user.companyName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Данные пользователя") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Город") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Сайт") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Компания") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        User(
                            id = user.id,
                            name = name,
                            username = username,
                            email = email,
                            phone = phone,
                            website = website,
                            companyName = company,
                            city = city
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun EditPostDialog(
    post: Post,
    onDismiss: () -> Unit,
    onSave: (Post) -> Unit
) {
    var title by remember(post.id) { mutableStateOf(post.title) }
    var body by remember(post.id) { mutableStateOf(post.body) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование поста") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Текст") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Post(
                            id = post.id,
                            userId = post.userId,
                            title = title,
                            body = body
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun UserDetailScreenPreview() {
    Lab7Theme {
        Text("Откройте экран на устройстве / эмуляторе")
    }
}
