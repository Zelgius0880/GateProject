package com.zelgius.gateController

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class GateWork(
    protected val gate: Gate,
) {

    val side: GateSide = gate.side

    open var stop: Boolean = false
    open var isOpen: Boolean = false

    protected val scope = CoroutineScope(Dispatchers.IO)
    protected var job: Job? = null

    fun start(): Job {
        return scope.launch {
            run()
        }.apply {
            job = this
        }
    }

    abstract suspend fun run(): GateStatus

    open suspend fun await() {
        job?.join()
    }
}