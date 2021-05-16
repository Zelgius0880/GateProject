import com.zelgius.gateController.GateRepository
import com.zelgius.gateController.GateSide
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
                repository.getProgress(GateSide.Right)
            )
        }
    }

    @Test
    fun setProgress() {
        runBlocking {
            val progress = Random.nextInt(100)
            repository.setProgress(GateSide.Right, progress)
            assertEquals(
                progress, repository.getProgress(GateSide.Right).also {
                    println(it)
                }
            )
        }
    }

    @Test
    fun getStatus() {
        runBlocking{
            assertNotNull(
                repository.getProgress(GateSide.Right)
            )
        }
    }

    @Test
    fun setStatus() {
        runBlocking {
            val status = GateStatus.values()[Random.nextInt( GateStatus.values().size)]
            repository.setStatus(GateSide.Right, status)
            assertEquals(
                status, repository.getStatus(GateSide.Right).also {
                    println(it)
                }
            )
        }
    }

    @Test
    fun listenStatus() {
        val latch = CountDownLatch(1)
        val status = GateStatus.values()[Random.nextInt( GateStatus.values().size)]
        repository.listenStatus { _, s ->
            assertEquals(s, status)
            latch.countDown()
        }

        runBlocking {
            repository.setStatus(GateSide.Right, status)
        }
        latch.await(5, TimeUnit.SECONDS)
        assertEquals(0, latch.count)
    }

    @Test
    fun getTime() {
        runBlocking{
            assertNotNull(
                repository.getTime(GateSide.Right)
            )
        }
    }

    @Test
    fun setTime() {
        runBlocking {
            val time = Random.nextLong( 1000)
            repository.setTime(GateSide.Right, time)
            assertEquals(
                time, repository.getTime(GateSide.Right).also {
                    println(it)
                }
            )
        }
    }

    @Test
    fun getCurrentStatus() {
        runBlocking{
            assertNotNull(
                repository.getCurrentStatus(GateSide.Right)
            )
        }
    }

    @Test
    fun setCurrentStatus() {
        runBlocking {
            val status = GateStatus.values()[Random.nextInt( GateStatus.values().size)]
            repository.setCurrentStatus(GateSide.Right, status)
            assertEquals(
                status, repository.getCurrentStatus(GateSide.Right).also {
                    println(it)
                }
            )
        }
    }
}