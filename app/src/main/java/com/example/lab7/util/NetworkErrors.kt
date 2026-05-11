package com.example.lab7.util

import java.net.UnknownHostException

fun Throwable.isOfflineNetworkError(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is UnknownHostException) return true
        current = current.cause
    }
    return false
}
