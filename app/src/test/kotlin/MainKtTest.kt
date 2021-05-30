import com.zelgius.gateController.GateRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

internal class MainKtTest{

    @Test
    fun testListening(){
        val repository = GateRepository()

        val latch = CountDownLatch(1);

        repository.listenStatus { gateSide, gateStatus ->
            println("$gateSide, $gateStatus")
        }

        println("Listening")

        latch.await()
    }
}