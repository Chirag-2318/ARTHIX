package com.chirag.arthix.domain.category

import com.chirag.arthix.data.model.Direction
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionCategoryAiClassifierTest {

    @Test
    fun classifyOutflow_foodMerchants_returnsFood() {
        assertThat(TransactionCategoryAiClassifier.classify("Swiggy", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_FOOD)
        assertThat(TransactionCategoryAiClassifier.classify("Zomato India", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_FOOD)
        assertThat(TransactionCategoryAiClassifier.classify("Starbucks Coffee", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_FOOD)
        assertThat(TransactionCategoryAiClassifier.classify("McDonalds", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_FOOD)
        assertThat(TransactionCategoryAiClassifier.classify("KFC Restaurant", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_FOOD)
        assertThat(TransactionCategoryAiClassifier.classify("Chai Point", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_FOOD)
    }

    @Test
    fun classifyOutflow_travelMerchants_returnsTravel() {
        assertThat(TransactionCategoryAiClassifier.classify("Uber India", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_TRAVEL)
        assertThat(TransactionCategoryAiClassifier.classify("Ola Cabs", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_TRAVEL)
        assertThat(TransactionCategoryAiClassifier.classify("Rapido Bike", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_TRAVEL)
        assertThat(TransactionCategoryAiClassifier.classify("Namma Metro", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_TRAVEL)
        assertThat(TransactionCategoryAiClassifier.classify("IRCTC Rail", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_TRAVEL)
        assertThat(TransactionCategoryAiClassifier.classify("IndianOil Petrol", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_TRAVEL)
    }

    @Test
    fun classifyOutflow_groceriesMerchants_returnsGroceries() {
        assertThat(TransactionCategoryAiClassifier.classify("Blinkit", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_GROCERIES)
        assertThat(TransactionCategoryAiClassifier.classify("Zepto Quick", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_GROCERIES)
        assertThat(TransactionCategoryAiClassifier.classify("Instamart", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_GROCERIES)
        assertThat(TransactionCategoryAiClassifier.classify("BigBasket Daily", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_GROCERIES)
        assertThat(TransactionCategoryAiClassifier.classify("DMart Supermarket", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_GROCERIES)
    }

    @Test
    fun classifyOutflow_shoppingMerchants_returnsShopping() {
        assertThat(TransactionCategoryAiClassifier.classify("Amazon India", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_SHOPPING)
        assertThat(TransactionCategoryAiClassifier.classify("Flipkart Internet", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_SHOPPING)
        assertThat(TransactionCategoryAiClassifier.classify("Myntra Designs", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_SHOPPING)
        assertThat(TransactionCategoryAiClassifier.classify("Zara Retail", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_SHOPPING)
        assertThat(TransactionCategoryAiClassifier.classify("Croma Electronics", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_SHOPPING)
    }

    @Test
    fun classifyOutflow_billsMerchants_returnsBills() {
        assertThat(TransactionCategoryAiClassifier.classify("BESCOM Electricity", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_BILLS)
        assertThat(TransactionCategoryAiClassifier.classify("Airtel Postpaid", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_BILLS)
        assertThat(TransactionCategoryAiClassifier.classify("Jio Recharge", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_BILLS)
        assertThat(TransactionCategoryAiClassifier.classify("Netflix Subscription", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_BILLS)
        assertThat(TransactionCategoryAiClassifier.classify("ACT Broadband", null, Direction.OUTFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.EXPENSE_BILLS)
    }

    @Test
    fun classifyInflow_salary_returnsSalary() {
        assertThat(TransactionCategoryAiClassifier.classify("Infosys Payroll", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_SALARY)
        assertThat(TransactionCategoryAiClassifier.classify("Monthly Salary Credit", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_SALARY)
        assertThat(TransactionCategoryAiClassifier.classify("Google Corp Stipend", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_SALARY)
    }

    @Test
    fun classifyInflow_refund_returnsRefund() {
        assertThat(TransactionCategoryAiClassifier.classify("Amazon Refund", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_REFUND)
        assertThat(TransactionCategoryAiClassifier.classify("UPI Cashback", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_REFUND)
        assertThat(TransactionCategoryAiClassifier.classify("Failed Txn Reversal", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_REFUND)
    }

    @Test
    fun classifyInflow_interest_returnsInterest() {
        assertThat(TransactionCategoryAiClassifier.classify("Bank Savings Interest", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_INTEREST)
        assertThat(TransactionCategoryAiClassifier.classify("FD Interest Credit", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_INTEREST)
        assertThat(TransactionCategoryAiClassifier.classify("Zerodha Dividend", null, Direction.INFLOW))
            .isEqualTo(TransactionCategoryAiClassifier.INCOME_INTEREST)
    }
}
