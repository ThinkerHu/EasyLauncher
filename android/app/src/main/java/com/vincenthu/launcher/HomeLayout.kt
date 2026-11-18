package com.vincenthu.launcher

object HomeLayout {
    const val PAGE_SIZE = 14
    fun columns(landscape: Boolean) = if (landscape) 7 else 4
    fun rows(landscape: Boolean) = if (landscape) 2 else 4
    fun pageCount(items: Int) = maxOf(1, (items + PAGE_SIZE - 1) / PAGE_SIZE)
}
