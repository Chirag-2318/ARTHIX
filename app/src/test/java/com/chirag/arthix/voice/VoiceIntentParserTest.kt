package com.chirag.arthix.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [VoiceIntentParser].
 *
 * All pure JVM — zero Android framework dependencies.
 * Covers Discard intent (EC-25), Category resolution with fuzzy matching (EC-28),
 * Amount intent (EC-24), Combined CategoryAndAmount, Split intent (FR-6), and Unclear intent.
 */
class VoiceIntentParserTest {

    // ── 1. Discard intent tests (EC-25) ────────────────────────────────────────

    @Test
    fun `discard keywords - recognized as Discard intent`() {
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("skip"))
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("not real"))
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("ignore that one"))
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("cancel"))
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("discard"))
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("nope"))
        assertEquals(VoiceIntent.Discard, VoiceIntentParser.parse("no"))
    }

    // ── 2. Category intent tests (EC-28) ───────────────────────────────────────

    @Test
    fun `exact category words - recognized as Category`() {
        val result = VoiceIntentParser.parse("food")
        assertTrue(result is VoiceIntent.Category)
        assertEquals("Food", (result as VoiceIntent.Category).category)

        val travel = VoiceIntentParser.parse("travel")
        assertTrue(travel is VoiceIntent.Category)
        assertEquals("Travel", (travel as VoiceIntent.Category).category)
    }

    @Test
    fun `synonyms mapped to canonical category`() {
        val restaurant = VoiceIntentParser.parse("restaurant")
        assertTrue(restaurant is VoiceIntent.Category)
        assertEquals("Food", (restaurant as VoiceIntent.Category).category)

        val cab = VoiceIntentParser.parse("cab")
        assertTrue(cab is VoiceIntent.Category)
        assertEquals("Travel", (cab as VoiceIntent.Category).category)

        val amazon = VoiceIntentParser.parse("amazon shopping")
        assertTrue(amazon is VoiceIntent.Category)
        assertEquals("Shopping", (amazon as VoiceIntent.Category).category)

        val electricity = VoiceIntentParser.parse("electricity bill")
        assertTrue(electricity is VoiceIntent.Category)
        assertEquals("Bills", (electricity as VoiceIntent.Category).category)
    }

    @Test
    fun `levenshtein distance within 2 - fuzzy matches category`() {
        // "fod" -> "Food" (dist 1)
        val fod = VoiceIntentParser.parse("fod")
        assertTrue(fod is VoiceIntent.Category)
        assertEquals("Food", (fod as VoiceIntent.Category).category)

        // "travl" -> "Travel" (dist 1)
        val travl = VoiceIntentParser.parse("travl")
        assertTrue(travl is VoiceIntent.Category)
        assertEquals("Travel", (travl as VoiceIntent.Category).category)

        // "shoping" -> "Shopping" (dist 1)
        val shoping = VoiceIntentParser.parse("shoping")
        assertTrue(shoping is VoiceIntent.Category)
        assertEquals("Shopping", (shoping as VoiceIntent.Category).category)
    }

    // ── 3. Amount intent tests (EC-24) ─────────────────────────────────────────

    @Test
    fun `amount only - recognized as Amount intent`() {
        val result = VoiceIntentParser.parse("four fifty")
        assertTrue(result is VoiceIntent.Amount)
        assertEquals(45000L, (result as VoiceIntent.Amount).amountPaise)

        val numResult = VoiceIntentParser.parse("500")
        assertTrue(numResult is VoiceIntent.Amount)
        assertEquals(50000L, (numResult as VoiceIntent.Amount).amountPaise)
    }

    // ── 4. Combined Category and Amount ────────────────────────────────────────

    @Test
    fun `category and amount in same phrase - parsed as CategoryAndAmount`() {
        val result = VoiceIntentParser.parse("food four fifty")
        assertTrue(result is VoiceIntent.CategoryAndAmount)
        val combined = result as VoiceIntent.CategoryAndAmount
        assertEquals("Food", combined.category)
        assertEquals(45000L, combined.amountPaise)

        val cabResult = VoiceIntentParser.parse("cab 200")
        assertTrue(cabResult is VoiceIntent.CategoryAndAmount)
        val cabCombined = cabResult as VoiceIntent.CategoryAndAmount
        assertEquals("Travel", cabCombined.category)
        assertEquals(20000L, cabCombined.amountPaise)
    }

    // ── 5. Split intent (FR-6 contract) ────────────────────────────────────────

    @Test
    fun `split phrase - extracts participant names`() {
        val result = VoiceIntentParser.parse("split with Aman and Priya")
        assertTrue(result is VoiceIntent.Split)
        val split = result as VoiceIntent.Split
        assertEquals(listOf("Aman", "Priya"), split.names)

        val threePeople = VoiceIntentParser.parse("split with Rahul, Sneha and Rohan")
        assertTrue(threePeople is VoiceIntent.Split)
        val splitThree = threePeople as VoiceIntent.Split
        assertEquals(listOf("Rahul", "Sneha", "Rohan"), splitThree.names)

        val divideResult = VoiceIntentParser.parse("divide between Aman and Priya")
        assertTrue(divideResult is VoiceIntent.Split)
        assertEquals(listOf("Aman", "Priya"), (divideResult as VoiceIntent.Split).names)

        val directList = VoiceIntentParser.parse("Aman, Sneha and Rohan")
        assertTrue(directList is VoiceIntent.Split)
        assertEquals(listOf("Aman", "Sneha", "Rohan"), (directList as VoiceIntent.Split).names)

        val ampersandList = VoiceIntentParser.parse("split with Rohit & Sneha")
        assertTrue(ampersandList is VoiceIntent.Split)
        assertEquals(listOf("Rohit", "Sneha"), (ampersandList as VoiceIntent.Split).names)
    }

    // ── 6. Payee and Destination / Recipient Extraction Tests ───────────────

    @Test
    fun `phrase with recipient or destination - extracts payee and place`() {
        val ojasResult = VoiceIntentParser.parse("450 to ojas")
        assertTrue(ojasResult is VoiceIntent.Amount)
        val ojasAmount = ojasResult as VoiceIntent.Amount
        assertEquals(45000L, ojasAmount.amountPaise)
        assertEquals("Ojas", ojasAmount.payee)

        val metroResult = VoiceIntentParser.parse("450 to metro")
        assertTrue(metroResult is VoiceIntent.CategoryAndAmount)
        val metroCombined = metroResult as VoiceIntent.CategoryAndAmount
        assertEquals("Travel", metroCombined.category)
        assertEquals(45000L, metroCombined.amountPaise)
        assertEquals("Metro", metroCombined.payee)

        val starbucksResult = VoiceIntentParser.parse("500 at starbucks")
        assertTrue(starbucksResult is VoiceIntent.CategoryAndAmount)
        val starbucksAmount = starbucksResult as VoiceIntent.CategoryAndAmount
        assertEquals(50000L, starbucksAmount.amountPaise)
        assertEquals("Starbucks", starbucksAmount.payee)
        assertEquals("Food", starbucksAmount.category)

        val splitWithAmount = VoiceIntentParser.parse("split 450 with ojas")
        assertTrue(splitWithAmount is VoiceIntent.Split)
        val splitIntent = splitWithAmount as VoiceIntent.Split
        assertEquals(45000L, splitIntent.amountPaise)
        assertEquals(listOf("Ojas"), splitIntent.names)
    }

    // ── 7. Complex Split Utterance (User Prompt Scenario) ───────────────────

    @Test
    fun `splitting on swiggy amongst three people - cleanly extracts category, payee, amount and names`() {
        val phrase = "splitting 450 on swiggy amongst three people ojas niranjan and chirag"
        val result = VoiceIntentParser.parse(phrase)

        assertTrue(result is VoiceIntent.Split)
        val split = result as VoiceIntent.Split
        assertEquals(45000L, split.amountPaise)
        assertEquals("Food", split.category)
        assertEquals("Swiggy", split.payee)
        assertEquals(listOf("Ojas", "Niranjan", "Chirag"), split.names)
    }

    @Test
    fun `split at starbucks with rahul and priya - extracts food category and participants`() {
        val phrase = "split 1200 at starbucks with rahul and priya"
        val result = VoiceIntentParser.parse(phrase)

        assertTrue(result is VoiceIntent.Split)
        val split = result as VoiceIntent.Split
        assertEquals(120000L, split.amountPaise)
        assertEquals("Food", split.category)
        assertEquals("Starbucks", split.payee)
        assertEquals(listOf("Rahul", "Priya"), split.names)
    }

    @Test
    fun `uber ride split between ojas and me - extracts travel category and excludes me`() {
        val phrase = "800 on uber divide between ojas and me"
        val result = VoiceIntentParser.parse(phrase)

        assertTrue(result is VoiceIntent.Split)
        val split = result as VoiceIntent.Split
        assertEquals(80000L, split.amountPaise)
        assertEquals("Travel", split.category)
        assertEquals("Uber", split.payee)
        assertEquals(listOf("Ojas"), split.names)
    }

    // ── 8. Unclear intent (EC-26) ──────────────────────────────────────────────

    @Test
    fun `unclear phrases - return Unclear intent`() {
        assertEquals(VoiceIntent.Unclear, VoiceIntentParser.parse(""))
        assertEquals(VoiceIntent.Unclear, VoiceIntentParser.parse("   "))
        assertEquals(VoiceIntent.Unclear, VoiceIntentParser.parse("hello world how are you"))
    }
}
