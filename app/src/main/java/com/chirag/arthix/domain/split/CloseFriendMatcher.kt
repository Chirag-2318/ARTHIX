package com.chirag.arthix.domain.split

import com.chirag.arthix.data.entity.CloseFriendEntity
import javax.inject.Inject
import javax.inject.Singleton

enum class MatchQuality {
    EXACT_NAME,
    EXACT_ALIAS,
    PHONETIC_NORMALIZED,
    EXACT_PHONETIC,
    FUZZY_EDIT_DISTANCE
}

data class FriendMatchResult(
    val friend: CloseFriendEntity,
    val matchedTerm: String,
    val quality: MatchQuality,
    val spokenCandidate: String
)

@Singleton
class CloseFriendMatcher @Inject constructor() {

    private val ACTION_STOP_WORDS = setOf(
        "logged", "log", "logging", "record", "recorded", "recording",
        "add", "added", "adding", "split", "splitting", "divide", "dividing",
        "share", "sharing", "bill", "expense", "transaction",
        "for", "to", "with", "from", "at", "on", "and"
    )

    /**
     * Attempts to resolve a spoken or typed name against a list of saved [CloseFriendEntity]s.
     * Evaluates in order:
     * 1. Exact Name match
     * 2. Indian English Phonetic Normalization match ("neeru" <-> "niru", "pooja" <-> "puja")
     * 3. Exact Alias match
     * 4. Phonetic Soundex match
     * 5. Action word stripping fallback (e.g. "neeru logged" -> "neeru" -> matched to "Niru")
     * 6. Levenshtein edit distance fallback (distance <= 2)
     */
    fun match(candidate: String, friends: List<CloseFriendEntity>): FriendMatchResult? {
        val trimmed = candidate.trim()
        if (trimmed.isBlank() || friends.isEmpty()) return null

        // 1. Direct match on full candidate
        val directMatch = matchSingleCandidate(trimmed, friends)
        if (directMatch != null) return directMatch

        // 2. Action word stripping fallback (e.g. "neeru logged" -> "neeru")
        val strippedTokens = trimmed.split(Regex("\\s+"))
            .filter { it.lowercase() !in ACTION_STOP_WORDS && it.isNotBlank() }

        if (strippedTokens.isNotEmpty()) {
            val strippedCandidate = strippedTokens.joinToString(" ").trim()
            if (strippedCandidate.isNotBlank() && !strippedCandidate.equals(trimmed, ignoreCase = true)) {
                val strippedMatch = matchSingleCandidate(strippedCandidate, friends)
                if (strippedMatch != null) {
                    return strippedMatch.copy(spokenCandidate = trimmed)
                }
            }

            // Also check individual tokens if candidate has multiple words
            if (strippedTokens.size > 1) {
                for (token in strippedTokens) {
                    val tokenMatch = matchSingleCandidate(token, friends)
                    if (tokenMatch != null) {
                        return tokenMatch.copy(spokenCandidate = trimmed)
                    }
                }
            }
        }

        return null
    }

    private fun matchSingleCandidate(candidate: String, friends: List<CloseFriendEntity>): FriendMatchResult? {
        val trimmed = candidate.trim()
        if (trimmed.isBlank() || friends.isEmpty()) return null
        val lowerCandidate = trimmed.lowercase()

        // 1. Exact Name match
        for (friend in friends) {
            if (friend.name.trim().equals(trimmed, ignoreCase = true)) {
                return FriendMatchResult(friend, friend.name, MatchQuality.EXACT_NAME, trimmed)
            }
        }

        // 2. Exact Alias match
        for (friend in friends) {
            for (alias in friend.aliases) {
                if (alias.trim().equals(trimmed, ignoreCase = true)) {
                    return FriendMatchResult(friend, alias, MatchQuality.EXACT_ALIAS, trimmed)
                }
            }
        }

        // 3. Indian English Phonetic Normalization match ("neeru" <-> "niru", "pooja" <-> "puja", "amman" <-> "aman")
        val candPhonetic = normalizePhonetic(trimmed)
        if (candPhonetic.isNotEmpty()) {
            for (friend in friends) {
                if (normalizePhonetic(friend.name) == candPhonetic) {
                    return FriendMatchResult(friend, friend.name, MatchQuality.PHONETIC_NORMALIZED, trimmed)
                }
                for (alias in friend.aliases) {
                    if (normalizePhonetic(alias) == candPhonetic) {
                        return FriendMatchResult(friend, alias, MatchQuality.PHONETIC_NORMALIZED, trimmed)
                    }
                }
            }
        }

        // 4. Phonetic Soundex match
        val candidateSoundex = soundex(trimmed)
        if (candidateSoundex.isNotEmpty()) {
            for (friend in friends) {
                if (soundex(friend.name) == candidateSoundex) {
                    return FriendMatchResult(friend, friend.name, MatchQuality.EXACT_PHONETIC, trimmed)
                }
                for (alias in friend.aliases) {
                    if (soundex(alias) == candidateSoundex) {
                        return FriendMatchResult(friend, alias, MatchQuality.EXACT_PHONETIC, trimmed)
                    }
                }
            }
        }

        // 5. Levenshtein distance fallback (distance <= 2 for words of length >= 3)
        if (lowerCandidate.length >= 3) {
            var bestMatch: FriendMatchResult? = null
            var lowestDistance = Int.MAX_VALUE

            for (friend in friends) {
                val distName = levenshteinDistance(lowerCandidate, friend.name.trim().lowercase())
                if (distName in 1..2 && distName < lowestDistance) {
                    lowestDistance = distName
                    bestMatch = FriendMatchResult(friend, friend.name, MatchQuality.FUZZY_EDIT_DISTANCE, trimmed)
                }

                for (alias in friend.aliases) {
                    val distAlias = levenshteinDistance(lowerCandidate, alias.trim().lowercase())
                    if (distAlias in 1..2 && distAlias < lowestDistance) {
                        lowestDistance = distAlias
                        bestMatch = FriendMatchResult(friend, alias, MatchQuality.FUZZY_EDIT_DISTANCE, trimmed)
                    }
                }
            }

            if (bestMatch != null) {
                return bestMatch
            }
        }

        return null
    }

    /**
     * Normalizes Indian English phonetic variations frequently output by speech-to-text engines:
     * - "ee" <-> "i" (Neeru <-> Niru, Geeta <-> Gita)
     * - "oo" <-> "u" (Pooja <-> Puja, Anoop <-> Anup)
     * - "aa" <-> "a" (Chiraag <-> Chirag)
     * - "w" <-> "v" (Wikas <-> Vikas)
     * - "ph" <-> "f" (Pharhan <-> Farhan)
     * - Collapses double consonants ("mm" -> "m", "rr" -> "r", "tt" -> "t", etc.)
     */
    fun normalizePhonetic(input: String): String {
        var s = input.trim().lowercase().filter { it in 'a'..'z' }
        if (s.isEmpty()) return ""

        s = s.replace("ee", "i")
            .replace("oo", "u")
            .replace("ou", "u")
            .replace("ow", "au")
            .replace("aa", "a")
            .replace("ph", "f")
            .replace("bh", "b")
            .replace("dh", "d")
            .replace("th", "t")
            .replace("kh", "k")
            .replace("gh", "g")
            .replace("ch", "c")
            .replace("sh", "s")
            .replace("zh", "z")
            .replace("w", "v")

        val sb = StringBuilder()
        for (c in s) {
            if (sb.isEmpty() || sb.last() != c) {
                sb.append(c)
            }
        }
        var res = sb.toString()
        if (res.endsWith("th")) res = res.dropLast(1)
        return res
    }

    /**
     * Computes the standard Soundex code for a word.
     * E.g. "Neeru" -> "N600", "Niru" -> "N600"
     */
    fun soundex(input: String): String {
        val clean = input.trim().uppercase().filter { it in 'A'..'Z' }
        if (clean.isEmpty()) return ""

        val firstLetter = clean[0]
        val encoded = StringBuilder().append(firstLetter)

        fun getCode(c: Char): Char {
            return when (c) {
                'B', 'F', 'P', 'V' -> '1'
                'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
                'D', 'T' -> '3'
                'L' -> '4'
                'M', 'N' -> '5'
                'R' -> '6'
                else -> '0'
            }
        }

        var prevCode = getCode(firstLetter)
        for (i in 1 until clean.length) {
            val code = getCode(clean[i])
            if (code != '0' && code != prevCode) {
                encoded.append(code)
            }
            prevCode = code
        }

        // Pad with 0s or truncate to length 4
        while (encoded.length < 4) {
            encoded.append('0')
        }
        return encoded.substring(0, 4)
    }

    /**
     * Computes Levenshtein edit distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) {
                    prev
                } else {
                    1 + minOf(prev, dp[j], dp[j - 1])
                }
                prev = temp
            }
        }
        return dp[b.length]
    }
}
