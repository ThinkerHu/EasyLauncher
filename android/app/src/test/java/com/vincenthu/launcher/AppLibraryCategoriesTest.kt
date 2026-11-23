package com.vincenthu.launcher

import android.content.pm.ApplicationInfo
import org.junit.Assert.*
import org.junit.Test

class AppLibraryCategoriesTest {
    @Test fun namedApplicationsRefineBroadSystemCategories() {
        assertEquals(AppLibraryCategory.SOCIAL, AppLibraryCategories.classify("飞书", "com.ss.android.lark", ApplicationInfo.CATEGORY_PRODUCTIVITY))
        assertEquals(AppLibraryCategory.PHOTO, AppLibraryCategories.classify("图库", "com.vendor.gallery", ApplicationInfo.CATEGORY_UNDEFINED))
        assertEquals(AppLibraryCategory.OFFICE, AppLibraryCategories.classify("WPS Office", "cn.wps.moffice", ApplicationInfo.CATEGORY_UNDEFINED))
        assertEquals(AppLibraryCategory.SHOPPING, AppLibraryCategories.classify("华为商城", "com.vmall.client", ApplicationInfo.CATEGORY_UNDEFINED))
    }

    @Test fun metadataClassifiesAppsWithoutKnownNames() {
        assertEquals(AppLibraryCategory.GAMES, AppLibraryCategories.classify("Tiny Planet", "org.example.planet", ApplicationInfo.CATEGORY_GAME))
        assertEquals(AppLibraryCategory.ENTERTAINMENT, AppLibraryCategories.classify("Orchestra", "org.example.orchestra", ApplicationInfo.CATEGORY_AUDIO))
        assertEquals(AppLibraryCategory.TRAVEL, AppLibraryCategories.classify("Wayfinder", "org.example.wayfinder", ApplicationInfo.CATEGORY_MAPS))
    }

    @Test fun unknownAndCustomAppsAlwaysHaveAFallbackGroup() {
        assertEquals(AppLibraryCategory.TOOLS, AppLibraryCategories.classify("自研测试", "com.example.custom", ApplicationInfo.CATEGORY_UNDEFINED))
        assertEquals(AppLibraryCategory.TOOLS, AppLibraryCategories.classify("", "", 999))
    }

    @Test fun labelAndPackageMatchingAreCaseInsensitive() {
        assertEquals(AppLibraryCategory.PHOTO, AppLibraryCategories.classify("Camera", "COM.EXAMPLE.CAMERA", -1))
        assertEquals(AppLibraryCategory.SOCIAL, AppLibraryCategories.classify("企业协作", "COM.SS.ANDROID.LARK", -1))
        assertEquals(AppLibraryCategory.HEALTH, AppLibraryCategories.classify("天气", "com.vendor.weather", -1))
    }
}
