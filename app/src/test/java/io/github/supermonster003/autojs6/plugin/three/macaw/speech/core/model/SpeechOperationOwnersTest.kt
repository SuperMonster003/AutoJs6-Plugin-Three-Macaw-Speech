package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.junit.Assert.*
import org.junit.Test

class SpeechOperationOwnersTest {
    @Test fun serviceCloseRacingCreationCancelsExactlyOnce() {
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            repeat(100) {
                val count = java.util.concurrent.atomic.AtomicInteger()
                val owners = SpeechOperationOwners { count.incrementAndGet() }
                val operation = owners.begin(10)
                val ready = java.util.concurrent.CountDownLatch(1)
                val attach = executor.submit { ready.await(); operation.attach(42) }
                val close = executor.submit { ready.await(); owners.close() }
                ready.countDown()
                attach.get(5, java.util.concurrent.TimeUnit.SECONDS)
                close.get(5, java.util.concurrent.TimeUnit.SECONDS)
                assertEquals(1, count.get())
                operation.finish()
            }
        } finally { executor.shutdownNow() }
    }
    @Test fun onlyTheCreatingUidCanCancelAndCompletionRemovesTheId() {
        val cancelled = mutableListOf<Int>()
        val owners = SpeechOperationOwners { cancelled += it }
        val operation = owners.begin(10)
        operation.attach(42)
        owners.cancel(20, 42)
        assertTrue(cancelled.isEmpty())
        owners.cancel(10, 42)
        owners.cancel(10, 42)
        assertEquals(listOf(42), cancelled)
        assertTrue(operation.finish())
        assertFalse(operation.finish())
        owners.cancel(10, 42)
        assertEquals(listOf(42), cancelled)
    }
    @Test fun callerDeathAndServiceCloseCancelLateOperationsWithoutCancellingCompletedOnes() {
        val cancelled = mutableListOf<Int>()
        val owners = SpeechOperationOwners { cancelled += it }
        val dying = owners.begin(10)
        dying.abort()
        dying.attach(1)
        val completed = owners.begin(20)
        assertTrue(completed.finish())
        completed.attach(2)
        val late = owners.begin(30)
        owners.close()
        late.attach(3)
        assertEquals(listOf(1, 3), cancelled)
        dying.finish()
        late.finish()
    }
}
