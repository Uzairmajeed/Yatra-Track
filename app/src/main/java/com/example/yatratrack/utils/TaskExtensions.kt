package com.example.yatratrack.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener {
        if (it.exception != null) {
            cont.resumeWithException(it.exception!!)
        } else {
            cont.resume(it.result)
        }
    }
}
