// Importing necessary libraries
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

import transportProtocols.snw_transport;
import transportProtocols.tcp_transport;

//String classpath = "TransportProtocols\src\transportProtocols\tcp_transport.java";
//import classpath
class Server {
    private final int portNumber;
    private final String transportMethod;
    private final String path;

    public Server(int portNumber, String userTransport) {
        this.portNumber = portNumber;
        this.transportMethod = userTransport;
        this.path = "server_fl";
    }
    
    public int getPortNumber() {
        return portNumber;
    }

    public String getTransportProtocol() {
        return transportMethod;
    }

    public String getPath() {
        return path;
    }
    


    public static void main(String[] args) throws IOException{
        if (args.length < 2) {
            System.out.println("Usage: java ServerMain <serverportnumber> <transportprotocol>");
            System.exit(1);
        }

        int serverPortNumber = Integer.parseInt(args[0]);
        String userTransport = args[1];

        Server myServer = new Server(serverPortNumber, userTransport);
        //System.out.println(myServer.portNumber);
        //System.out.println(myServer.transportProtocol);

        if (myServer.transportMethod.equals("tcp")) {

            System.out.println(myServer.portNumber);
            System.out.println(myServer.transportMethod);
            System.out.println(myServer.path);
            // We need to create an instance of the tcp_module class
            tcp_transport tcpModule = new tcp_transport();
            // We need to create an instance of the snw_tansport class
            snw_transport snwModule = new snw_transport();
            // Creating server to accept put request
            /* 
               try {
                tcpModule.tcpCreateTwoServers(myServer.portNumber, myServer.portNumber+ 1000, myServer.path, myServer.transportMethod);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            */
            // Using threading to create sockets to handle both
            // put and get commands and their associated file transfers
            ExecutorService executor = Executors.newFixedThreadPool(2);
        
            executor.submit(() -> {
                try {
                    tcpModule.tcpCreateTwoServers(myServer.portNumber, myServer.portNumber + 1000, myServer.path, myServer.transportMethod);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
            executor.submit(() -> {
                try {
                    tcpModule.tcpCreateTwoServersGetCommandSendFile(myServer.getPortNumber(), myServer.getPath());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        else if (myServer.transportMethod.equals("snw")) {
            // We need to create an instance of the snw_tansport class
            snw_transport snwModule = new snw_transport();
            //snwModule.snw_create_two_servers(myServer.portNumber, myServer.portNumber+1000, myServer.path, myServer.transportMethod);


            ExecutorService executor = Executors.newFixedThreadPool(2);
            
            executor.submit(() -> {
                try {
                    snwModule.snw_create_two_servers(myServer.portNumber, myServer.portNumber + 1000, myServer.path, myServer.transportMethod);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            executor.submit(() -> {
                try {
                    snwModule.snw_create_two_servers_get_command_send_file(myServer.portNumber, myServer.portNumber + 1001, myServer.path);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        
        }
        else{
            System.out.println("Error here!");
        }
        

    }
}   
         




