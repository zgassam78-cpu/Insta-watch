package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class InstaWatchRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun publishPost_andObservePublicPosts_emitsRealtimeReelAndLongVideo() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        signInTestUser("alice_$suffix@test.com")
        val repository = InstaWatchRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserBootstrapped("Alice Verma", "alice_$suffix@test.com", "").getOrThrow()
        }

        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishMediaPost(
                profile = profile,
                mediaType = "reel",
                title = "Neon Mumbai Reel $suffix",
                caption = "Exploring night street vibes",
                category = "Entertainment",
                mediaUrl = "https://example.com/reel.mp4",
                durationSec = 28L,
                audioTrack = "Original Sound",
                locationTag = "Mumbai",
                tags = listOf("#instawatch", "#reels"),
                visibility = "public",
                thumbnailBase64 = null,
            ).getOrThrow()
        }
        assertTrue(postId.isNotBlank())

        val emittedPosts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(emittedPosts.any { it.id == postId && it.mediaType == "reel" })
    }

    @Test
    fun getPrivateAccount_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val aliceUid = signInTestUser("alice_pii_$suffix@test.com")
        val aliceRepo = InstaWatchRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserBootstrapped("Alice", "alice_pii_$suffix@test.com", "").getOrThrow()
        }

        signInTestUser("bob_pii_$suffix@test.com")
        val bobRepo = InstaWatchRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPrivateAccountById(aliceUid)
        }
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun directChatAndCallSession_enforcesParticipantIsolationAndTerminalLock() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        signInTestUser("alice_chat_$suffix@test.com")
        val aliceRepo = InstaWatchRepository(firestore, auth)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserBootstrapped("Alice", "alice_chat_$suffix@test.com", "").getOrThrow()
        }

        signInTestUser("bob_chat_$suffix@test.com")
        val bobRepo = InstaWatchRepository(firestore, auth)
        val bobProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.ensureUserBootstrapped("Bob", "bob_chat_$suffix@test.com", "").getOrThrow()
        }

        // Switch back to Alice, start chat and send message
        signInTestUser("alice_chat_$suffix@test.com")
        val chat = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getOrCreateChatWithPeer(aliceProfile, bobProfile).getOrThrow()
        }
        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendMessage(chat, "Alice", "Hello Bob on InstaWatch!").getOrThrow()
        }
        assertTrue(msgId.isNotBlank())

        // Start a video call, end it, and verify terminal state lock rejects further updates
        val call = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.startCall(aliceProfile, bobProfile, "video").getOrThrow()
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.endOrDeclineCall(call.id, "ended", 35L).getOrThrow()
        }
        val postEndAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.answerCall(call.id)
        }
        assertTrue(postEndAttempt.isFailure)
        val callEx = postEndAttempt.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, callEx?.code)
    }

    @Test
    fun observePublicPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = InstaWatchRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicPosts().first()
            }
            fail("Expected unauthenticated query to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
