package com.vincenthu.launcher

/** First page shares space with the header; subsequent pages use the full grid.
 * Capacities stay the same after rotation so apps never change their page.
 */
object HomeLayout {
    const val FIRST_PAGE_SIZE = 14
    const val FULL_PAGE_SIZE = 28

    fun columns(landscape: Boolean) = if (landscape) 7 else 4
    fun rows(landscape: Boolean, page: Int = 0) =
        if (page == 0) { if (landscape) 2 else 4 } else { if (landscape) 4 else 7 }

    fun capacity(page: Int) = if (page <= 0) FIRST_PAGE_SIZE else FULL_PAGE_SIZE
    fun pageStart(page: Int) = if (page <= 0) 0 else FIRST_PAGE_SIZE + (page - 1) * FULL_PAGE_SIZE
    fun pageCount(items: Int): Int {
        val remaining = (items - FIRST_PAGE_SIZE).coerceAtLeast(0)
        return 1 + (remaining + FULL_PAGE_SIZE - 1) / FULL_PAGE_SIZE
    }

    fun <T> members(items: List<T>, page: Int): List<T> =
        if (page < 0 || page >= pageCount(items.size)) emptyList()
        else items.drop(pageStart(page)).take(capacity(page))
}
