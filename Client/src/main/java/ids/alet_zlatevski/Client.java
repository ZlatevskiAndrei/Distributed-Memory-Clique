package ids.alet_zlatevski;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) { //just a stupid test class
        try {
            String host = "localhost";
            int port = 1099;

            Registry registry = LocateRegistry.getRegistry(host, port);
            ControllerAccess controller =
                    (ControllerAccess) registry.lookup("API");

            System.out.println("Connected to controller.");
            System.out.println("Commands:");
            System.out.println("  write <addr> <value>");
            System.out.println("  read <addr>");
            System.out.println("  size");
            System.out.println("  exit");

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();

                if (line.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...");
                    break;
                }

                String[] parts = line.split("\\s+");

                try {
                    switch (parts[0].toLowerCase()) {

                        case "write":
                            if (parts.length != 3) {
                                System.out.println("Usage: write <addr> <value>");
                                break;
                            }

                            int wAddr = Integer.parseInt(parts[1]);
                            long value = Long.parseLong(parts[2]);

                            controller.write(wAddr, value);
                            System.out.println("OK");
                            break;

                        case "read":
                            if (parts.length != 2) {
                                System.out.println("Usage: read <addr>");
                                break;
                            }

                            int rAddr = Integer.parseInt(parts[1]);
                            long result = controller.read(rAddr);

                            System.out.println("Value: " + result);
                            break;

                        case "size":
                            System.out.println("Size: " + controller.size());
                            break;

                        default:
                            System.out.println("Unknown command");
                    }

                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            scanner.close();

        } catch (Exception e) {
            System.err.println("Client error:");
            e.printStackTrace();
        }
    }
}