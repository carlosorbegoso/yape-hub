package org.sky.util;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockRetryServiceTest {

    private DeadlockRetryService deadlockRetryService;

    @BeforeEach
    void setUp() {
        deadlockRetryService = new DeadlockRetryService();
    }

    @Test
    void testSuccessfulOperationWithoutRetry() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        Uni<String> result = deadlockRetryService.executeWithRetry(
            () -> {
                callCount.incrementAndGet();
                return Uni.createFrom().item("success");
            },
            "testOperation"
        );

        String actualResult = result.await().atMost(Duration.ofSeconds(5));
        
        assertEquals("success", actualResult);
        assertEquals(1, callCount.get());
    }

    @Test
    void testDeadlockRetrySuccess() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        Uni<String> result = deadlockRetryService.executeWithRetry(
            () -> {
                int currentCall = callCount.incrementAndGet();
                if (currentCall <= 2) {
                    // Simular deadlock en los primeros 2 intentos
                    RuntimeException deadlockError = new RuntimeException("deadlock detected (40P01)");
                    return Uni.createFrom().failure(deadlockError);
                } else {
                    // Éxito en el tercer intento
                    return Uni.createFrom().item("success after retry");
                }
            },
            "testDeadlockOperation"
        );

        String actualResult = result.await().atMost(Duration.ofSeconds(10));
        
        assertEquals("success after retry", actualResult);
        assertEquals(3, callCount.get());
    }

    @Test
    void testMaxRetriesExceeded() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        Uni<String> result = deadlockRetryService.executeWithRetry(
            () -> {
                callCount.incrementAndGet();
                RuntimeException deadlockError = new RuntimeException("deadlock detected (40P01)");
                return Uni.createFrom().failure(deadlockError);
            },
            "testMaxRetriesOperation",
            2, // Solo 2 reintentos máximo
            Duration.ofMillis(50) // Delay más corto para test
        );

        UniAssertSubscriber<String> subscriber = result
            .subscribe().withSubscriber(UniAssertSubscriber.create());
        
        subscriber.awaitFailure(Duration.ofSeconds(5));
        
        // Debería haber hecho 3 intentos (1 inicial + 2 reintentos)
        assertEquals(3, callCount.get());
        assertTrue(subscriber.getFailure().getMessage().contains("deadlock detected"));
    }

    @Test
    void testNonDeadlockErrorNoRetry() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        Uni<String> result = deadlockRetryService.executeWithRetry(
            () -> {
                callCount.incrementAndGet();
                RuntimeException nonDeadlockError = new RuntimeException("connection timeout");
                return Uni.createFrom().failure(nonDeadlockError);
            },
            "testNonDeadlockOperation"
        );

        UniAssertSubscriber<String> subscriber = result
            .subscribe().withSubscriber(UniAssertSubscriber.create());
        
        subscriber.awaitFailure(Duration.ofSeconds(5));
        
        // Solo debería haber hecho 1 intento (no reintentar errores no-deadlock)
        assertEquals(1, callCount.get());
        assertTrue(subscriber.getFailure().getMessage().contains("connection timeout"));
    }

    @Test
    void testVoidOperationWithRetry() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        Uni<Void> result = deadlockRetryService.executeWithRetryVoid(
            () -> {
                int currentCall = callCount.incrementAndGet();
                if (currentCall <= 1) {
                    // Simular deadlock en el primer intento
                    RuntimeException deadlockError = new RuntimeException("deadlock detected");
                    return Uni.createFrom().failure(deadlockError);
                } else {
                    // Éxito en el segundo intento
                    return Uni.createFrom().voidItem();
                }
            },
            "testVoidOperation"
        );

        result.await().atMost(Duration.ofSeconds(5));
        
        assertEquals(2, callCount.get());
    }

    @Test
    void testDeadlockDetectionVariations() {
        String[] deadlockMessages = {
            "deadlock detected (40P01)",
            "DEADLOCK DETECTED",
            "deadlock detected",
            "lock timeout",
            "could not serialize access"
        };

        for (String message : deadlockMessages) {
            AtomicInteger callCount = new AtomicInteger(0);
            
            Uni<String> result = deadlockRetryService.executeWithRetry(
                () -> {
                    int currentCall = callCount.incrementAndGet();
                    if (currentCall <= 1) {
                        RuntimeException deadlockError = new RuntimeException(message);
                        return Uni.createFrom().failure(deadlockError);
                    } else {
                        return Uni.createFrom().item("success");
                    }
                },
                "testDeadlockDetection: " + message
            );

            String actualResult = result.await().atMost(Duration.ofSeconds(5));
            assertEquals("success", actualResult);
            assertEquals(2, callCount.get());
        }
    }
}
