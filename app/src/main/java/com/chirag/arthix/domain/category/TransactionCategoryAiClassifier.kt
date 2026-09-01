package com.chirag.arthix.domain.category

import com.chirag.arthix.data.model.Direction

/**
 * Pure Kotlin AI / heuristic category classifier for transactions.
 *
 * Covers 200+ Indian and global merchants, UPI keywords, and narratives.
 * Categorizes both OUTFLOW (Expense) and INFLOW (Income) payments into
 * canonical application categories.
 *
 * Zero Android framework dependencies — 100% JVM unit-testable.
 */
object TransactionCategoryAiClassifier {

    // ═══════════════════════════════════════════════════════════════════════════
    // OUTFLOW (EXPENSE) TAXONOMY & KEYWORDS
    // ═══════════════════════════════════════════════════════════════════════════

    val EXPENSE_FOOD = "Food"
    val EXPENSE_TRAVEL = "Travel"
    val EXPENSE_SHOPPING = "Shopping"
    val EXPENSE_BILLS = "Bills"
    val EXPENSE_GROCERIES = "Groceries"
    val EXPENSE_OTHER = "Other"

    // ═══════════════════════════════════════════════════════════════════════════
    // INFLOW (INCOME) TAXONOMY & KEYWORDS
    // ═══════════════════════════════════════════════════════════════════════════

    val INCOME_SALARY = "Salary"
    val INCOME_REFUND = "Refund"
    val INCOME_GIFT = "Gift"
    val INCOME_INTEREST = "Interest"
    val INCOME_OTHER = "Other"

    // ── Outflow / Expense Keyword Mappings ─────────────────────────────────────

    private val FOOD_KEYWORDS = setOf(
        "swiggy", "zomato", "mcdonald", "mcdonalds", "kfc", "domino", "dominos", "pizza", "pizzahut",
        "subway", "burger", "burgerking", "starbucks", "cafe", "coffee", "chaipoint", "chaayos",
        "barbequenation", "haldiram", "bikanervala", "behrouz", "faasos", "freshmenu", "eatclub",
        "rebel foods", "baskin", "baskinrobbins", "naturals ice cream", "barista", "costa coffee",
        "third wave coffee", "blue tokai", "theobroma", "belgian waffle", "dunkin", "restaurant",
        "dining", "food", "lunch", "dinner", "breakfast", "snack", "snacks", "bakery", "sweets",
        "bhojan", "khana", "dhaba", "canteen", "biryani", "shawarma", "bar", "pub", "brewery",
        "tiffin", "mess", "chai", "tea", "paratha", "dosai", "dosa", "idli", "roll", "rolls",
        "bakehouse", "patisserie", "bistro", "eatery", "kitchen", "treat", "hotel food"
    )

    private val TRAVEL_KEYWORDS = setOf(
        "uber", "ola", "rapido", "namma metro", "delhi metro", "metro", "irctc", "railway", "train",
        "makemytrip", "mmt", "goibibo", "cleartrip", "ixigo", "yatra", "easemytrip", "indigo",
        "air india", "spicejet", "akasa", "vistara", "airasia", "flight", "airline", "airport",
        "redbus", "abhibus", "bus", "cab", "taxi", "auto", "rickshaw", "chalo", "zoomcar", "revv",
        "indianoil", "ioc", "iocl", "hpcl", "hindustan petroleum", "bpcl", "bharat petroleum",
        "shell", "petrol", "diesel", "cng", "fuel", "gas station", "fastag", "toll", "parking",
        "park+", "bounce", "vogo", "transport", "travel", "commute", "fare", "ride", "trip", "ticket"
    )

    private val GROCERIES_KEYWORDS = setOf(
        "blinkit", "zepto", "instamart", "swiggy instamart", "bigbasket", "bbdaily", "dmart",
        "dmart ready", "reliance fresh", "reliance smart", "nature's basket", "milkbasket",
        "country delight", "spencer", "spencers", "more retail", "hypercity", "grofers", "dunzo",
        "grocery", "groceries", "supermarket", "mart", "kirana", "provision", "provisions",
        "vegetable", "vegetables", "fruit", "fruits", "sabzi", "mandi", "milk", "dairy",
        "amul", "mother dairy", "nandini", "meat", "licious", "freshtohome", "egg", "fish"
    )

    private val SHOPPING_KEYWORDS = setOf(
        "amazon", "flipkart", "myntra", "ajio", "nykaa", "tata cliq", "meesho", "zara", "h&m",
        "hnm", "uniqlo", "marks & spencer", "westside", "pantaloons", "shoppers stop", "lifestyle",
        "max fashion", "trends", "reliance digital", "croma", "vijay sales", "apple", "samsung",
        "oneplus", "xiaomi", "mi store", "boat", "noise", "decathlon", "nike", "adidas", "puma",
        "skechers", "reebok", "woodland", "bata", "lenskart", "titan", "fastrack", "sephora",
        "purplle", "mall", "shopping", "clothes", "clothing", "apparel", "shoes", "footwear",
        "electronics", "gadget", "store", "retail", "fashion", "boutique", "jewellers", "tanishq",
        "kalyan", "malabar", "caratlane"
    )

    private val BILLS_KEYWORDS = setOf(
        "electricity", "bescom", "tata power", "adani electricity", "cesc", "mseb", "bses",
        "water bill", "water supply", "gas bill", "igl", "mahanagar gas", "indraprastha gas",
        "hp gas", "indane", "bharat gas", "lpg", "broadband", "wifi", "act fibernet", "airtel",
        "airtel xstream", "jio", "jiofiber", "jiocare", "vodafone", "vi", "bsnl", "tata play",
        "tata sky", "dish tv", "dth", "recharge", "mobile bill", "postpaid", "prepaid",
        "netflix", "spotify", "prime video", "amazon prime", "hotstar", "disney", "youtube premium",
        "apple.com/bill", "itunes", "google play", "playstore", "rent", "maintenance", "society",
        "nobroker", "mygate", "apartment", "utility", "billdesk", "bbps", "insurance", "lic",
        "hdfc ergo", "star health", "policybazaar", "emi", "loan", "credit card", "cred", "cheq"
    )

    // ── Inflow / Income Keyword Mappings ──────────────────────────────────────

    private val SALARY_KEYWORDS = setOf(
        "salary", "payroll", "stipend", "wages", "remuneration", "monthly pay", "bonus",
        "infosys", "tcs", "wipro", "cognizant", "accenture", "google", "microsoft", "amazon dev",
        "flipkart internet", "swiggy corp", "zomato media", "tech mahindra", "hcl", "capgemini",
        "ibm", "oracle", "deloitte", "pwc", "ey", "kpmg", "employer", "corporate credit",
        "sal cr", "salary for", "payroll credit"
    )

    private val REFUND_KEYWORDS = setOf(
        "refund", "cashback", "reversal", "reversed", "returned", "credit back", "reimbursed",
        "reimbursement", "payout refund", "chargeback", "settlement refund", "failed txn refund",
        "upi refund", "claim approved", "refund from"
    )

    private val GIFT_KEYWORDS = setOf(
        "gift", "reward", "prize", "shagun", "diwali", "birthday", "festive", "bonus reward",
        "lottery", "contest", "cash reward", "scratch card", "voucher credit", "referral bonus",
        "cash prize", "token of love"
    )

    private val INTEREST_KEYWORDS = setOf(
        "interest", "int.pd", "int pd", "savings interest", "fd interest", "term deposit",
        "fixed deposit", "recurring deposit", "dividend", "dividend payout", "coupon credit",
        "mutual fund", "mf payout", "zerodha", "groww", "upstox", "indmoney", "capital gain"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Classifies a transaction based on payee/merchant name, raw narrative text, and direction.
     *
     * @param payee The payee / place / merchant name (e.g. "Swiggy", "Uber", "TCS Payroll").
     * @param rawText Optional raw text from notification, SMS, or voice transcript.
     * @param direction OUTFLOW or INFLOW.
     * @return The best matching canonical category name, or null if ambiguous.
     */
    fun classify(payee: String?, rawText: String? = null, direction: Direction): String? {
        val combined = listOfNotNull(payee, rawText)
            .joinToString(" ")
            .lowercase()
            .trim()

        if (combined.isBlank()) return null

        return if (direction == Direction.INFLOW) {
            classifyInflow(combined)
        } else {
            classifyOutflow(combined)
        }
    }

    /**
     * Classify an outflow / expense transaction.
     */
    fun classifyOutflow(text: String): String? {
        val lower = text.lowercase()

        // 1. Groceries checked before Food (Instamart, Blinkit, Zepto, DMart are groceries)
        if (matchesAny(lower, GROCERIES_KEYWORDS)) return EXPENSE_GROCERIES

        // 2. Food & Dining
        if (matchesAny(lower, FOOD_KEYWORDS)) return EXPENSE_FOOD

        // 3. Travel & Transportation
        if (matchesAny(lower, TRAVEL_KEYWORDS)) return EXPENSE_TRAVEL

        // 4. Bills & Utilities
        if (matchesAny(lower, BILLS_KEYWORDS)) return EXPENSE_BILLS

        // 5. Shopping & Retail
        if (matchesAny(lower, SHOPPING_KEYWORDS)) return EXPENSE_SHOPPING

        return null
    }

    /**
     * Classify an inflow / income transaction.
     */
    fun classifyInflow(text: String): String? {
        val lower = text.lowercase()

        // 1. Salary & Payroll
        if (matchesAny(lower, SALARY_KEYWORDS)) return INCOME_SALARY

        // 2. Refund & Cashback
        if (matchesAny(lower, REFUND_KEYWORDS)) return INCOME_REFUND

        // 3. Interest & Dividends
        if (matchesAny(lower, INTEREST_KEYWORDS)) return INCOME_INTEREST

        // 4. Gift & Rewards
        if (matchesAny(lower, GIFT_KEYWORDS)) return INCOME_GIFT

        return null
    }

    /**
     * Returns true if [text] contains any keyword in [keywords] as a word or substring.
     */
    private fun matchesAny(text: String, keywords: Set<String>): Boolean {
        for (kw in keywords) {
            if (kw.contains(' ')) {
                if (text.contains(kw)) return true
            } else {
                // Word boundary or containment check
                val regex = Regex("\\b${Regex.escape(kw)}\\b")
                if (regex.containsMatchIn(text) || text.contains(kw)) return true
            }
        }
        return false
    }
}
