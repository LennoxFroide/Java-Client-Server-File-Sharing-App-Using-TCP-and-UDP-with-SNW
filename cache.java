import java.net.*;
import java.io.*;
import java.util.*;
import transportProtocols.*;


// This set up the cache 
class Cache {
    /**
     * This class establishes the cache.
     */
    private int cachePortNumber;
    private String serverIp;
    private int serverPort;
    private String transportProtocol;
    private String path;

    public Cache(int cacheportnumber, String serverip, int serverport, String transportprotocol) {
        // Setting the port that cache will run on
        this.cachePortNumber = cacheportnumber;
        // Saving the server's ip
        this.serverIp = serverip;
        // Saving the server's port number
        this.serverPort = serverport;
        // Setting the transport protocol that the cache will use
        this.transportProtocol = transportprotocol;
        // The directory to cache
        this.path = "cache_fl";
    }

    public static void main(String[] args) throws IOException {
        // We need this main method to get inputs from the user from the command line
        if (args.length < 4) {
            System.out.println("Usage: java Main <cacheportnumber> <serverip> <serverportnumber> <transportprotocol>");
            System.exit(1);
        }

        int cachePortNumber = Integer.parseInt(args[0]);
        String serverIp = args[1];
        int serverPortNumber = Integer.parseInt(args[2]);
        String transportProtocol = args[3];

        // System.out.println(Arrays.toString(args));

        // Now we can set the values for the client class
        Cache myCache = new Cache(cachePortNumber, serverIp, serverPortNumber, transportProtocol);

        // By default the server will be a receiver
        if (myCache.transportProtocol.equals("tcp")) {
            //System.out.println(Arrays.toString(args));
            System.out.println(myCache.cachePortNumber);
            System.out.println(myCache.serverIp);
            System.out.println(myCache.serverPort);
            System.out.println(myCache.transportProtocol);
            // Implement tcp_upload here
            tcp_transport tcpModule = new tcp_transport();
            // We establish a tcp welcoming door        // Receives client's get command  // Send get to server
            try{
            tcpModule.tcpCreateTwoCachesGetCommand(myCache.cachePortNumber, myCache.cachePortNumber + 10, myCache.path, myCache.serverIp, myCache.serverPort + 100);
            }catch(Exception ex){
                System.out.println("Cache IOException!!!");
            }
        } else if (myCache.transportProtocol.equals("snw")) {
            //UDP Socket to receive files from server 
            snw_transport snwModule = new snw_transport();
            //clientSocketSNW.connect(InetAddress.getByName(myClient.cacheIp), myClient.cachePort + 100);
            DatagramSocket cacheSocketSNW = new DatagramSocket(cachePortNumber+100);
            //cacheSocketSNW.connect(myCache.serverIp);
            snwModule.snw_create_two_caches_get_command(myCache.cachePortNumber, myCache.cachePortNumber + 10, myCache.path, myCache.serverIp, myCache.serverPort, cacheSocketSNW);
        } else {
            // Do nothing
        }
    }
}

// Note: You'll need to implement TcpModule and SnwModule classes with the respective methods.
// Also, make sure to handle exceptions appropriately in a real implementation.



