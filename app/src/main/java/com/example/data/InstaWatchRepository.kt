package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InstaWatchRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeHandle(raw: String, fallbackUid: String): String {
        val cleaned = raw.lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .take(24)
        val base = if (cleaned.length >= 2) cleaned else "user_${fallbackUid.take(6).lowercase()}"
        return "@$base"
    }

    // --- 1. Onboarding & Creator Registration Form ---

    suspend fun ensureUserBootstrapped(
        displayName: String,
        email: String,
        avatarUrl: String,
    ): Result<CreatorProfile> = runCatching {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "InstaWatch Creator" }.take(60)
        val safeEmail = email.trim().ifBlank { "$uid@instawatch.user" }.take(254)
        val profileRef = db.collection("profiles").document(uid)
        val accountRef = db.collection("users").document(uid)

        val existingSnap = try {
            profileRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, profileRef.path)
            throw e
        }

        val profile = if (existingSnap.exists()) {
            existingSnap.toObject(
                CreatorProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CreatorProfile(userId = uid, displayName = safeName, handle = sanitizeHandle(safeName, uid))
        } else {
            val newProfile = CreatorProfile(
                userId = uid,
                displayName = safeName,
                handle = sanitizeHandle(safeName, uid),
                bio = "Creating videos, Reels & moments on InstaWatch",
                creatorCategory = "Entertainment",
                avatarUrl = avatarUrl.take(1000),
                isDiscoverable = true,
                realScore = 100L,
                postsCount = 0L,
                followersCount = 0L,
            )
            try {
                profileRef.set(newProfile.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, profileRef.path)
                throw e
            }
            newProfile.copy(id = uid)
        }

        val accountSnap = try {
            accountRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, accountRef.path)
            throw e
        }

        if (!accountSnap.exists()) {
            val account = UserAccount(
                userId = uid,
                email = safeEmail,
                notificationsEnabled = true,
            )
            try {
                accountRef.set(account.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, accountRef.path)
                throw e
            }
        }

        profile
    }

    suspend fun saveCreatorRegistrationForm(
        displayName: String,
        handle: String,
        bio: String,
        creatorCategory: String,
        phone: String,
        isDiscoverable: Boolean,
        notificationsEnabled: Boolean,
        avatarUrl: String,
        email: String,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Creator" }.take(60)
        val formattedHandle = if (handle.trim().startsWith("@")) {
            "@" + handle.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_]"), "").take(30).ifBlank { uid.take(6) }
        } else {
            sanitizeHandle(handle, uid)
        }
        val safeBio = bio.trim().take(300)
        val safeCategory = creatorCategory.trim().ifBlank { "Entertainment" }.take(40)
        val safeEmail = email.trim().ifBlank { "$uid@instawatch.user" }.take(254)
        val safePhone = phone.trim().take(30)

        val profileRef = db.collection("profiles").document(uid)
        val profileSnap = try {
            profileRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, profileRef.path)
            throw e
        }

        try {
            if (profileSnap.exists()) {
                profileRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "handle" to formattedHandle,
                        "bio" to safeBio,
                        "creatorCategory" to safeCategory,
                        "avatarUrl" to avatarUrl.take(1000),
                        "isDiscoverable" to isDiscoverable,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                val created = CreatorProfile(
                    userId = uid,
                    displayName = safeName,
                    handle = formattedHandle,
                    bio = safeBio,
                    creatorCategory = safeCategory,
                    avatarUrl = avatarUrl.take(1000),
                    isDiscoverable = isDiscoverable,
                    realScore = 120L,
                    postsCount = 0L,
                    followersCount = 0L,
                )
                profileRef.set(created.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            throw e
        }

        val userRef = db.collection("users").document(uid)
        val userSnap = try {
            userRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path)
            throw e
        }

        try {
            if (userSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "email" to safeEmail,
                    "notificationsEnabled" to notificationsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (safePhone.isNotBlank()) {
                    updates["phone"] = safePhone
                }
                userRef.update(updates).await()
            } else {
                val account = UserAccount(
                    userId = uid,
                    email = safeEmail,
                    phone = safePhone.ifBlank { null },
                    notificationsEnabled = notificationsEnabled,
                )
                userRef.set(account.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    fun observeMyProfile(userId: String): Flow<CreatorProfile?> = flow {
        val path = "profiles/$userId"
        emitAll(
            db.collection("profiles").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                    throw e
                }
        )
    }

    fun observeMyPrivateAccount(userId: String): Flow<UserAccount?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                    throw e
                }
        )
    }

    suspend fun getPrivateAccountById(targetUserId: String): Result<UserAccount?> = runCatching {
        val docRef = db.collection("users").document(targetUserId)
        try {
            val snap = docRef.get().await()
            snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeDiscoverableProfiles(): Flow<List<CreatorProfile>> = flow {
        val path = "profiles"
        emitAll(
            db.collection("profiles")
                .whereEqualTo("isDiscoverable", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.realScore }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    // --- 2. Stories / Status Bar ---

    fun observePublicStories(): Flow<List<StoryItem>> = flow {
        val path = "stories"
        emitAll(
            db.collection("stories")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun createStory(
        profile: CreatorProfile,
        caption: String,
        accentColorKey: String = "coral",
        thumbnailBase64: String? = null,
        localMediaUri: String? = null,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("stories").document()
        val story = StoryItem(
            authorId = uid,
            authorName = profile.displayName.ifBlank { "Creator" }.take(60),
            authorHandle = profile.handle.ifBlank { sanitizeHandle("creator", uid) }.take(35),
            authorAvatarUrl = profile.avatarUrl.take(1000),
            caption = caption.trim().ifBlank { "My InstaWatch Status" }.take(280),
            accentColorKey = if (accentColorKey in listOf("coral", "teal", "cobalt", "amber", "violet")) accentColorKey else "coral",
            visibility = "public",
            viewsCount = 1L,
            thumbnailBase64 = thumbnailBase64?.take(60000),
            localMediaUri = localMediaUri?.take(1000),
        )
        try {
            docRef.set(story.toCreateMap()).await()
            db.collection("profiles").document(uid).update(
                mapOf(
                    "realScore" to FieldValue.increment(5L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun incrementStoryViews(story: StoryItem): Result<Unit> = runCatching {
        if (story.id.isBlank()) return@runCatching
        val docRef = db.collection("stories").document(story.id)
        try {
            docRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 3. Unified Posts: Photos, Reels & Long-Form Watch Videos ---

    fun observePublicPosts(): Flow<List<MediaPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MediaPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeMyPosts(userId: String): Flow<List<MediaPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("authorId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MediaPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun getPostById(postId: String): Result<MediaPost?> = runCatching {
        val docRef = db.collection("posts").document(postId)
        try {
            val snap = docRef.get().await()
            snap.toObject(MediaPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun publishMediaPost(
        profile: CreatorProfile,
        mediaType: String,
        title: String,
        caption: String,
        category: String,
        mediaUrl: String,
        durationSec: Long,
        audioTrack: String,
        locationTag: String,
        tags: List<String>,
        visibility: String,
        thumbnailBase64: String?,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val validType = if (mediaType in listOf("photo", "reel", "long_video")) mediaType else "photo"
        val validVisibility = if (visibility in listOf("public", "private")) visibility else "public"
        val scoreReward = when (validType) {
            "long_video" -> 35L
            "reel" -> 25L
            else -> 15L
        }

        val docRef = db.collection("posts").document()
        val post = MediaPost(
            authorId = uid,
            authorName = profile.displayName.ifBlank { "Creator" }.take(60),
            authorHandle = profile.handle.ifBlank { sanitizeHandle("creator", uid) }.take(35),
            authorAvatarUrl = profile.avatarUrl.take(1000),
            mediaType = validType,
            title = title.trim().ifBlank { "Untitled Upload" }.take(120),
            caption = caption.trim().ifBlank { title.trim().ifBlank { "Shared on InstaWatch" } }.take(1000),
            category = category.trim().ifBlank { "Entertainment" }.take(40),
            mediaUrl = mediaUrl.trim().take(1000),
            durationSec = durationSec.coerceIn(0L, 36000L),
            audioTrack = audioTrack.trim().take(100),
            locationTag = locationTag.trim().take(80),
            tags = tags.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(8),
            visibility = validVisibility,
            likesCount = 0L,
            commentsCount = 0L,
            viewsCount = 1L,
            realScorePoints = scoreReward,
            likedByUids = emptyList(),
            thumbnailBase64 = thumbnailBase64?.take(60000),
        )

        try {
            docRef.set(post.toCreateMap()).await()
            db.collection("profiles").document(uid).update(
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "realScore" to FieldValue.increment(scoreReward),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }

        docRef.id
    }

    suspend fun toggleLikePost(post: MediaPost): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID required" }
        val docRef = db.collection("posts").document(post.id)
        val alreadyLiked = uid in post.likedByUids
        val updatedLikedUids = if (alreadyLiked) {
            post.likedByUids.filterNot { it == uid }.take(10)
        } else {
            (listOf(uid) + post.likedByUids.filterNot { it == uid }).take(10)
        }
        val newLikesCount = if (alreadyLiked) (post.likesCount - 1L).coerceAtLeast(0L) else post.likesCount + 1L
        val scoreDelta = if (alreadyLiked) -5L else 5L
        val newPostScore = (post.realScorePoints + scoreDelta).coerceAtLeast(0L)

        try {
            docRef.update(
                mapOf(
                    "likesCount" to newLikesCount,
                    "likedByUids" to updatedLikedUids,
                    "realScorePoints" to newPostScore,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            if (!alreadyLiked && post.visibility == "public") {
                val authorRef = db.collection("profiles").document(post.authorId)
                runCatching {
                    authorRef.update(
                        mapOf(
                            "realScore" to FieldValue.increment(5L),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun incrementPostViews(post: MediaPost): Result<Unit> = runCatching {
        if (post.id.isBlank()) return@runCatching
        val docRef = db.collection("posts").document(post.id)
        try {
            docRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "realScorePoints" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("posts").document(postId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 4. Post Comments ---

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun addComment(
        post: MediaPost,
        profile: CreatorProfile,
        text: String,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanText = text.trim().take(500)
        require(cleanText.isNotBlank()) { "Comment cannot be empty" }
        val commentRef = db.collection("posts").document(post.id).collection("comments").document()
        val comment = PostComment(
            postId = post.id,
            authorId = uid,
            authorName = profile.displayName.ifBlank { "Creator" }.take(60),
            authorHandle = profile.handle.ifBlank { sanitizeHandle("creator", uid) }.take(35),
            text = cleanText,
        )
        try {
            commentRef.set(comment.toCreateMap()).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "realScorePoints" to FieldValue.increment(3L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
        commentRef.id
    }

    // --- 5. WhatsApp-Style Real-Time Messaging ---

    fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun getOrCreateChatWithPeer(
        myProfile: CreatorProfile,
        peerProfile: CreatorProfile,
    ): Result<ChatThread> = runCatching {
        val uid = requireUserId()
        val participants = listOf(uid, peerProfile.userId).distinct()
        val chatsPath = "chats"

        val existingSnap = try {
            db.collection("chats")
                .whereArrayContains("participantIds", uid)
                .get()
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, chatsPath)
            throw e
        }

        val existingChats = existingSnap.toObjects(
            ChatThread::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val matched = existingChats.firstOrNull { it.participantIds.toSet() == participants.toSet() }
        if (matched != null) return@runCatching matched

        val isSelfChat = peerProfile.userId == uid
        val docRef = db.collection("chats").document()
        val newChat = ChatThread(
            initiatorId = uid,
            participantIds = participants,
            chatTitle = if (isSelfChat) "Saved Studio Notes" else peerProfile.displayName.ifBlank { "Chat" }.take(80),
            peerId = peerProfile.userId,
            peerName = if (isSelfChat) "${myProfile.displayName} (You)" else peerProfile.displayName.ifBlank { "Creator" }.take(60),
            peerHandle = peerProfile.handle.ifBlank { "@creator" }.take(35),
            lastMessage = "Conversation started",
            lastSenderId = uid,
            messageCount = 0L,
        )
        try {
            docRef.set(newChat.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        newChat.copy(id = docRef.id)
    }

    fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun sendMessage(
        chat: ChatThread,
        senderName: String,
        text: String,
        messageType: String = "text",
        sharedPostId: String = "",
        thumbnailBase64: String? = null,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanText = text.trim().ifBlank {
            if (messageType == "photo") "Photo attachment" else "Shared a post"
        }.take(1000)
        val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
        val msg = ChatMessage(
            chatId = chat.id,
            senderId = uid,
            senderName = senderName.ifBlank { "User" }.take(60),
            participantIds = chat.participantIds,
            text = cleanText,
            messageType = if (messageType in listOf("text", "photo", "shared_post", "call_event")) messageType else "text",
            sharedPostId = sharedPostId.take(128),
            thumbnailBase64 = thumbnailBase64?.take(60000),
        )
        try {
            msgRef.set(msg.toCreateMap()).await()
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "lastMessage" to cleanText.take(300),
                    "lastSenderId" to uid,
                    "messageCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
        msgRef.id
    }

    // --- 6. Real-Time Voice ("Phone") & Video Calling ---

    fun observeCalls(userId: String): Flow<List<CallSession>> = flow {
        val path = "calls"
        emitAll(
            db.collection("calls")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeCallSession(callId: String): Flow<CallSession?> = flow {
        val path = "calls/$callId"
        emitAll(
            db.collection("calls").document(callId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                    throw e
                }
        )
    }

    suspend fun startCall(
        callerProfile: CreatorProfile,
        receiverProfile: CreatorProfile,
        callType: String,
    ): Result<CallSession> = runCatching {
        val uid = requireUserId()
        val validType = if (callType == "audio") "audio" else "video"
        val participants = listOf(uid, receiverProfile.userId).distinct()
        val docRef = db.collection("calls").document()
        val call = CallSession(
            callerId = uid,
            callerName = callerProfile.displayName.ifBlank { "Caller" }.take(60),
            receiverId = receiverProfile.userId,
            receiverName = receiverProfile.displayName.ifBlank { "Receiver" }.take(60),
            participantIds = participants,
            callType = validType,
            status = "ringing",
            durationSec = 0L,
            isMuted = false,
            isCameraEnabled = validType == "video",
        )
        try {
            docRef.set(call.toCreateMap()).await()
            if (participants.size == 1) {
                // Self test-call connects immediately so the user can test live camera/mic on emulator
                docRef.update(
                    mapOf(
                        "status" to "connected",
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                return@runCatching call.copy(id = docRef.id, status = "connected")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        call.copy(id = docRef.id)
    }

    suspend fun answerCall(callId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to "connected",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCallMediaState(
        callId: String,
        isMuted: Boolean,
        isCameraEnabled: Boolean,
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "isMuted" to isMuted,
                    "isCameraEnabled" to isCameraEnabled,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun endOrDeclineCall(
        callId: String,
        terminalStatus: String,
        durationSec: Long,
    ): Result<Unit> = runCatching {
        requireUserId()
        val status = if (terminalStatus == "declined") "declined" else "ended"
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "durationSec" to durationSec.coerceIn(0L, 86400L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
