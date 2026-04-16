package ids.alet_zlatevski.behaviour;

import ids.alet_zlatevski.ControllerAccess;

import java.io.IOException;

public class Increment {
    public static void act_stress(ControllerAccess controller, int iterations) throws IOException, InterruptedException {
        int addr = 0;
        System.out.println("Starting stress test: " + iterations + " increments on address " + addr);

        for (int i = 0; i < iterations; i++) {
            long next = controller.increment(addr);
            System.out.println("Iteration " + i + ": value is " + next);
        }

        System.out.println("Stress test finished. Final local view of value: " + controller.read(addr));
    }
}
