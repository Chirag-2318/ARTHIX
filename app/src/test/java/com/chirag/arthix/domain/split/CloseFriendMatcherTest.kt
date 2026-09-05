package com.chirag.arthix.domain.split

import com.chirag.arthix.data.entity.CloseFriendEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class CloseFriendMatcherTest {

    private lateinit var matcher: CloseFriendMatcher

    private val closeFriends = listOf(
        CloseFriendEntity(
            id = 1L,
            name = "Ojas",
            phoneNumber = "+919876543210",
            aliases = listOf("Oj", "Oji")
        ),
        CloseFriendEntity(
            id = 2L,
            name = "Neeru",
            phoneNumber = "+919876543211",
            aliases = listOf("Niru")
        ),
        CloseFriendEntity(
            id = 3L,
            name = "Sneha",
            phoneNumber = "+919876543212",
            aliases = emptyList()
        ),
        CloseFriendEntity(
            id = 4L,
            name = "Priya Sharma",
            phoneNumber = "+919876543213",
            aliases = listOf("Priya", "Pree")
        )
    )

    @Before
    fun setUp() {
        matcher = CloseFriendMatcher()
    }

    @Test
    fun match_exactName_returnsExactNameQuality() {
        val result = matcher.match("Ojas", closeFriends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Ojas")
        assertThat(result.quality).isEqualTo(MatchQuality.EXACT_NAME)
        assertThat(result.friend.phoneNumber).isEqualTo("+919876543210")
    }

    @Test
    fun match_exactName_caseInsensitive() {
        val result = matcher.match("sneha", closeFriends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Sneha")
        assertThat(result.quality).isEqualTo(MatchQuality.EXACT_NAME)
    }

    @Test
    fun match_exactAlias_returnsExactAliasQuality() {
        val result = matcher.match("Oj", closeFriends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Ojas")
        assertThat(result.matchedTerm).isEqualTo("Oj")
        assertThat(result.quality).isEqualTo(MatchQuality.EXACT_ALIAS)
    }

    @Test
    fun match_indianPhonetic_matchesNeeruToNiruWithoutAlias() {
        val friendsWithNiru = listOf(
            CloseFriendEntity(id = 2L, name = "Niru", phoneNumber = "+919876543211", aliases = emptyList())
        )
        val result = matcher.match("neeru", friendsWithNiru)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Niru")
        assertThat(result.quality).isEqualTo(MatchQuality.PHONETIC_NORMALIZED)
        assertThat(result.friend.phoneNumber).isEqualTo("+919876543211")
    }

    @Test
    fun match_actionWordsStripped_matchesNeeruLoggedToNiru() {
        val friendsWithNiru = listOf(
            CloseFriendEntity(id = 2L, name = "Niru", phoneNumber = "+919876543211", aliases = emptyList())
        )
        val result = matcher.match("neeru logged", friendsWithNiru)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Niru")
        assertThat(result.friend.phoneNumber).isEqualTo("+919876543211")
    }

    @Test
    fun match_indianPhonetic_matchesPoojaToPuja() {
        val friends = listOf(
            CloseFriendEntity(id = 10L, name = "Puja", phoneNumber = "+919876543299", aliases = emptyList())
        )
        val result = matcher.match("pooja", friends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Puja")
        assertThat(result.quality).isEqualTo(MatchQuality.PHONETIC_NORMALIZED)
    }

    @Test
    fun match_indianPhonetic_matchesAmmanToAman() {
        val friends = listOf(
            CloseFriendEntity(id = 11L, name = "Aman", phoneNumber = "+919876543298", aliases = emptyList())
        )
        val result = matcher.match("amman", friends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Aman")
        assertThat(result.quality).isEqualTo(MatchQuality.PHONETIC_NORMALIZED)
    }

    @Test
    fun match_indianPhonetic_matchesVikasToWikas() {
        val friends = listOf(
            CloseFriendEntity(id = 12L, name = "Wikas", phoneNumber = "+919876543297", aliases = emptyList())
        )
        val result = matcher.match("vikas", friends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Wikas")
        assertThat(result.quality).isEqualTo(MatchQuality.PHONETIC_NORMALIZED)
    }

    @Test
    fun match_soundexPhonetic_matchesNiruToNeeru() {
        // When phonetic normalization is run, "Niru" and "Neeru" normalize to "niru"
        val friendsWithoutNiruAlias = listOf(
            CloseFriendEntity(id = 2L, name = "Neeru", phoneNumber = "+919876543211", aliases = emptyList())
        )
        val result = matcher.match("Niru", friendsWithoutNiruAlias)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Neeru")
        assertThat(result.quality).isEqualTo(MatchQuality.PHONETIC_NORMALIZED)
    }

    @Test
    fun match_levenshteinDistance_matchesTypoWithinDistanceTwo() {
        // "Snehaa" has distance 1 from "Sneha"
        val result = matcher.match("Snehaa", closeFriends)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Sneha")
    }

    @Test
    fun match_unknownName_returnsNull() {
        val result = matcher.match("Zack", closeFriends)
        assertThat(result).isNull()
    }

    @Test
    fun match_emptyCandidate_returnsNull() {
        assertThat(matcher.match("", closeFriends)).isNull()
        assertThat(matcher.match("   ", closeFriends)).isNull()
    }

    @Test
    fun normalizePhonetic_computesAccurateNormalizations() {
        assertThat(matcher.normalizePhonetic("neeru")).isEqualTo(matcher.normalizePhonetic("niru"))
        assertThat(matcher.normalizePhonetic("pooja")).isEqualTo(matcher.normalizePhonetic("puja"))
        assertThat(matcher.normalizePhonetic("amman")).isEqualTo(matcher.normalizePhonetic("aman"))
        assertThat(matcher.normalizePhonetic("vikas")).isEqualTo(matcher.normalizePhonetic("wikas"))
    }

    @Test
    fun soundex_computesAccurateCodes() {
        assertThat(matcher.soundex("Neeru")).isEqualTo("N600")
        assertThat(matcher.soundex("Niru")).isEqualTo("N600")
        assertThat(matcher.soundex("Chirag")).isEqualTo("C620")
    }

    @Test
    fun levenshteinDistance_computesAccurateDistance() {
        assertThat(matcher.levenshteinDistance("kitten", "sitting")).isEqualTo(3)
        assertThat(matcher.levenshteinDistance("Ojas", "Oja")).isEqualTo(1)
        assertThat(matcher.levenshteinDistance("Priya", "Priya")).isEqualTo(0)
    }
}
