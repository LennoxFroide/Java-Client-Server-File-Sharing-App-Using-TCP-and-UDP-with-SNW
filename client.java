import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import transportProtocols.*;


class Client {
    private String serverIp;
    private int serverPort;
    private String cacheIp;
    private int cachePort;
    private String transportProtocol;
    private String path;

    public Client(String serverIp, int serverPort, String cacheIp, int cachePort, String transportProtocol) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.cacheIp = cacheIp;
        this.cachePort = cachePort;
        this.transportProtocol = transportProtocol;
        this.path = "client_fl";
    }

    public String[] getCommandAndFile(String input) {
        String[] actionArray = input.split("\\s+");
        if (actionArray.length < 1 || actionArray.length > 2) {
            throw new IllegalArgumentException("Error in taking in commands!");
        }
        String action = actionArray[0].toLowerCase();
        String filename = actionArray.length == 2 ? actionArray[1] : null;
        return new String[]{action, filename};
    }

    public void quit() {
        System.out.println("Exiting program!");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        
        if (args.length < 5) {
            System.out.println("Usage: java Client <serverip> <serverportnumber> <cacheip> <cacheportnumber> <transportprotocol>");
            System.exit(1);
        }
        
        Client myClient = new Client(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4]);
        //System.out.println(myClient.serverIp);
        //System.out.println(myClient.serverPort); //myClient.cacheIp, myClient.cachePort, myClient.transportProtocol;
        //System.out.println(myClient.cacheIp);
        //System.out.println(myClient.cachePort);
        //System.out.println(myClient.transportProtocol);
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("Enter command: ");
            String userCommand = userInput.readLine();


            String[] commandFileArray = myClient.getCommandAndFile(userCommand);
            String command = commandFileArray[0];
            String filename = commandFileArray[1];
            // Possible command
            String possibleCommand = "put";
            if (possibleCommand.equals(command)) {
                //System.out.println(possibleCommand.equals(command));
                //System.out.println("The command is " + command);
                //System.out.println("The file is " + filename);
                // Fails because server is not running
                Socket clientSocket = new Socket(myClient.serverIp, myClient.serverPort);
                clientSocket.setSoTimeout(6000000);
                // Implement tcp_upload here
                tcp_transport tcpModule = new tcp_transport();
                //System.out.println("Have created instance of tcp transport class!");
                // Sending the put command to the server
                //tcp_put_command = tcpModule.tcpPutCommand();
                // We have to add a try and catch block to handle the IOException
                //tcpModule.tcpPutCommand(filename, 0, command, clientSocket);
                try{
                    tcpModule.tcpPutCommand(myClient.serverIp, myClient.serverPort, userCommand, clientSocket);
                    //System.out.println(possibleCommand.equals(command));
                }catch(IOException e){
                    System.out.println("An IOException: " + e.getMessage());
                }
                // Implement tcp_put_command here
                String pathName = Paths.get(myClient.path, filename).toString();
                // Reading the entire file into a string
                String fileToUpload = Files.readString(Paths.get(pathName));
                // System.out.println(fileToUpload);
                if ("tcp".equals(myClient.transportProtocol)) {
                    //System.out.println(filename);
                    //System.out.println(pathName);
                    // Implement tcp_upload here
                    // tcp_transport tcpModule = new tcp_transport();
                    // Sending the actual file to the server.
                    try{
                    tcpModule.tcpUpload(myClient.serverIp, myClient.serverPort, pathName, clientSocket);
                    }
                    catch(Exception ex){
                        System.out.println("An IOException: " + ex.getMessage());
                    }
                } else {
                    // Creating an instance of the snw_transport class
                    snw_transport snwModule = new snw_transport();
                    //System.out.println("We've created an instance of the snw put class");
                    snwModule.snw_upload(myClient.serverIp, myClient.serverPort + 1000, myClient.path, filename);;
                }
            } else if ("quit".equals(command)) {
                myClient.quit();
            } else if ("get".equals(command)) {
                //System.out.println("The command is " + command);
                //System.out.println("The file is " + filename);
                if ("tcp".equals(myClient.transportProtocol)) {
                    // Implement tcp_upload here
                    tcp_transport tcpModule = new tcp_transport();
                    //System.out.println("Have created instance of tcp transport class get command!");
                    //System.out.println(command);
                    // Implement tcp_get_command here
                    String sourceOfFile;
                    // sourceOfFile = null;
                    tcpModule.tcpGetCommand(myClient.path, myClient.cacheIp, myClient.cachePort, myClient.serverIp, myClient.serverPort, userCommand);
                } else {
                    DatagramSocket clientSocketSNW = new DatagramSocket();
                    clientSocketSNW.connect(InetAddress.getByName(myClient.cacheIp), myClient.cachePort + 100);
                    // Implement snw_get_command here
                    snw_transport snwModule = new snw_transport();
                    snwModule.snw_get_command(myClient.path, myClient.cacheIp, myClient.cachePort, myClient.serverIp, myClient.serverPort, userCommand, clientSocketSNW);
                }
            }
        }
    }
}

