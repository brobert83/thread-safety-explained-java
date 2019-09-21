package threadsafety;

import java.util.Random;

import static java.util.stream.IntStream.range;

public class ThreadSafetyExplained {

    static void sleep(int millis) {
        try {

            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static Runnable sleepBeforeTest = () -> sleep(3000);
    static Runnable sleepAfterTest = () -> sleep(100); // this is needed to correctly capture System.out/err output

    static void info(String message) {
        System.out.println(message);
    }

    static void error(String message) {
        System.err.println(message);
    }

    static class SafeCalculator {

        void runBusinessLogic(String name, int value, int beforeWait, int expectedCalculation) {

            info(name + " -> SLEEP " + beforeWait + " before calculation: " + value);
            sleep(beforeWait);

            info(name + " -> doing calculation with :" + value);
            int calculation = value + 55;


            if (calculation == expectedCalculation) {
                info(" -> Calculation correct: " + calculation + "\n");
            } else {
                throw new RuntimeException(" -> Wrong calculation, expected: " + expectedCalculation + " but was: " + calculation + "\n THIS CANNOT HAPPEN");
            }
        }

    }

    static class UnsafeCalculator {

        int value;

        public void setValue(int value) {
            this.value = value;
        }

        void runBusinessLogic(String name, int beforeWait, int expectedCalculation) {

            info(name + " -> SLEEP " + beforeWait + " before calculation: " + value);
            sleep(beforeWait);

            info(name + " -> doing calculation with :" + this.value);
            int calculation = this.value + 55;

            if (calculation == expectedCalculation) {
                info(" -> Calculation correct: " + calculation + "\n");//never gonna happen
            } else {
                error(name + " -> Wrong calculation, expected: " + expectedCalculation + " but was: " + calculation);
            }
        }

    }

    static SafeCalculator safeCalculator = new SafeCalculator();
    static UnsafeCalculator unsafeCalculator = new UnsafeCalculator();
    static Random random = new Random();

    private static void threadSafeSimpleTest() {
        info("Starting single instance, thread safe");
        sleepBeforeTest.run();

        Thread safeThread1 = new Thread(() -> safeCalculator.runBusinessLogic("Thread Safe Thread 1", 10, 50, 65));
        Thread safeThread2 = new Thread(() -> safeCalculator.runBusinessLogic("Thread Safe Thread 1", 5, 1, 60));

        safeThread1.start();
        safeThread2.start();

        try {
            safeThread1.join();
            safeThread2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sleepAfterTest.run();
        info("Finished single instance, thread safe, thread safe\n\n");
        info("------------------------------------------------------------------------");
    }

    private static void notThreadSafeSimpleTest() {
        info("\nStarting single instance, NOT thread safe, classic Servlet problem");
        sleepBeforeTest.run();

        Thread unsafeThread1 = new Thread(() -> {
            unsafeCalculator.setValue(10);
            unsafeCalculator.runBusinessLogic("NOT Thread Safe Thread 1", 50, 65);
        });
        Thread unsafeThread2 = new Thread(() -> {
            unsafeCalculator.setValue(5);
            unsafeCalculator.runBusinessLogic("NOT Thread Safe Thread 2", 1, 60);
        });

        unsafeThread1.start();
        unsafeThread2.start();

        try {
            unsafeThread1.join();
            unsafeThread2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sleepAfterTest.run();
        info("Finished single instance, NOT thread safe, thread safe\n\n");
        info("------------------------------------------------------------------------");
    }

    private static void threadSafeExhaustiveTest() {
        info("Starting exhaustive single instance -> thread safe");
        sleepBeforeTest.run();

        range(0, 100)
                .parallel()
                .forEach(index -> safeCalculator.runBusinessLogic("Thread" + index, index, random.nextInt(10), index + 55));
        info("Finished exhaustive single instance, thread safe\n\n");
        info("------------------------------------------------------------------------");
    }

    private static void notThreadSafeExhaustiveTest() {
        info("Starting exhaustive single instance, NOT thread safe");
        sleepBeforeTest.run();
        range(0, 100)
                .parallel()
                .forEach(index -> {
                    unsafeCalculator.setValue(index);
                    unsafeCalculator.runBusinessLogic("Thread" + index, random.nextInt(10), index + 55);
                });
        sleepAfterTest.run();
        info("Finished exhaustive single instance, NOT thread safe");
        info("------------------------------------------------------------------------");
    }

    public static void main(String[] args) {

        threadSafeSimpleTest();

        notThreadSafeSimpleTest();

        threadSafeExhaustiveTest();

        notThreadSafeExhaustiveTest();
    }

}
