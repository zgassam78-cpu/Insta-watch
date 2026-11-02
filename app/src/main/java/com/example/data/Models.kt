package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phone: String? = null,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "email" to email,
            "phone" to phone?.takeIf { it.isNotBlank() },
            "notificationsEnabled" to notificationsEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return raw.filterValues { it != null }.mapValues { it.value as Any }
    }
}

@IgnoreExtraProperties
data class CreatorProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val creatorCategory: String = "Entertainment",
    val avatarUrl: String = "",
    @get:PropertyName("isDiscoverable")
    @set:PropertyName("isDiscoverable")
    var isDiscoverable: Boolean = true,
    val realScore: Long = 100L,
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "handle" to handle,
        "bio" to bio,
        "creatorCategory" to creatorCategory,
        "avatarUrl" to avatarUrl,
        "isDiscoverable" to isDiscoverable,
        "realScore" to realScore,
        "postsCount" to postsCount,
        "followersCount" to followersCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class StoryItem(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarUrl: String = "",
    val caption: String = "",
    val accentColorKey: String = "coral",
    val visibility: String = "public",
    val viewsCount: Long = 0L,
    val thumbnailBase64: String? = null,
    val localMediaUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorHandle" to authorHandle,
            "authorAvatarUrl" to authorAvatarUrl,
            "caption" to caption,
            "accentColorKey" to accentColorKey,
            "visibility" to visibility,
            "viewsCount" to viewsCount,
            "thumbnailBase64" to thumbnailBase64?.takeIf { it.isNotBlank() },
            "localMediaUri" to localMediaUri?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return raw.filterValues { it != null }.mapValues { it.value as Any }
    }
}

@IgnoreExtraProperties
data class MediaPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarUrl: String = "",
    val mediaType: String = "photo", // "photo" | "reel" | "long_video"
    val title: String = "",
    val caption: String = "",
    val category: String = "Entertainment",
    val mediaUrl: String = "",
    val durationSec: Long = 0L,
    val audioTrack: String = "",
    val locationTag: String = "",
    val tags: List<String> = emptyList(),
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val realScorePoints: Long = 10L,
    val likedByUids: List<String> = emptyList(),
    val thumbnailBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorHandle" to authorHandle,
            "authorAvatarUrl" to authorAvatarUrl,
            "mediaType" to mediaType,
            "title" to title,
            "caption" to caption,
            "category" to category,
            "mediaUrl" to mediaUrl,
            "durationSec" to durationSec,
            "audioTrack" to audioTrack,
            "locationTag" to locationTag,
            "tags" to tags.take(8),
            "visibility" to visibility,
            "likesCount" to likesCount,
            "commentsCount" to commentsCount,
            "viewsCount" to viewsCount,
            "realScorePoints" to realScorePoints,
            "likedByUids" to likedByUids.take(10),
            "thumbnailBase64" to thumbnailBase64?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return raw.filterValues { it != null }.mapValues { it.value as Any }
    }
}

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "postId" to postId,
        "authorId" to authorId,
        "authorName" to authorName,
        "authorHandle" to authorHandle,
        "text" to text,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val initiatorId: String = "",
    val participantIds: List<String> = emptyList(),
    val chatTitle: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val peerHandle: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "initiatorId" to initiatorId,
        "participantIds" to participantIds.distinct().take(10),
        "chatTitle" to chatTitle,
        "peerId" to peerId,
        "peerName" to peerName,
        "peerHandle" to peerHandle,
        "lastMessage" to lastMessage,
        "lastSenderId" to lastSenderId,
        "messageCount" to messageCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = "text", // "text" | "photo" | "shared_post" | "call_event"
    val sharedPostId: String = "",
    val thumbnailBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "chatId" to chatId,
            "senderId" to senderId,
            "senderName" to senderName,
            "participantIds" to participantIds.distinct().take(10),
            "text" to text,
            "messageType" to messageType,
            "sharedPostId" to sharedPostId,
            "thumbnailBase64" to thumbnailBase64?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return raw.filterValues { it != null }.mapValues { it.value as Any }
    }
}

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val participantIds: List<String> = emptyList(),
    val callType: String = "video", // "audio" | "video"
    val status: String = "ringing", // "ringing" | "connected" | "ended" | "declined"
    val durationSec: Long = 0L,
    @get:PropertyName("isMuted")
    @set:PropertyName("isMuted")
    var isMuted: Boolean = false,
    @get:PropertyName("isCameraEnabled")
    @set:PropertyName("isCameraEnabled")
    var isCameraEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "callerId" to callerId,
        "callerName" to callerName,
        "receiverId" to receiverId,
        "receiverName" to receiverName,
        "participantIds" to participantIds.distinct().take(2),
        "callType" to callType,
        "status" to status,
        "durationSec" to durationSec,
        "isMuted" to isMuted,
        "isCameraEnabled" to isCameraEnabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
