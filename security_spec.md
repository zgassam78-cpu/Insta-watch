# InstaWatch Security Specification & Red Team Audit

## 1. Core Data Invariants
1. **PII Split Isolation (`/users/{userId}` vs `/profiles/{userId}`)**: Email and phone numbers reside exclusively in `/users/{userId}`, readable and writable solely when `request.auth.uid == userId`. `/profiles/{userId}` forbids PII keys via `hasOnly`.
2. **Global Relational Existence**: Creating a `StoryItem`, `MediaPost`, `ChatThread`, or `CallSession` requires `exists(/databases/$(database)/documents/profiles/$(request.auth.uid))` so orphaned records from unregistered identities cannot exist.
3. **Subcollection Parent Gates**:
   - `posts/{postId}/comments/{commentId}` verifies parent post existence and visibility (`public` or owned by caller) on `create`, and binds `incoming().postId == postId`.
   - `chats/{chatId}/messages/{messageId}` verifies parent chat existence, verifies `request.auth.uid` is in parent `participantIds`, and enforces `incoming().participantIds == parent.participantIds` to block participant-list poisoning.
4. **Query Enforcer (Zero Blanket Reads)**: Every `allow list` rule evaluates `existing()` fields (`userId`, `isDiscoverable`, `visibility`, `postId`, or `participantIds`) with zero `get()`/`exists()` calls inside `list`.
5. **Terminal State Locking**: Once a `CallSession` reaches `'ended'` or `'declined'`, all subsequent updates are rejected.
6. **Temporal Integrity**: All `createdAt` and `updatedAt` fields require `is timestamp` and `<= request.time`, and `createdAt` is immutable on updates.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Feed Scrape**: `unauthDb.collection("posts").where("visibility", "==", "public").get()` -> Rejected (`isSignedIn()`).
2. **Cross-User PII Leak**: Alice reads `/users/bob_456` -> Rejected (`request.auth.uid == userId`).
3. **PII Injection into Public Profile**: Alice creates `/profiles/alice_123` with `"email": "alice@example.com"` -> Rejected (`keys().hasOnly(...)`).
4. **Identity Spoofing on Post Creation**: Alice creates `/posts/post_1` with `"authorId": "bob_456"` -> Rejected (`incoming().authorId == request.auth.uid`).
5. **Orphaned Post Without Profile**: User without `/profiles/{uid}` creates `/posts/post_1` -> Rejected (`exists(.../profiles/$(request.auth.uid))`).
6. **Shadow Update / Ghost Field Injection**: Alice updates `/posts/post_1` adding `"isVerified": true` -> Rejected (`affectedKeys().hasOnly(...)`).
7. **Value Poisoning on Engagement Update**: Bob updates `/posts/post_1` setting `"likesCount": -50` or `"likesCount": "million"` -> Rejected (`isValidMediaPost(incoming())`).
8. **Private Post Leakage**: Bob reads or queries Alice's post where `visibility == 'private'` -> Rejected (`existing().visibility == 'public' || existing().authorId == request.auth.uid`).
9. **Unfiltered Collection Scan**: Alice calls `db.collection("posts").get()` without `.where("visibility", "==", "public")` -> Rejected (rules are not filters).
10. **Cross-User Chat Snooping**: Eve queries `chats` or `chats/{chatId}/messages` where `eve` is not in `participantIds` -> Rejected (`request.auth.uid in existing().participantIds`).
11. **Message Participant Poisoning**: Alice creates a message in `chats/ab_chat` (`participantIds: ['alice', 'bob']`) with `message.participantIds: ['alice', 'bob', 'eve']` -> Rejected (`incoming().participantIds == parentChat.participantIds`).
12. **Terminal Call Resurrection**: Caller attempts to update `calls/call_1` from `status: 'ended'` back to `'connected'` -> Rejected (`!(existing().status in ['ended', 'declined'])`).
