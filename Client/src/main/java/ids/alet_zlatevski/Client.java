package ids.alet_zlatevski;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import static ids.alet_zlatevski.behaviour.Manual.act_manual;

public class Client {

    public static void main(String[] args) { //just a stupid test class
        try {
            String host = "localhost";
            int port = 1099;

            Registry registry = LocateRegistry.getRegistry(host, port);
            ControllerAccess controller =
                    (ControllerAccess) registry.lookup("API");


            switch (args[2]) {
                case "1" -> {
                    act_manual(controller);
                }
                case "2" -> {
                    // TODO add differents type of clients
                }
                default -> {
                    System.out.println("This mode doesn't exist.");
                }
            }

        } catch (Exception e) {
            System.err.println("Client error:");
            e.printStackTrace();
        }
    }
}