package com.myhaylow.nexodriver.uber

import android.view.accessibility.AccessibilityNodeInfo

/** Coordinate-free projection of an accessibility tree, also used by JVM tests. */
data class AccessibilityTree(
    val packageName: String?,
    val text: String? = null,
    val contentDescription: String? = null,
    val children: List<AccessibilityTree> = emptyList(),
) {
    fun semanticTexts(targetPackage: String = packageName.orEmpty()): List<String> = buildList {
        if (packageName != targetPackage) return@buildList
        text?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        contentDescription?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        children.forEach { addAll(it.semanticTexts(targetPackage)) }
    }

    companion object {
        fun from(node: AccessibilityNodeInfo): AccessibilityTree = AccessibilityTree(
            packageName = node.packageName?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            children = (0 until node.childCount).mapNotNull { index ->
                node.getChild(index)?.let(::from)
            },
        )
    }
}
