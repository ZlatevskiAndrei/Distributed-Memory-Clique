package ids.alet_zlatevski;

import ids.alet_zlatevski.controller.Controller;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        String brokerHost = (args.length > 0) ? args[0] : "localhost";
        Controller controller = new Controller(brokerHost);
        IO.println("Controller started with broker: " + brokerHost);
    }
}
