package com.zelgius.gateApp

import android.Manifest
import android.app.Application
import androidx.lifecycle.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(InstantExecutorExtension::class)
class TcpConnectionRepositoryTest {

    private val repository = TcpConnectionRepository

    @Rule
    @JvmField
    val mRuntimePermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
        )

    @Test
    fun start() {
        runBlocking {
            repository.start()
            assertTrue(repository.isConnected)
        }
    }

    @Test
    fun send() {
        try {
            runBlocking {
                repository.start()
                assertTrue(repository.isConnected)
                repository.send("[0;1]")

                //FIXME The receiving message when connected will be removed soon. Just here for testing the message receiving
                repository.messageChannel.asFlow().collectIndexed { _, message ->
                    println(message)
                    this.cancel()
                }
            }
        } catch (e: CancellationException) {
            repository.stop()
            // Everything went right if there
        }
    }

    @Test
    fun stop() {
        runBlocking {
            repository.start()
            assertTrue(repository.isConnected)
            repository.stop()
            assertFalse(repository.isConnected)
        }
    }

    @BeforeEach
    fun beforeEach() {
        val context: Application = ApplicationProvider.getApplicationContext()!!
        val wifiViewModel = WifiViewModel(context)
        val latch = CountDownLatch(1)

        wifiViewModel.connected.observeForever {
            if (!it) {
                wifiViewModel.connectToGateWifi()
            } else latch.countDown()
        }

        //latch.await(10, TimeUnit.SECONDS)
        latch.await()
        assertEquals(wifiViewModel.connected.value, true)
    }

    @AfterAll
    fun afterAll() {
        repository.stop()
    }
}

class OneTimeObserver<T>(private val handler: (T) -> Unit) : Observer<T>, LifecycleOwner {
    private val lifecycle = LifecycleRegistry(this)

    init {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun onChanged(t: T) {
        handler(t)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

fun <T> LiveData<T>.observeOnce(onChangeHandler: (T) -> Unit) {
    val observer = OneTimeObserver(handler = onChangeHandler)
    observe(observer, observer)
}