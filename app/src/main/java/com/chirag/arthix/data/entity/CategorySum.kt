package com.chirag.arthix.data.entity

/**
 * Projection class for the [TransactionDao.getCategorySums] query.
 * Room maps the query result columns (`category`, `total`) to these fields.
 */
data class CategorySum(
    val category: String?,
    val total: Long
)
