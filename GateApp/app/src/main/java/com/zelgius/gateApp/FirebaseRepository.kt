package com.zelgius.gateApp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.isActive
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


open class FirebaseRepository(val anonymousAuth: Boolean = true) {
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            //.setPersistenceEnabled(true)
            .build()
    }
    private val auth = FirebaseAuth.getInstance()

    suspend fun getSnapshot(key: String, path: String): DocumentSnapshot {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(path)
                .document(key)
                .get()
                .addOnSuccessListener {
                    continuation.resume(it)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    suspend fun listen(
        key: String,
        path: String,
        listener: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        checkLogin()
        return db.collection(path)
            .document(key)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot != null)
                    listener(documentSnapshot, firebaseFirestoreException)
            }
    }

    private suspend fun checkLogin() =
        if (anonymousAuth) {
            if (auth.currentUser == null) {
                suspendCoroutine<FirebaseUser> { continuation ->
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                continuation.resume(auth.currentUser!!)
                            } else {
                                continuation.resumeWithException(
                                    task.exception ?: IllegalStateException("Unknown error")
                                )
                            }
                        }
                }
            } else auth.currentUser!!
        } else null

    suspend fun set(
        key: String,
        path: String,
        map: Map<String, Any>,
        options: SetOptions = SetOptions.merge()
    ) {
        checkLogin()
        suspendCoroutine<FirebaseFirestoreException?> { continuation ->
            db.collection(path).document(key).set(map, options)
                .addOnSuccessListener {
                    continuation.resume(null)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

}