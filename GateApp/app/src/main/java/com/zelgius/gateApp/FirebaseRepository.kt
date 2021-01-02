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
    protected val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            //.setPersistenceEnabled(true)
            .build()
    }
    private val auth = FirebaseAuth.getInstance()

    suspend fun db(): FirebaseFirestore {
        checkLogin()
        return db
    }

    /**
     * Get a list of object (type defined with [objClass]) from the targeted collection started at [snapshot] and sized with [size]. The collection is defined with [path] and should correspond to a Firestore collection path
     * The collections are ordered by [order]. If null, no ordering is applied
     * @param snapshot DocumentSnapshot
     * @param path String
     * @param size Long
     * @param objClass Class<T>
     * @return List<T>
     */
    suspend fun <T> getPaged(
        snapshot: DocumentSnapshot,
        path: String,
        size: Long,
        objClass: Class<T>,
        order: String? = null
    ): List<T> {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(path)
                .let {
                    if (order != null) {
                        it.orderBy(order)
                    } else it
                }
                .startAfter(snapshot)
                .limit(size)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        continuation.resumeWithException(e)
                    }

                    if (snapshots != null) {
                        val list: MutableList<T> = ArrayList()
                        for (doc in snapshots.documents) {
                            doc.toObject(objClass)?.apply {
                                list.add(this)
                            }
                        }

                        if (continuation.context.isActive)
                            continuation.resume(list)
                    }
                }
        }
    }

    /**
     * See [getPaged]
     * Call for initial -> nothing paged yet
     * @param path String
     * @param size Long
     * @param objClass Class<T>
     * @return List<T>
     */
    protected suspend fun <T : FirebaseObject> getPaged(
        path: String,
        size: Long,
        objClass: Class<T>,
        order: String? = null
    ): List<T> {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(path)
                .limit(size)
                .let {
                    if (order != null) {
                        it.orderBy(order)
                    } else it
                }
                .get()
                .addOnSuccessListener {
                    /* val list: MutableList<T> = //snapshots.toObjects(objClass)
                 ArrayList()
                 for (doc in snapshots.documents) {
                     doc.toObject(objClass)?.apply {
                         list.add(this)
                     }
                 }

                 continuation.resume(list)*/
                    continuation.resume(it.toObjects(objClass))
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    protected suspend fun getSnapshot(firebaseObject: FirebaseObject): DocumentSnapshot {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(firebaseObject.firebasePath)
                .document(firebaseObject.key!!)
                .get()
                .addOnSuccessListener {
                    continuation.resume(it)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    /**
     * Retrieve a [QuerySnapshot] from [parent] parent. [path] is the full path of the collection -> {parent.path}/{collection}
     * @param parent FirebaseObject
     * @param path String
     * @return QuerySnapshot
     */
    protected suspend fun getSubListSnapshot(
        parent: FirebaseObject,
        path: String,
        vararg order: String
    ): QuerySnapshot {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(parent.firebasePath)
                .document(parent.key!!)
                .collection(path.replace("${parent.firebasePath}/", ""))
                .run {
                    if (order.isNotEmpty()) {
                        var query: Query? = null
                        order.forEach {
                            query = if (query == null) orderBy(it)
                            else query!!.orderBy(it)
                        }
                        query!!
                    } else
                        this
                }
                .get()
                .addOnSuccessListener {
                    continuation.resume(it)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    /**
     * Retrieve a [QuerySnapshot] from [parent] parent. [path] is the full path of the collection -> {parent.path}/{collection}
     * @param parent FirebaseObject
     * @param path String
     * @return QuerySnapshot
     */
    protected suspend fun <T : FirebaseObject> getSublist(
        parent: FirebaseObject,
        path: String,
        clazz: Class<T>,
        vararg order: String
    ): List<T> {
        checkLogin()
        return mutableListOf<T>().apply {
            getSubListSnapshot(parent, path, *order).forEach { document ->
                document.toObject(clazz).let {
                    it.key = document.id
                    add(it)
                }
            }
        }
    }

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


    class AlreadyExistsException(string: String) : IllegalStateException(string)

    suspend fun <T : FirebaseObject> createOrUpdate(
        item: T,
        path: String,
        checkUnique: Pair<String, Any>? = null
    ): T {
        checkLogin()
        if (checkUnique != null && !checkIfNotExists(
                path,
                checkUnique
            )
        ) throw AlreadyExistsException("$checkUnique already exists in the collection $path")
        else
            return suspendCoroutine { continuation ->
                db.collection(path)
                    .run {
                        if (item.key != null) {
                            document(item.key!!)
                        } else {
                            document()
                        }
                    }.apply {
                        set(item)
                            .addOnSuccessListener {
                                item.key = id
                                continuation.resume(item)
                            }
                            .addOnFailureListener {
                                continuation.resumeWithException(it)
                            }
                    }
            }
    }

    private suspend fun checkIfNotExists(
        path: String,
        checkUnique: Pair<String, Any>
    ): Boolean {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(path)
                .whereEqualTo(checkUnique.first, checkUnique.second)
                .get()
                .addOnSuccessListener {
                    continuation.resume(it.isEmpty)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    suspend fun <T : FirebaseObject> delete(item: T, path: String): T {
        checkLogin()
        delete(item.key!!, path)
        return item
    }


    suspend fun delete(key: String, path: String): Boolean {
        checkLogin()
        return suspendCoroutine { continuation ->
            db.collection(path)
                .document(key)
                .delete()
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
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

    suspend fun findCrossCollection(field: String, key: String, path: String): QuerySnapshot {
        val db = db()
        return suspendCoroutine { continuation ->
            db.collectionGroup(path)
                .whereEqualTo(field, key)
                .get()
                .addOnSuccessListener {
                    continuation.resume(it)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

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

@IgnoreExtraProperties
interface FirebaseObject {
    var key: String?
    val firebasePath: String

}