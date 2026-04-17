package ids.alet_zlatevski.behaviour;

import ids.alet_zlatevski.ControllerAccess;
import java.io.IOException;

public class Add {


    public static void act_atomic_stress(ControllerAccess controller, int iterations) throws IOException, InterruptedException {
        int addr = 0;
        long delta = 5;
        System.out.println("Atomic Stress: Starting " + iterations + " pairs of add(" + delta + ")/add(-" + delta + ")");

        for (int i = 0; i < iterations; i++) {
            controller.add(addr, delta);
            controller.add(addr, -delta);
            if (i % 10 == 0) System.out.println("Iteration " + i + " done");
        }

        long finalVal = controller.read(addr);
        System.out.println("Atomic Stress finished. Current value: " + finalVal);
    }
}
