package com.homely.smarthome.core.data

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.functions
import com.google.firebase.storage.storage
import com.homely.smarthome.BuildConfig

private var emulatorsConfigured = false

fun configureFirebaseEmulators() {
    if (!BuildConfig.USE_FIREBASE_EMULATORS || emulatorsConfigured) return

    val host = "10.0.2.2"
    Firebase.auth.useEmulator(host, 9099)
    Firebase.firestore.useEmulator(host, 8080)
    Firebase.storage.useEmulator(host, 9199)
    Firebase.functions.useEmulator(host, 5001)
    emulatorsConfigured = true
}

