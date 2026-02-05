//============================================================================
// Name        : CSC450_Mod8_CounterThreads.java
// Author      : raquilo2 (Richard Aquilo Jr.)
// Description : Module 8 Portfolio Project Part II - Java Concurrency Counters
//============================================================================

package csc450.mod8;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CSC450_Mod8_CounterThreads {

    // Shared state is intentionally minimal and encapsulated.
    private static final class SharedState {
        private int counterValue = 0;         // protected by lock
        private final Object lock = new Object();
    }

    // Use a latch to coordinate "Thread 2 starts after Thread 1 reaches 20".
    private static final CountDownLatch reachedTwenty = new CountDownLatch(1);

    // Separate lock to keep console output readable (avoid interleaving).
    private static final Object consoleLock = new Object();

    private static void safePrint(String label, int value) {
        synchronized (consoleLock) {
            System.out.println(label + value);
        }
    }

    public static void main(String[] args) {
        final SharedState state = new SharedState();

        Thread tUp = new Thread(() -> {
            try {
                for (int i = 0; i <= 20; i++) {
                    synchronized (state.lock) {
                        state.counterValue = i;
                    }
                    safePrint("UP  : ", i);

                    if (i == 20) {
                        reachedTwenty.countDown(); // signal Thread 2 exactly once
                    }

                    // Sleep only to make output observable for grading.
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (InterruptedException ie) {
                // Restore the interrupted status and exit safely.
                Thread.currentThread().interrupt();
                safePrint("UP  : interrupted at ", getCounterSafely(state));
            } catch (RuntimeException re) {
                // Boundary catch to prevent an uncontrolled crash.
                safePrint("UP  : unexpected error: ", 0);
            }
        }, "Counter-Up");

        Thread tDown = new Thread(() -> {
            try {
                // Wait until Thread 1 signals it reached 20.
                reachedTwenty.await();

                for (int i = 20; i >= 0; i--) {
                    synchronized (state.lock) {
                        state.counterValue = i;
                    }
                    safePrint("DOWN: ", i);
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                safePrint("DOWN: interrupted at ", getCounterSafely(state));
            } catch (RuntimeException re) {
                safePrint("DOWN: unexpected error: ", 0);
            }
        }, "Counter-Down");

        tUp.start();
        tDown.start();

        try {
            tUp.join();
            tDown.join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            safePrint("MAIN: interrupted.", 0);
        }

        safePrint("Done.", 0);
    }

    private static int getCounterSafely(SharedState state) {
        synchronized (state.lock) {
            return state.counterValue;
        }
    }
}

