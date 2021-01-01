import com.zelgius.gateController.GateRepository
import com.zelgius.gateController.GateStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


internal class GateRepositoryTest {

    companion object {
        private lateinit var repository : GateRepository

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            repository = GateRepository()
        }
    }
    @Test
    fun getProgress() {
        runBlocking{
            assertNotNull(
                repository.getProgress()
            )
        }
    }

    @Test
    fun setProgress() {
        runBlocking {
            val progress = Random.nextInt(100)
            repository.setProgress(progress)
            assertEquals(
                progress, repository.getProgress().also {
                    println(it)
                }
            )
        }
    }

    @Test
    fun getStatus() {
        runBlocking{
            assertNotNull(
                repository.getProgress()
            )
        }
    }

    @Test
    fun setStatus() {
        runBlocking {
            val status = GateStatus.values()[Random.nextInt( GateStatus.values().size)]
            repository.setStatus(status)
            assertEquals(
                status, repository.getStatus().also {
                    println(it)
                }
            )
        }
    }

    @Test
    fun listenStatus() {
        val latch = CountDownLatch(1)
        val status = GateStatus.values()[Random.nextInt( GateStatus.values().size)]
        repository.listenStatus {
            assertEquals(it, status)
            latch.countDown()
        }

        runBlocking {
            repository.setStatus(status)
        }
        latch.await(5, TimeUnit.SECONDS)
        assertEquals(0, latch.count)
    }

    @Test
    fun getTime() {
        runBlocking{
            assertNotNull(
                repository.getTime()
            )
        }
    }

    @Test
    fun setTime() {
        runBlocking {
            val time = Random.nextLong( 1000)
            repository.setTime(time)
            assertEquals(
                time, repository.getTime().also {
                    println(it)
                }
            )
        }
    }

    @Test
    fun getCurrentStatus() {
        runBlocking{
            assertNotNull(
                repository.getCurrentStatus()
            )
        }
    }

    @Test
    fun setCurrentStatus() {
        runBlocking {
            val status = GateStatus.values()[Random.nextInt( GateStatus.values().size)]
            repository.setCurrentStatus(status)
            assertEquals(
                status, repository.getCurrentStatus().also {
                    println(it)
                }
            )
        }
    }
}