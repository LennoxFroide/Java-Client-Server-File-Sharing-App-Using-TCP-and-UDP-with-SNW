package transportProtocols;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class tcp_transport {

    public static String[] tcpExtractCommandAndFile(String string) {
        
            String[] actionArray = string.split(" ");
            if (actionArray.length <= 1 || actionArray.length > 2) {
                throw new IllegalArgumentException("Error in taking in commands!");
                // System.out.println("Error in taking in commands!");
            } else if (actionArray.length == 2) {
                String action = actionArray[0].toLowerCase();
                String filename = actionArray[1];
                return new String[]{action, filename};
            } else {
                return null;
            }
    }

    public void tcpPutCommand(String serverIp, int serverPortNumber, String putCommand, Socket clientSocket) throws IOException {
        try{
            System.out.println("Awaiting server response.");
            String serverName = serverIp;
            int serverPort = serverPortNumber;
            String message = putCommand;
            //System.out.println(message.length());
            // We need to create output stream
            PrintWriter streamToServer = new PrintWriter(clientSocket.getOutputStream(), true);
            // Sedning message to server
            streamToServer.println(message);
            // clientSocket.getOutputStream().write(message.getBytes());
            // BufferReaders take in input from server
            BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String serverResponse = streamFromServer.readLine();

            if (serverResponse.equals("File created successfully!")) {
                //System.out.println("Server did it!");
                //clientSocket.close();
            } else {
                System.out.println("Server side error!\n");
                System.out.println(serverResponse);
            }

        }
        catch(IOException e){
            System.out.println("IOException caught!");
        }
    }


    // ----------------------FUNCTION TO GET THE GET COMMAND-------------------------------------------------//
    
    public void tcpGetCommand(String clientDirectory, String cacheIp, int cachePortNumber, String serverIp, int serverPortNumber, String getCommand) throws Exception {
        try{
        String cacheName = cacheIp;
        int cachePort = cachePortNumber;
        String serverName = serverIp;
        int serverPort = serverPortNumber;
        Socket clientSocket = new Socket(cacheName, cachePort);
        String message = getCommand;
        String[] commandAndFile = tcpExtractCommandAndFile(getCommand);
        String filename = commandAndFile[1];
        // Sending get command to server
        PrintWriter streamGetCommandToCache = new PrintWriter(clientSocket.getOutputStream(), true);
        streamGetCommandToCache.println(message);
        //clientSocket.getOutputStream().write(message.getBytes());
        // Preparing file to write server data to
        Path filePath = Paths.get(clientDirectory, filename);
        BufferedWriter fileToWrite = Files.newBufferedWriter(filePath);
        /* 
        String filename = commandAndFile[1];
        clientSocket.getOutputStream().write(message.getBytes());
        Path filePath = Paths.get(clientDirectory, filename);
        BufferedWriter fileToWrite = Files.newBufferedWriter(filePath);
        */
        // Grabbing the directory of where the file resides
        String cacheDirectory = "cache_fl";
        String serverDirectory = "server_fl";
        Path filePathCache = Paths.get(cacheDirectory, filename);
        Path filePathServer = Paths.get(serverDirectory, filename);
        String sourceMessage;

        if (Files.exists(filePathCache)) {
            sourceMessage = "File delivered from cache.";
        } else if (Files.exists(filePathServer)) {
            sourceMessage = "File delivered from server.";
        } else {
            sourceMessage = "Error: File is neither in cache nor server!";
        }

        BufferedReader streamFileFromCache = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String cacheResponse;
        while ((cacheResponse = streamFileFromCache.readLine()) != null) {
            //System.out.println(cacheResponse);
            fileToWrite.write(cacheResponse);
            fileToWrite.write("\n");
        }
        fileToWrite.close();
        clientSocket.close();
        System.out.println(sourceMessage);
    }
    catch(Exception ex){
        System.out.println("IOException caught!");
        }
    }
    

    //------------------FUNCTION TO CREATE TWO CACHES FOR THE GET COMMAND----------------------------------------//
    
    public void tcpCreateTwoCachesGetCommand(int cachePortNumber, int cachePortTwo, String cacheDirectory, String serverIp, int serverPortNumber) throws Exception {
        int cachePortGetCommand = cachePortNumber;
        int cachePortConnectToServer = cachePortTwo;
        int serverPortSendGet = serverPortNumber;
        String serverSendGetName = serverIp;
        // This will be a server socket awaiting the client to reach out
        ServerSocket cacheSocketGetCommand = new ServerSocket(cachePortGetCommand);
        // This socket will send the get command to the server
        Socket cacheSocketSendCommandToServer = new Socket(serverSendGetName, serverPortSendGet);

        System.out.println("Cache is ready for the client!");

        while (true) {
            Socket clientSocket = cacheSocketGetCommand.accept();
            BufferedReader streamClientSendingGet = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = streamClientSendingGet.readLine();
            String[] commandAndFile = tcpExtractCommandAndFile(message);
            String command = commandAndFile[0];
            String filename = commandAndFile[1];

            if (command.equals("get")) {
                Path filePath = Paths.get(cacheDirectory, filename);
                // Checking if the file exists in the cache folder
                if (Files.exists(filePath)) {// We have the file
                    // This sends the file to the client
                    BufferedReader readFile = Files.newBufferedReader(filePath);
                    StringBuilder fileToDownload = new StringBuilder();
                    String line;
                    while ((line = readFile.readLine()) != null) {
                        fileToDownload.append(line).append("\n");
                    }
                    clientSocket.getOutputStream().write(fileToDownload.toString().getBytes());
                    clientSocket.close();
                    continue;
                } // Getting the file from the server
                else {
                    // cacheSocketSendCommandToServer.connect(new InetSocketAddress(serverSendGetName, serverPortSendGet));
                    // We want to send the get command to the client socket-->cacheSocketSendCommandToServer
                    PrintWriter streamGetCommandToServer = new PrintWriter(cacheSocketSendCommandToServer.getOutputStream(), true);
                    streamGetCommandToServer.println(message);
                    // cacheSocketSendCommandToServer.getOutputStream().write(message.getBytes());
                    // Preparing file to write the server data in
                    BufferedWriter cachefileToWrite = Files.newBufferedWriter(filePath);
                    BufferedReader streamFromServerGetCommandToCache = new BufferedReader(new InputStreamReader(cacheSocketSendCommandToServer.getInputStream()));
                    String serverResponse;
                    while ((serverResponse = streamFromServerGetCommandToCache.readLine()) != null) {
                        //streamFromServerGetCommandToCache.ready();
                        //serverResponse = streamFromServerGetCommandToCache.readLine();
                        //(serverResponse = streamFromServerGetCommandToCache.readLine()) != null
                        System.out.println(serverResponse);
                        cachefileToWrite.write(serverResponse);
                        cachefileToWrite.write("\n");
                        //cachefileToWrite.flush();
                        //cachefileToWrite.close();
                        //cachefileToWrite.append(serverResponse);
                        //cachefileToWrite.append("\n");
                    }
                    cachefileToWrite.close();
                    BufferedReader fileToClient = Files.newBufferedReader(filePath);
                    StringBuilder fileData = new StringBuilder();
                    String line;
                    while ((line = fileToClient.readLine()) != null) {
                        System.out.println(fileData);
                        fileData.append(line);
                        fileData.append("\n");
                    }
                    //System.out.println(fileData);
                    fileToClient.close();
                    clientSocket.getOutputStream().write(fileData.toString().getBytes());
                    clientSocket.close();
                    //break;
                    continue;
                }
            }
        }
    }
    

    // -------------------------FUNCTION TO CREATE TWO SERVERS FOR THE GET COMMAND----------------------------------//
    public void tcpCreateTwoServersGetCommandSendFile(int serverPortNumber, String serverDirectory) throws IOException {
        // We will use a different port number from the one that receives the put command
        // and also different from the one that accepts file with put command from client.
        int serverPort = serverPortNumber + 100;
        // Creating socket to receive get command from cache
        ServerSocket serverSocket = new ServerSocket(serverPort);

        //System.out.println("Server 2 is ready for the client!");

        while (true) {
            System.out.println("Server 2 is ready for the client!");
            Socket cacheSendingGetRequest = serverSocket.accept();
            // Accepting the get command from the cache
            BufferedReader streamGetCommandFromCache = new BufferedReader(new InputStreamReader(cacheSendingGetRequest.getInputStream()));
            String message = streamGetCommandFromCache.readLine();
            // Extracting the get command and filename from cache's message
            String[] commandAndFile = tcpExtractCommandAndFile(message);
            String command = commandAndFile[0];
            String filename = commandAndFile[1];
            // Just a sanity check since we know that this port is reserved for cache's get command.
            if (command.equals("get")) {
                // Dynamically creating the full path to file
                Path filePath = Paths.get(serverDirectory, filename);
                // We need to send the file to the cache
                // PrintWriter streamFileToCache = new PrintWriter(cacheSendingGetRequest.getOutputStream(), true);
                BufferedReader fileToCache = Files.newBufferedReader(filePath);
                // We'll use string builder because strings are immutable in java
                StringBuilder fileData = new StringBuilder();
                String line;
                int counter = 0;
                while ((line = fileToCache.readLine()) != null) {
                    // Grabbing all of the lines from the file and adding them to a string
                    fileData.append(line).append("\n");
                    counter += 1;
                    System.out.println(counter);
                }
                // Sending the file to the cache
                //System.out.println(fileData);
                fileToCache.close();
                cacheSendingGetRequest.getOutputStream().write(fileData.toString().getBytes());
                cacheSendingGetRequest.close();
                continue;
            }
        }
    }
    
    //-----------------------FUNCTION TO RECEIVE THE GET COMMAND FROM THE CACHE---------------------------------//
    public void tcpCreateTwoServers(int serverPortNumber1, int serverPortNumber2, String serverDirectory, String serverTransportProtocol) throws Exception {
        int serverPortCommand = serverPortNumber1;
        int serverPortData = serverPortNumber2;
        ServerSocket serverSocketCommand = new ServerSocket(serverPortCommand);
        ServerSocket serverSocketData = new ServerSocket(serverPortData);
        // Setting the timeout
        serverSocketCommand.setSoTimeout(6000000);
        serverSocketData.setSoTimeout(6000000);


        // System.out.println("Server is ready for the client!");

        while (true) {
            System.out.println("Server is ready for the client!");
            // Waiting for client connection
            Socket clientSocket = serverSocketCommand.accept();
            // Reading the put filename command from client
            BufferedReader streamFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter streamToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            // Getting the put command from client
            String message = streamFromClient.readLine();
            System.out.println(message);
            // Extracting the command and filename
            String[] commandAndFile = tcpExtractCommandAndFile(message);
            String command = commandAndFile[0];
            String filename = commandAndFile[1];

            if (command.equals("put")) {
                // Creating the file to write to
                Path filePath = Paths.get(serverDirectory, filename);
                BufferedWriter f = Files.newBufferedWriter(filePath);
                // Sending response to client
                System.out.println("File created successfully!");
                String response = "File created successfully!";
                // Sending response to client
                f.write(response);
                streamToClient.println(response);
                f.close();
                //clientSocket.getOutputStream().write(response.getBytes());
                //break;
            }

            Path filePath = Paths.get(serverDirectory, filename);
            BufferedWriter fileToWrite = Files.newBufferedWriter(filePath);
            // clientSocket.setSoTimeout(100);
            // We will receive all of the data at once
            System.out.println("Reading file!");
            // System.out.println(streamFromClient.readLine());
            StringBuilder result = new StringBuilder();
            int counter = 0;
            String line; 
            while (true){
                //streamFromClient.ready()
                //(line = streamFromClient.readLine()) != null )
                line = streamFromClient.readLine();
                if (line == null){
                    break;
                }
                // System.out.println("Sending file to server!");
                System.out.println(line);
                //fileToWrite.append("\n");
                fileToWrite.write(line);
                fileToWrite.write("\n");
                fileToWrite.flush();
                //if (line == null){break;}
                // Flushing buffer
                //fileToWrite.flush();
                // fileToWrite.write(counter);
                //System.out.println(result.toString());
                //result.append(line);
                //counter += 1;
                //System.out.println(counter);
            }
            // String fileData = streamFromClient.readLine();
            //System.out.println(result.toString());
            //fileToWrite.write(line);
            //fileToWrite.write(line);
            //fileToWrite.write("\n");
            //fileToWrite.flush();
            fileToWrite.close();
            System.out.println("Outside for loop!");
            String responseUpload = "File uploaded successfully";
            //streamToClient.println(responseUpload);
            // Server sends response to client
            //PrintWriter streamResponseToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            clientSocket.getOutputStream().write(responseUpload.getBytes());
            //TimeUnit.SECONDS.sleep(10);
            clientSocket.close();
            continue;
            //streamToClient.println(response);
            //clientSocket.close();
            //break;
            // Server sends response to client
            /* 
            PrintWriter streamResponseToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            while (true){
                String fileData = streamFromClient.readLine();
                fileToWrite.write(fileData);
                fileToWrite.close();
                String response = "File uploaded to server!";
                // Server sends response to client
                PrintWriter streamResponseToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                // streamResponseToClient.println(response);
                // We only close socket once we've received the entire message
                if(fileData == null){
                    streamResponseToClient.println(response);
                    clientSocket.close();
                    break;
                }
            */
            //clientSocket.getOutputStream().write(response.getBytes());
            //clientSocket.close();
            
            /*
            while (true) {
                String fileData = streamFromClient.readLine();
                fileToWrite.write(fileData);
                fileToWrite.close();
                String response = "File uploaded to server!";
                clientSocket.getOutputStream().write(response.getBytes());
                clientSocket.close();
                // break;
                }
            */
            //if ()
        }
    }
    
    public void tcpUpload(String serverIp, int serverPortNumber, String path_name, Socket clientSocket ) throws IOException {
        String serverName = serverIp;
        int serverPort = serverPortNumber;
        //Socket clientSocket = new Socket(serverName, serverPort);
        //String message1 = fileToUpload;
        //System.out.println(message1);
        //System.out.println("Tcp upload!");
        // We need to send data to the server
        PrintWriter streamToServer = new PrintWriter(clientSocket.getOutputStream(), true);
        // Reading text file
        //streamToServer.println(message1);
        FileInputStream fstream = new FileInputStream(path_name);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String strLine;

        /**
        read file line by line
        */
        int counter = 0;
        /* 
        while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        System.out.println(strLine);
        streamToServer.println(strLine);
        counter += 1;
        System.out.println(counter);
        }
        */
        StringBuilder fileDataUpload = new StringBuilder();
        while ((strLine = br.readLine()) != null){
            fileDataUpload.append(strLine);
            fileDataUpload.append("\n");
            counter += 1;
            //System.out.println(counter);

        }
        //String nullString = null;
        //streamToServer.println(nullString);

        //Close the input stream
        in.close();
        //System.out.println("Outside loop client!");
        //String nullString = null;
        //streamToServer.println(nullString);
        
        clientSocket.getOutputStream().write(fileDataUpload.toString().getBytes());
        // To get the response from the server
        System.out.println("File successfully uploaded.");
        clientSocket.close();
        /* 
        BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //String serverResponse = streamFromServer.readLine();
        String serverResponse = streamFromServer.readLine();
        System.out.println(serverResponse);
        if (serverResponse.equals("File uploaded successfully")) {
            System.out.println("File uploaded to server!");
            clientSocket.close();
        }
        */
        //clientSocket.close();
    }
    
}
