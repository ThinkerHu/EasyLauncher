package com.vincenthu.launcher

import android.content.pm.ApplicationInfo

/** Stable category keys also survive rotation and process recreation. */
internal enum class AppLibraryCategory(val label: String) {
    ENTERTAINMENT("影音娱乐"), SHOPPING("购物与美食"), TOOLS("实用工具"),
    HEALTH("生活与健康"), OFFICE("办公与金融"), SOCIAL("社交通讯"),
    GAMES("游戏"), TRAVEL("旅游与出行"), PHOTO("拍摄美化"), EDUCATION("教育学习")
}

internal object AppLibraryCategories {
    fun classify(label: String, packageName: String, systemCategory: Int): AppLibraryCategory {
        val name = label.lowercase(java.util.Locale.ROOT)
        val pkg = packageName.lowercase(java.util.Locale.ROOT)
        fun matches(vararg words: String) = words.any { name.contains(it) || pkg.contains(it) }
        // Names refine broad vendor metadata (camera/gallery often report IMAGE,
        // and messaging clients sometimes report PRODUCTIVITY).
        return when {
            matches("相机", "图库", "照片", "美颜", "修图", "camera", "gallery", "photos", "snapseed") -> AppLibraryCategory.PHOTO
            matches("微信", "飞书", "钉钉", "邮件", "联系人", "电话", "weixin", "wechat", "tencent.mobileqq",
                "com.tencent.mobileqq", "com.tencent.tim", "lark", "dingtalk", "email", "contacts", "telegram", "whatsapp") -> AppLibraryCategory.SOCIAL
            matches("淘宝", "京东", "商城", "购物", "美团", "饿了么", "拼多多", "taobao", "jingdong", "pinduoduo", "meituan", "eleme", "vmall") -> AppLibraryCategory.SHOPPING
            matches("地图", "导航", "旅行", "出行", "打车", "铁路", "航旅", "maps", "autonavi", "ctrip", "12306", "trip") -> AppLibraryCategory.TRAVEL
            matches("健康", "运动", "健身", "天气", "日历", "health", "fitness", "weather", "calendar") -> AppLibraryCategory.HEALTH
            matches("学习", "教育", "词典", "课堂", "英语", "儿童", "宝宝", "dictionary", "education", "duolingo") -> AppLibraryCategory.EDUCATION
            matches("音乐", "视频", "播放器", "抖音", "哔哩", "music", "video", "bilibili", "douyin", "youtube", "spotify") -> AppLibraryCategory.ENTERTAINMENT
            matches("办公", "笔记", "备忘录", "文档", "股票", "银行", "钱包", "金融", "office", "notes", "notion", "stock", "wallet", "bank") -> AppLibraryCategory.OFFICE
            matches("游戏", "game") || systemCategory == ApplicationInfo.CATEGORY_GAME -> AppLibraryCategory.GAMES
            systemCategory == ApplicationInfo.CATEGORY_AUDIO || systemCategory == ApplicationInfo.CATEGORY_VIDEO -> AppLibraryCategory.ENTERTAINMENT
            systemCategory == ApplicationInfo.CATEGORY_IMAGE -> AppLibraryCategory.PHOTO
            systemCategory == ApplicationInfo.CATEGORY_SOCIAL -> AppLibraryCategory.SOCIAL
            systemCategory == ApplicationInfo.CATEGORY_MAPS -> AppLibraryCategory.TRAVEL
            systemCategory == ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppLibraryCategory.OFFICE
            else -> AppLibraryCategory.TOOLS
        }
    }
}
