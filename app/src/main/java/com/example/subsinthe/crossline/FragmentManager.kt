package com.example.subsinthe.crossline

import android.support.v4.app.FragmentManager
import android.support.v4.app.Fragment

fun FragmentManager.pushFragment(frameId: Int, fragment: Fragment, meta: String = "") {
    beginTransaction()
        .replace(frameId, fragment)
        .addToBackStack(meta)
        .commit()
}

fun FragmentManager.tryPopFragment(): String? {
    val fragmentCount = getBackStackEntryCount()
    if (fragmentCount == 0)
        return null

    popBackStack()
    if (fragmentCount == 1)
        return null

    val currentFragment = fragmentCount - 2
    return getBackStackEntryAt(currentFragment).getName()
}
