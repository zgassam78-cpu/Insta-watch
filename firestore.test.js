const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const EVE_UID = "eve_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function serverTs() {
  return new Date(Date.now() - 1000);
}

async function seedProfile(uid, displayName = "Creator") {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("profiles").doc(uid).set({
      userId: uid,
      displayName,
      handle: `@${uid}`,
      bio: "Video & Reel creator",
      creatorCategory: "Entertainment",
      avatarUrl: "",
      isDiscoverable: true,
      realScore: 100,
      postsCount: 0,
      followersCount: 0,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    });
  });
}

// --- 1. Unauthenticated Rejections ---

test("Unauthenticated user cannot read public posts or profiles", async () => {
  await seedProfile(ALICE_UID);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("profiles").where("isDiscoverable", "==", true).get());
});

// --- 2. PII Split Isolation (/users vs /profiles) ---

test("Private /users/{uid} PII document is strictly isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phone: "+919876543210",
      notificationsEnabled: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Public /profiles/{uid} rejects PII injection (email/phone)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      bio: "Hi",
      creatorCategory: "Tech",
      avatarUrl: "",
      isDiscoverable: true,
      realScore: 0,
      postsCount: 0,
      followersCount: 0,
      email: "leak@example.com",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

// --- 3. MediaPost Creation, Orphan Protection, Shadow Update & Query Alignment ---

test("Orphaned post creation without registered CreatorProfile fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "@alice",
      authorAvatarUrl: "",
      mediaType: "reel",
      title: "My First Reel",
      caption: "Dancing under neon lights",
      category: "Entertainment",
      mediaUrl: "https://example.com/reel.mp4",
      durationSec: 25,
      audioTrack: "Original Audio",
      locationTag: "Mumbai",
      tags: ["#reels"],
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      realScorePoints: 10,
      likedByUids: [],
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

test("Registered creator can publish photo/reel/long_video, query public posts, and reject private post leaks", async () => {
  await seedProfile(ALICE_UID, "Alice");
  await seedProfile(BOB_UID, "Bob");

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("public_watch_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "@alice_123",
      authorAvatarUrl: "",
      mediaType: "long_video",
      title: "Cinema Travel Vlog",
      caption: "Full 4K mountain documentary",
      category: "Vlog",
      mediaUrl: "https://example.com/vlog.mp4",
      durationSec: 620,
      audioTrack: "Ambient Score",
      locationTag: "Manali",
      tags: ["#watch", "#vlog"],
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      realScorePoints: 25,
      likedByUids: [],
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("posts").doc("private_draft_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "@alice_123",
      authorAvatarUrl: "",
      mediaType: "photo",
      title: "Secret Draft",
      caption: "Private notes",
      category: "Lifestyle",
      mediaUrl: "",
      durationSec: 0,
      audioTrack: "",
      locationTag: "",
      tags: [],
      visibility: "private",
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      realScorePoints: 0,
      likedByUids: [],
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Aligned public query succeeds
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  // Unfiltered query fails (rules are not filters!)
  await assertFails(bobDb.collection("posts").get());
  // Private post read by non-owner fails
  await assertFails(bobDb.collection("posts").doc("private_draft_1").get());

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("posts").doc("public_watch_1").update({
      title: "Updated Title",
      isVerified: true,
      updatedAt: serverTs(),
    })
  );

  // Community like / Real Score update succeeds within strict schema bounds
  await assertSucceeds(
    bobDb.collection("posts").doc("public_watch_1").update({
      likesCount: 1,
      realScorePoints: 30,
      likedByUids: [BOB_UID],
      updatedAt: serverTs(),
    })
  );

  // Value poisoning (negative likesCount) fails
  await assertFails(
    bobDb.collection("posts").doc("public_watch_1").update({
      likesCount: -5,
      updatedAt: serverTs(),
    })
  );
});

// --- 4. WhatsApp-Style Chat & Message Participant Poisoning Guard ---

test("Chat threads and messages isolate participants and block participant poisoning", async () => {
  await seedProfile(ALICE_UID, "Alice");
  await seedProfile(BOB_UID, "Bob");

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").set({
      initiatorId: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      chatTitle: "Bob",
      peerId: BOB_UID,
      peerName: "Bob",
      peerHandle: "@bob_456",
      lastMessage: "Hey!",
      lastSenderId: ALICE_UID,
      messageCount: 1,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Valid message matching parent participantIds succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").set({
      chatId: "chat_ab",
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      text: "Namaste Bob!",
      messageType: "text",
      sharedPostId: "",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Poisoning message participantIds with EVE_UID fails
  await assertFails(
    aliceDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_poison").set({
      chatId: "chat_ab",
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID, EVE_UID],
      text: "Leaked message",
      messageType: "text",
      sharedPostId: "",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Eve cannot read chat_ab or list Alice/Bob messages
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  await assertFails(eveDb.collection("chats").doc("chat_ab").get());
  await assertFails(eveDb.collection("chats").doc("chat_ab").collection("messages").get());
  await assertFails(
    eveDb.collection("chats").doc("chat_ab").collection("messages").where("participantIds", "array-contains", ALICE_UID).get()
  );

  // Bob can query chats and messages with array-contains
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("chats").where("participantIds", "array-contains", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_ab").collection("messages").where("participantIds", "array-contains", BOB_UID).get()
  );
});

// --- 5. Voice & Video Call Terminal State Locking ---

test("CallSession enforces terminal state locking once ended", async () => {
  await seedProfile(ALICE_UID, "Alice");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice",
      receiverId: BOB_UID,
      receiverName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      callType: "video",
      status: "ringing",
      durationSec: 0,
      isMuted: false,
      isCameraEnabled: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Bob answers call -> status: connected
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "connected",
      updatedAt: serverTs(),
    })
  );

  // Alice ends call -> status: ended
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").update({
      status: "ended",
      durationSec: 42,
      updatedAt: serverTs(),
    })
  );

  // Attempting to resurrect an ended call MUST fail
  await assertFails(
    bobDb.collection("calls").doc("call_1").update({
      status: "connected",
      updatedAt: serverTs(),
    })
  );
});
