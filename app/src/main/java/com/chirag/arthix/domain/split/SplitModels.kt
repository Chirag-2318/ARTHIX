package com.chirag.arthix.domain.split

// Mock contracts for Phase 4 (Voice Split Intent)
data class VoiceSplitIntent(
    val rawTranscript: String,
    val recognizedParticipants: List<RecognizedParticipant>,
    val unresolvedNames: List<String>,
    val proportionIntentDetected: Boolean,
    val sttConfidence: Float
)

data class RecognizedParticipant(
    val spokenName: String,
    val matches: List<ContactCandidate>
)

data class ContactCandidate(
    val contactId: String,
    val displayName: String,
    val disambiguatingDetail: String
)

// Mock contracts for Phase 5 (Group Suggestion)
data class ContactRef(val contactId: String, val displayName: String)

sealed class GroupSuggestionResult
data class SuggestedGroup(val groupId: String, val participants: List<ContactRef>) : GroupSuggestionResult()
object ColdStart : GroupSuggestionResult()
