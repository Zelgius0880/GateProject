package com.zelgius.gateController


import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.*
import com.google.cloud.firestore.annotation.IgnoreExtraProperties
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@IgnoreExtraProperties
interface FirebaseObject {
    var key: String?
    val firebasePath: String
}

open class FirebaseRepository(serviceAccount: InputStream) {
    private val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl("https://tests-cafb2.firebaseio.com")
        .build();

    init {
        FirebaseApp.initializeApp(options);
    }

    protected val db = FirestoreClient.getFirestore(FirebaseApp.getInstance())

    suspend fun getSnapshot(key: String, path: String): DocumentSnapshot {
        return suspendCancellableCoroutine { cont ->
            ApiFutures.addCallback(
                db.collection(path)
                    .document(key)
                    .get(), object : ApiFutureCallback<DocumentSnapshot> {
                    override fun onFailure(t: Throwable) {
                        cont.resumeWithException(t)
                    }

                    override fun onSuccess(result: DocumentSnapshot) {
                        cont.resume(result)
                    }

                }, Executors.newSingleThreadExecutor()
            )
        }
    }


    fun listen(
        key: String,
        path: String,
        listener: (DocumentSnapshot?, FirestoreException?) -> Unit
    ): ListenerRegistration {
        return db.collection(path)
            .document(key)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot != null)
                    listener(documentSnapshot, firebaseFirestoreException)
            }
    }

    suspend fun set(key: String, path: String, map: Map<String, Any>, options: SetOptions = SetOptions.merge()) =
        suspendCancellableCoroutine<FirestoreException?> {
            println(map)
            ApiFutures.addCallback(
                db.collection(path).document(key).set(map, options),
                object : ApiFutureCallback<WriteResult> {
                    override fun onFailure(t: Throwable) {
                        it.resumeWithException(t)
                    }

                    override fun onSuccess(result: WriteResult) {
                        it.resume(null)
                    }

                },
                Executors.newSingleThreadExecutor()
            )
        }
}