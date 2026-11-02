package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.CallSession
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.CreatorProfile
import com.example.data.InstaWatchRepository
import com.example.data.MediaPost
import com.example.data.PostComment
import com.example.data.StoryItem
import com.example.data.UserAccount
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainTab {
    FEED,
    WATCH,
    CREATE,
    REELS,
    CONNECT,
    PROFILE,
}

enum class CreateFormMode {
    PHOTO,
    REEL,
    LONG_VIDEO,
    CREATOR_FORM,
}

@OptIn(ExperimentalCoroutinesApi::class)
class InstaWatchViewModel(
    private val repository: InstaWatchRepository,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    private val initialAvatarUrl: String,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.FEED)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _createFormMode = MutableStateFlow(CreateFormMode.PHOTO)
    val createFormMode: StateFlow<CreateFormMode> = _createFormMode.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _activeCommentPost = MutableStateFlow<MediaPost?>(null)
    val activeCommentPost: StateFlow<MediaPost?> = _activeCommentPost.asStateFlow()

    private val _activeSharePost = MutableStateFlow<MediaPost?>(null)
    val activeSharePost: StateFlow<MediaPost?> = _activeSharePost.asStateFlow()

    private val _activeChat = MutableStateFlow<ChatThread?>(null)
    val activeChat: StateFlow<ChatThread?> = _activeChat.asStateFlow()

    private val _activeCallId = MutableStateFlow<String?>(null)
    val activeCallId: StateFlow<String?> = _activeCallId.asStateFlow()

    val myProfileState: StateFlow<UiState<CreatorProfile?>> = repository.observeMyProfile(currentUserId)
        .map<CreatorProfile?, UiState<CreatorProfile?>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing profile", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val myAccountState: StateFlow<UiState<UserAccount?>> = repository.observeMyPrivateAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing account", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load account settings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val discoverableProfilesState: StateFlow<UiState<List<CreatorProfile>>> = repository.observeDiscoverableProfiles()
        .map<List<CreatorProfile>, UiState<List<CreatorProfile>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing creators", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load creators"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val storiesState: StateFlow<UiState<List<StoryItem>>> = repository.observePublicStories()
        .map<List<StoryItem>, UiState<List<StoryItem>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing stories", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load stories"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publicPostsState: StateFlow<UiState<List<MediaPost>>> = repository.observePublicPosts()
        .map<List<MediaPost>, UiState<List<MediaPost>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing public posts", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val myPostsState: StateFlow<UiState<List<MediaPost>>> = repository.observeMyPosts(currentUserId)
        .map<List<MediaPost>, UiState<List<MediaPost>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing own posts", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load your uploads"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val chatsState: StateFlow<UiState<List<ChatThread>>> = repository.observeChats(currentUserId)
        .map<List<ChatThread>, UiState<List<ChatThread>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing chats", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val callsState: StateFlow<UiState<List<CallSession>>> = repository.observeCalls(currentUserId)
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing calls", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load call logs"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val activeCommentsState: StateFlow<UiState<List<PostComment>>> = _activeCommentPost
        .flatMapLatest { post ->
            if (post == null || post.id.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(post.id)
                    .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
                    .catch { err ->
                        Log.w(TAG, "Error observing comments", err)
                        emit(UiState.Error(err.localizedMessage ?: "Failed to load comments"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = _activeChat
        .flatMapLatest { chat ->
            if (chat == null || chat.id.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(chat.id, currentUserId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { err ->
                        Log.w(TAG, "Error observing messages", err)
                        emit(UiState.Error(err.localizedMessage ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    val activeCallSessionState: StateFlow<CallSession?> = _activeCallId
        .flatMapLatest { callId ->
            if (callId.isNullOrBlank()) {
                flowOf(null)
            } else {
                repository.observeCallSession(callId)
                    .catch { err ->
                        Log.w(TAG, "Error observing active call", err)
                        emit(null)
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        viewModelScope.launch {
            repository.ensureUserBootstrapped(
                displayName = initialDisplayName,
                email = initialEmail,
                avatarUrl = initialAvatarUrl,
            ).onFailure { err ->
                Log.w(TAG, "Profile bootstrap failed", err)
            }
        }
    }

    fun applySignUpDraft(
        channelName: String,
        handle: String,
        category: String,
    ) {
        viewModelScope.launch {
            repository.ensureUserBootstrapped(
                displayName = channelName.ifBlank { initialDisplayName },
                email = initialEmail,
                avatarUrl = initialAvatarUrl,
            )
            repository.saveCreatorRegistrationForm(
                displayName = channelName.ifBlank { initialDisplayName.ifBlank { "Creator" } },
                handle = handle.ifBlank { channelName.ifBlank { initialDisplayName } },
                bio = "Creating videos, Reels & moments on InstaWatch",
                creatorCategory = category.ifBlank { "Entertainment" },
                phone = "",
                isDiscoverable = true,
                notificationsEnabled = true,
                avatarUrl = initialAvatarUrl,
                email = initialEmail,
            ).onSuccess {
                _statusBannerMessage.value = "Welcome to InstaWatch! Your creator account is ready."
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openCreateWithMode(mode: CreateFormMode) {
        _createFormMode.value = mode
        _selectedTab.value = MainTab.CREATE
    }

    fun setCreateFormMode(mode: CreateFormMode) {
        _createFormMode.value = mode
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    private fun currentProfileOrDefault(): CreatorProfile {
        val loaded = (myProfileState.value as? UiState.Success)?.data
        return loaded ?: CreatorProfile(
            userId = currentUserId,
            displayName = initialDisplayName.ifBlank { "Creator" },
            handle = "@${initialDisplayName.lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank { currentUserId.take(6) }}",
            avatarUrl = initialAvatarUrl,
        )
    }

    fun saveCreatorForm(
        displayName: String,
        handle: String,
        bio: String,
        creatorCategory: String,
        phone: String,
        isDiscoverable: Boolean,
        notificationsEnabled: Boolean,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.saveCreatorRegistrationForm(
                displayName = displayName,
                handle = handle,
                bio = bio,
                creatorCategory = creatorCategory,
                phone = phone,
                isDiscoverable = isDiscoverable,
                notificationsEnabled = notificationsEnabled,
                avatarUrl = currentProfileOrDefault().avatarUrl.ifBlank { initialAvatarUrl },
                email = initialEmail,
            ).onSuccess {
                _statusBannerMessage.value = "Creator channel & registration form saved!"
                _selectedTab.value = MainTab.PROFILE
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Could not save form"
            }
            _isSubmitting.value = false
        }
    }

    fun publishPost(
        mediaType: String,
        title: String,
        caption: String,
        category: String,
        mediaUrl: String,
        durationSec: Long,
        audioTrack: String,
        locationTag: String,
        tagsRaw: String,
        isPublic: Boolean,
        thumbnailBase64: String?,
        onPublished: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val parsedTags = tagsRaw.split(",", " ")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { if (it.startsWith("#")) it else "#$it" }
                .take(8)
            val profile = currentProfileOrDefault()
            repository.ensureUserBootstrapped(initialDisplayName, initialEmail, initialAvatarUrl)
            repository.publishMediaPost(
                profile = profile,
                mediaType = mediaType,
                title = title,
                caption = caption,
                category = category,
                mediaUrl = mediaUrl,
                durationSec = durationSec,
                audioTrack = audioTrack,
                locationTag = locationTag,
                tags = parsedTags,
                visibility = if (isPublic) "public" else "private",
                thumbnailBase64 = thumbnailBase64,
            ).onSuccess {
                onPublished()
                _statusBannerMessage.value = when (mediaType) {
                    "long_video" -> "Long video published to Watch (+35 Real Score)!"
                    "reel" -> "Reel published (+25 Real Score)!"
                    else -> "Photo published to Feed (+15 Real Score)!"
                }
                _selectedTab.value = when (mediaType) {
                    "long_video" -> MainTab.WATCH
                    "reel" -> MainTab.REELS
                    else -> MainTab.FEED
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Upload failed"
            }
            _isSubmitting.value = false
        }
    }

    fun publishStory(
        caption: String,
        accentColorKey: String,
        thumbnailBase64: String?,
        localMediaUri: String?,
    ) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.ensureUserBootstrapped(initialDisplayName, initialEmail, initialAvatarUrl)
            repository.createStory(
                profile = profile,
                caption = caption,
                accentColorKey = accentColorKey,
                thumbnailBase64 = thumbnailBase64,
                localMediaUri = localMediaUri,
            ).onSuccess {
                _statusBannerMessage.value = "Story shared (+5 Real Score)!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Could not post story"
            }
        }
    }

    fun markStoryViewed(story: StoryItem) {
        viewModelScope.launch {
            repository.incrementStoryViews(story)
        }
    }

    fun toggleLike(post: MediaPost) {
        viewModelScope.launch {
            repository.toggleLikePost(post)
        }
    }

    fun recordPostView(post: MediaPost) {
        viewModelScope.launch {
            repository.incrementPostViews(post)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onSuccess {
                _statusBannerMessage.value = "Post deleted"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Could not delete post"
            }
        }
    }

    fun openComments(post: MediaPost) {
        _activeCommentPost.value = post
    }

    fun closeComments() {
        _activeCommentPost.value = null
    }

    fun submitComment(text: String) {
        val post = _activeCommentPost.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(post, currentProfileOrDefault(), text).onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to add comment"
            }
        }
    }

    fun openShareSheet(post: MediaPost) {
        _activeSharePost.value = post
    }

    fun closeShareSheet() {
        _activeSharePost.value = null
    }

    fun sharePostToChat(chat: ChatThread) {
        val post = _activeSharePost.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chat = chat,
                senderName = currentProfileOrDefault().displayName,
                text = "Shared ${post.mediaType.replace('_', ' ')}: ${post.title}",
                messageType = "shared_post",
                sharedPostId = post.id,
                thumbnailBase64 = post.thumbnailBase64,
            ).onSuccess {
                _activeSharePost.value = null
                _statusBannerMessage.value = "Shared to ${chat.chatTitle}"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to share"
            }
        }
    }

    fun openChat(chat: ChatThread) {
        _activeChat.value = chat
    }

    fun closeActiveChat() {
        _activeChat.value = null
    }

    fun startOrOpenChatWithPeer(peerProfile: CreatorProfile) {
        viewModelScope.launch {
            val myProfile = currentProfileOrDefault()
            repository.ensureUserBootstrapped(initialDisplayName, initialEmail, initialAvatarUrl)
            repository.getOrCreateChatWithPeer(myProfile, peerProfile).onSuccess { chat ->
                _selectedTab.value = MainTab.CONNECT
                _activeChat.value = chat
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Could not open chat"
            }
        }
    }

    fun openSavedNotesChat() {
        startOrOpenChatWithPeer(currentProfileOrDefault())
    }

    fun sendChatMessage(
        text: String,
        messageType: String = "text",
        thumbnailBase64: String? = null,
    ) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chat = chat,
                senderName = currentProfileOrDefault().displayName,
                text = text,
                messageType = messageType,
                thumbnailBase64 = thumbnailBase64,
            ).onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Message failed to send"
            }
        }
    }

    fun initiateCall(peerProfile: CreatorProfile, callType: String) {
        viewModelScope.launch {
            val myProfile = currentProfileOrDefault()
            repository.ensureUserBootstrapped(initialDisplayName, initialEmail, initialAvatarUrl)
            repository.startCall(myProfile, peerProfile, callType).onSuccess { call ->
                _activeCallId.value = call.id
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to start call"
            }
        }
    }

    fun startSelfTestCall(callType: String) {
        initiateCall(currentProfileOrDefault(), callType)
    }

    fun acceptIncomingCall(call: CallSession) {
        viewModelScope.launch {
            repository.answerCall(call.id).onSuccess {
                _activeCallId.value = call.id
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Could not answer call"
            }
        }
    }

    fun declineIncomingCall(call: CallSession) {
        viewModelScope.launch {
            repository.endOrDeclineCall(call.id, "declined", 0L)
        }
    }

    fun updateActiveCallControls(callId: String, isMuted: Boolean, isCameraEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateCallMediaState(callId, isMuted, isCameraEnabled)
        }
    }

    fun endActiveCall(durationSec: Long) {
        val callId = _activeCallId.value ?: return
        _activeCallId.value = null
        viewModelScope.launch {
            repository.endOrDeclineCall(callId, "ended", durationSec)
        }
    }

    companion object {
        private const val TAG = "InstaWatchVM"
        private const val STOP_TIMEOUT_MS = 5000L

        fun factory(
            currentUserId: String,
            displayName: String,
            email: String,
            avatarUrl: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                InstaWatchViewModel(
                    repository = InstaWatchRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email,
                    initialAvatarUrl = avatarUrl,
                )
            }
        }
    }
}
