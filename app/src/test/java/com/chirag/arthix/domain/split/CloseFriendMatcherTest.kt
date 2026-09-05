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
    fun match_soundexPhonetic_matchesNiruToNeeru() {
        // "Niru" soundex is N600, "Neeru" soundex is N600
        val friendsWithoutNiruAlias = listOf(
            CloseFriendEntity(id = 2L, name = "Neeru", phoneNumber = "+919876543211", aliases = emptyList())
        )
        val result = matcher.match("Niru", friendsWithoutNiruAlias)
        assertThat(result).isNotNull()
        assertThat(result!!.friend.name).isEqualTo("Neeru")
        assertThat(result.quality).isEqualTo(MatchQuality.EXACT_PHONETIC)
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
