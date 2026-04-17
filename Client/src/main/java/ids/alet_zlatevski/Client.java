package ids.alet_zlatevski;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import static ids.alet_zlatevski.behaviour.Increment.act_increment;
import static ids.alet_zlatevski.behaviour.Manual.act_manual;
import static ids.alet_zlatevski.behaviour.Add.*;

public class Client {

    public static void main(String args[]) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter Controller RMI Port: ");
            int port = Integer.parseInt(scanner.nextLine());

            String host = "localhost";
            Registry registry = LocateRegistry.getRegistry(host, port);
            ControllerAccess controller = (ControllerAccess) registry.lookup("API");

            System.out.println("Select client mode:");
            System.out.println("1 - Manual");
            System.out.println("2 - Stress Test (100 increments)");
            System.out.println("3 - Atomic Stress (Concurrent add - 50 iterations)");
            System.out.print("> ");
            String mode = scanner.nextLine();

            switch (mode) {
                case "1" -> act_manual(controller);
                case "2" -> act_increment(controller, 100);
                case "3" -> act_atomic_stress(controller, 50);
                default -> System.out.println("This mode doesn't exist.");
            }

        } catch (Exception e) {
            System.err.println("Client error:");
            e.printStackTrace();
        }
    }
}
