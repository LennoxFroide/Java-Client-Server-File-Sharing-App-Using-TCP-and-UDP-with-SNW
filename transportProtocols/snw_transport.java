package transportProtocols;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class snw_transport {
    private static final String clientDirectory = "client_fl";
    //----------------------METHOD TO EXTRACT THE COMMAND AND FILE NAME FROM CMD--------------------------------------//
    // List <>
    public static String[] snw_extract_command_and_file(String string) {
        String[] actionArray = string.split(" ");
        if (actionArray.length <= 1 || actionArray.length > 2) {
            throw new IllegalArgumentException("Error in taking in commands!");
        } else if (actionArray.length == 2) {
            String action = actionArray[0].toLowerCase();
            String filename = actionArray[1];
            return new String[]{action, filename};
            //return Arrays.asList(action, filename);
        } else {
            return null;
            // return new ArrayList<>();
        }
    }
    //----------------------METHOD TO MAKE OUR FILE DATA INTO CHUNKS-----------------------------------------------//
    public static List<Object> chunkData(String directory, String file) throws IOException {
        // Creating an array to hold our chunks
        List<String> arrayData = new ArrayList<>();
        // Creating a path for any filename and directory pairs passed into the method
        String file_path = directory + File.separator + file;
        // Then we're reading the data from the text file into the fileData variable
        BufferedReader fileData = new BufferedReader(new FileReader(file_path));
        // String builder helps us to work with the immutable string data types in python
        StringBuilder fileContent = new StringBuilder();
        String line;
        while ((line = fileData.readLine()) != null) {
            fileContent.append(line);
        }
        // Closing the file after grabbing all the data
        fileData.close();
        // Getting the byte size of fileContent variable
        int sizeBytes = fileContent.toString().getBytes("UTF-8").length;

        fileData = new BufferedReader(new FileReader(file_path));
        while (true) {
            // Setting a max number of bytes to read at 1000bytes
            char[] buffer = new char[1000];
            // Grabbing a chunk
            int charsRead = fileData.read(buffer);
            // Ince we get to the end of the file we will break
            if (charsRead == -1) {
                // Having grabbed all chunks
                fileData.close();
                break;
            }
            // Adding all of the chunks into an array
            arrayData.add(new String(buffer, 0, charsRead));
        }
        // arrayData.add(new String(buffer, 0, charsRead));
        return Arrays.asList(sizeBytes, arrayData);
    }
    //------------------------METHOD TO SEND THE PUT COMMAND TO THE SERVER----------------------//
    public void snw_put_command(String serverIp, int serverPortNumber, String putCommand) throws IOException {
        String serverName = serverIp;
        int serverPort = serverPortNumber;
        // Linking the client socket to the server socket to send the put filename command
        Socket clientSocket = new Socket(serverName, serverPort);
        // Having a stream to send put command to server
        PrintWriter streamSendCommandToServer= new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader streamDataFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // Sending the actual command
        streamSendCommandToServer.println(putCommand);
        // A channel to grab data from server
        String serverResponse = streamDataFromServer.readLine();
        // Closing the tcp socket since server got the command
        if (serverResponse.equals("File created successfully!")) {
            System.out.println("Server did it!");
            clientSocket.close();
        } else {
            System.out.println("Server side error!\n");
            System.out.println(serverResponse);
        }
    }

    //-------------------METHOD TO SEND ACTUAL DATA TO THE SERVER-----------------------------------------------------//
    public void snw_upload(String serverIp, int serverPortNumber, String directory, String file) throws IOException {
        String serverName = serverIp;
        int serverPort = serverPortNumber;
        // Creating a UDP socket to send file to server using snw
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(600000);
        //InetAddress serverAddress = InetAddress.getByName(serverName);
        // Chunking the data
        List<Object> chunkDataResult = chunkData(directory, file);
        // Byte size of the file
        int fileByteSize = (int) chunkDataResult.get(0);
        // An array containg the chunks to be sent
        List<String> fileChunkArray = (List<String>) chunkDataResult.get(1);
        // Converting fileByteSize to a string and concatenating it with LEN:
        String stringFileSize = "LEN:" + Integer.toString(fileByteSize);
        //System.out.println(stringFileSize);
        // Preparing data to be send via UDP socket
        byte[] lengthToSendToServer = stringFileSize.getBytes();
        // String sendData = stringFileSize;
        // Creating the UDP packet
        DatagramPacket packetToSendToServer = new DatagramPacket(lengthToSendToServer, lengthToSendToServer.length, InetAddress.getByName(serverName), serverPort);
        // Sending the packet containing LEN: Bytes
        clientSocket.send(packetToSendToServer);
        // Indicate that LEN has been sent
        //System.out.println("I have sent LEN!");

        for (String chunk : fileChunkArray) {
            // Grabbing the current chunk
            //System.out.println("Sending chunk!");
            //System.out.println(chunk);
            byte[] chunkToSend = chunk.getBytes("UTF-8");
            // Making the chunk into a packet
            DatagramPacket sendPacket = new DatagramPacket(chunkToSend, chunkToSend.length, InetAddress.getByName(serverName), serverPort);
            // Sending the packet to the server
            clientSocket.send(sendPacket);
            // Receiving responses from the server
            byte[] receiveServerResponse = new byte[2048];
            DatagramPacket receiveServerPacket = new DatagramPacket(receiveServerResponse, receiveServerResponse.length);
            clientSocket.receive(receiveServerPacket);
            // Decoding the server's packet to get the server's message
            String serverResponse = new String(receiveServerPacket.getData(), 0, receiveServerPacket.getLength());
            // Assesing the server's response to know whether to continue or close connection
            if (serverResponse.equals("ACK")) {
                continue;
            } else if (serverResponse.equals("FIN!")) {
                System.out.println("Server Response: File successfully uploaded.");
                //clientSocket.close();
                break;
            }else{

            }
        }
    }

    //------------------------METHOD TO CREATE SOCKETS TO RECEIVE PUT COMMAND AND FILE FROM CLIENT--------------------//
    public void snw_create_two_servers(int serverPortNumber1, int serverPortNumber2, String serverDirectory, String serverTransportProtocol) throws IOException {
        // One socket is tcp to receive put command from client
        int serverPortCommand = serverPortNumber1;
        // The other socket is a UDP socket to send the actual file to the client
        int serverPortData = serverPortNumber2;
        // Creating the two sockets
        ServerSocket serverSocketCommand = new ServerSocket(serverPortCommand);// TCP Socket 
        //DatagramSocket serverSocketData = new DatagramSocket(serverPortData);// UDP Socket

        //System.out.println("Server is ready for the client!");

        while (true) {
            System.out.println("Server is ready for the client!");
            // Waiting for client to send put command
            Socket clientSocket = serverSocketCommand.accept();
            // Using TCP convention here because commands are sent via TCP
            BufferedReader streamPutCommandFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter streamTCPMessageToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            // Reading the command from the client
            String message = streamPutCommandFromClient.readLine();
            String[] commandAndFilename = snw_extract_command_and_file(message);
            String command = commandAndFilename[0];
            String filename = commandAndFilename[1];
            // Assessing our command
            if (command.equals("put")) {
                // Creating file to write the client's data to
                String file_path = serverDirectory + File.separator + filename;
                FileWriter file = new FileWriter(file_path);
                System.out.println("File created successfully!");
                System.out.println("File created successfully!");
                // We need to send a response to client
                String response = "File created successfully!";
                // Sending response to client
                streamTCPMessageToClient.println(response);
                file.close();
                // Added this to close file
                //file.close();
                //break;
            }
        // This will receive the LEN: ByteSize from client
        DatagramSocket serverSocketData = new DatagramSocket(serverPortData);// UDP Socket
        serverSocketData.setSoTimeout(60000);
        byte[] receiveLenData = new byte[1024];
        while (true) {
            System.out.println("Getting length");
            // This should contain LEN message
            //DatagramSocket receiveLenString = new DatagramSocket(se) 
            // TODO MOVE THIS OUTSIDE WHILE LOOP
            DatagramPacket receiveLenByteSizePacket = new DatagramPacket(receiveLenData, receiveLenData.length);
            // Receiving data from client
            serverSocketData.receive(receiveLenByteSizePacket);
            // Extracting the message
            String lengthMessage = new String(receiveLenByteSizePacket.getData(), 0, receiveLenByteSizePacket.getLength());
            // Using : as the delimiter of LEN and sizeByte
            String[] lengthArray = lengthMessage.split(":");
            // Converting string size to an integer
            int integerSizeBytes = Integer.parseInt(lengthArray[1]);
            System.out.println(integerSizeBytes);
            //break;
            System.out.println("We're past printinng byte size!");
            // Getting ready to receive chunks from client
            int runningChunkSize = 0;// a counter for chunck received so far
            String file_path = serverDirectory + File.separator + filename;
            // Accessing the file to write to
            FileWriter fileToWrite = new FileWriter(file_path, true);
            while (true) {
                System.out.println("Getting data!");
                // Getting the chunks
                DatagramPacket receivePacket = new DatagramPacket(new byte[1000], 1000);
                serverSocketData.receive(receivePacket);
                // Decoding the chunks
                String fileData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println(fileData.getBytes("UTF-8").length);
                // Updating the counter for our base case
                runningChunkSize += 1000;
                fileToWrite.append(fileData);
                fileToWrite.append("\n");
                // Preparing the ACk message to send to client 
                String acknowledgement = "ACK";
                byte[] sendACKResponseToClient = acknowledgement.getBytes();
                DatagramPacket sendACKResponse = new DatagramPacket(sendACKResponseToClient, sendACKResponseToClient.length, receivePacket.getAddress(), receivePacket.getPort());
                serverSocketData.send(sendACKResponse);
                // We might have to use append here!
                // fileToWrite.append(fileData);
                // fileToWrite.append("\n");
                if (runningChunkSize >= integerSizeBytes) {
                    System.out.println("I'm done!");
                    acknowledgement = "FIN!";
                    // We need to send FIN to the client
                    // Preparing for the send
                    byte[]  finMessage = acknowledgement.getBytes();
                    DatagramPacket finPacketToClient = new DatagramPacket(finMessage, finMessage.length, receivePacket.getAddress(), receivePacket.getPort());
                    serverSocketData.send(finPacketToClient);
                    fileToWrite.append(fileData);
                    //fileToWrite.flush();
                    fileToWrite.close();
                    serverSocketData.close();
                    break;
                // This is just a sanity check
                } else if (fileData.isEmpty()) {
                    // Sending the sanity check to the client
                    acknowledgement = "Packets were lost!";
                    byte [] sanityCheck = acknowledgement.getBytes();
                    DatagramPacket sanityCheckPacket = new DatagramPacket(sanityCheck, sanityCheck.length, receivePacket.getAddress(), receivePacket.getPort());
                    serverSocketData.send(sanityCheckPacket);
                    break;
                } else {
                
                }
                //break;
            }
            break;
        }
    }
    }
    //------------------------METHOD TO CREATE 2 SOCKETS TO RECEIVE GET COMMAND FROM CACHE------------------------/
    public void snw_create_two_servers_get_command_send_file(int serverPortNumber, int serverUDPPort, String serverDirectory) throws IOException {
        // Getting the port number to receive get command from cache
        int serverPort = serverPortNumber + 100;
        ServerSocket serverSocket = new ServerSocket(serverPort);// This is a tcp socket
        DatagramSocket serverSocketData = new DatagramSocket(serverUDPPort);

        //System.out.println("Server 2 is ready for the client!");

        while (true) {
            System.out.println("Server 2 is ready for the client!");
            // Grabbing a get message from the cache
            Socket cacheSendingGetRequest = serverSocket.accept();
            // To work with the tcp server socket
            BufferedReader streamCommandFromCache = new BufferedReader(new InputStreamReader(cacheSendingGetRequest.getInputStream()));
            PrintWriter streamToCache = new PrintWriter(cacheSendingGetRequest.getOutputStream(), true);
            // Getting cache command
            String message = streamCommandFromCache.readLine();
            String[] commandAndFilename = snw_extract_command_and_file(message);
            String command = commandAndFilename[0];
            String filename = commandAndFilename[1];

            if (command.equals("get")) {
                // We need to send message chunks to the cache
                List<Object> chunkDataResult = chunkData(serverDirectory, filename);
                int fileByteSize = (int) chunkDataResult.get(0);
                List<String> fileChunkArray = (List<String>) chunkDataResult.get(1);
                String stringFileSize = "LEN:" + fileByteSize;
                // LEN is sent via serverSocket
                //PrintWriter streamLenToClient = new PrintWriter(serverSocket.getOutputStream(), true);
                // Using declared print write to send len to cache
                streamToCache.println(stringFileSize);
                //out.println(stringFileSize);
                System.out.println("I have sent LEN!");

                while (true) {
                    // Cache will send Ready! so that we have it's address
                    // This is UDP
                    byte[] readyMessage = new byte[1024];
                    // Listening
                    DatagramPacket receiveReadyPacket = new DatagramPacket(readyMessage, readyMessage.length);
                    // Receiving message
                    // Socket cacheFileRequest = serverSocketData.receive();
                    serverSocketData.receive(receiveReadyPacket);
                    // Extracting the string
                    String clientReadyMessage = new String(receiveReadyPacket.getData(), 0, receiveReadyPacket.getLength());
                    if (clientReadyMessage != null) {
                        System.out.println("Cache has sent ready!");
                        //break;
                    }
                    /* 
                    else{
                        System.out.println("Still waiting for ready from cache!");
                    }
                    */

                    System.out.println("Cache is ready to receive file from server!");
                    // byte[] chunkToCache = new byte[1000];
                    for (String chunk : fileChunkArray) {
                        // Convering chunk to required format
                        byte[] chunkToCache = chunk.getBytes();
                        // Packeting it
                        DatagramPacket sendChunkPacket = new DatagramPacket(chunkToCache, chunkToCache.length, receiveReadyPacket.getAddress(), receiveReadyPacket.getPort());
                        serverSocketData.send(sendChunkPacket);
                        // Waiting for acknowledgement from cache
                        byte[] receiveCacheACK = new byte[2048];
                        DatagramPacket receiveACKPacket = new DatagramPacket(receiveCacheACK, receiveCacheACK.length);
                        serverSocketData.receive(receiveACKPacket);
                        String cacheResponse = new String(receiveACKPacket.getData(), 0, receiveACKPacket.getLength());

                        if (cacheResponse.equals("ACK")) {
                            continue;
                        } else if (cacheResponse.equals("FIN!")) {
                            System.out.println("File sent to cache successfully!");
                            //cacheFileRequest.close();
                            // continue;
                            break;
                        }
                    }
                    break;
                } 
                continue;
            }
        }
    
    }
    //------------------METHOD TO RECEIVE GET COMMAND FROM THE CLIENT TO CACHE----------------------------------//
    //, DatagramSocket cacheSNWSocket
    public void snw_create_two_caches_get_command(int cachePortNumber, int cachePortTwo, String cacheDirectory, String serverIp, int serverPortNumber, DatagramSocket cacheSNWSocket) throws IOException {
        int cachePortGetCommand = cachePortNumber;// Receives TCP from client
        int serverPortSendGet = serverPortNumber + 100;// Send TCP GET TO SERVER 
        int serverPortRecvServer = serverPortNumber + 1001; //  Server side port to send file to cache
        String serverSendGetName = serverIp;

        // Receives get command from client
        ServerSocket cacheSocketGetCommand = new ServerSocket(cachePortGetCommand);//TCP
        // Sends get command to server
        Socket cacheSocketSendCommandToServer = new Socket(serverSendGetName, serverPortSendGet);//TCP
        // Need a buffer reader and printwriter for server communication
        BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(cacheSocketSendCommandToServer.getInputStream()));
        PrintWriter streamGetToServer = new PrintWriter(cacheSocketSendCommandToServer.getOutputStream(), true);
        // Binding to listen for data from server over snw
        //cacheSNWSocket.bind(new InetSocketAddress(cachePortNumber + 100));
        // DatagramSocket cacheSocketReceiveFileFromServer = new DatagramSocket(serverPortRecvServer);// UDP RECEIVE FILE FROM SERVER
        //DatagramSocket cacheSNWSocket = new DatagramSocket();
        // System.out.println("Here1");
        //System.out.println("Cache is ready for the client!");
        //cacheSNWSocket.bind('', cachePortNumber+100);

        while (true) {
            System.out.println("Cache is ready for the client!");
            // TCP Socket to accept get command from client
            Socket clientSocket = cacheSocketGetCommand.accept();
            // To read command
            BufferedReader receiveGetCommand = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter SendTCPToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            // Getting the get command from the client
            String message = receiveGetCommand.readLine();
            // Extracting the command and the filename
            String[] commandAndFilename = snw_extract_command_and_file(message);
            String command = commandAndFilename[0];
            String filename = commandAndFilename[1];

            if (command.equals("get")) {
                // Creating a path in order to check for a file
                String file_path = cacheDirectory + File.separator + filename;
                // Going to the server
                if (!new File(file_path).exists()) {
                    // Sending message to server get message
                    streamGetToServer.println(message);
                    // I COMMENTED THESE OUT
                    // cacheSocketSendCommandToServer.connect(new InetSocketAddress(serverSendGetName, serverPortSendGet));
                    //cacheSocketSendCommandToServer.getOutputStream().write(message.getBytes());
                    /* 
                    while (true) {
                        // THIS IS STILL TCP
                        // Server will be sending length to cache
                        System.out.println("Getting length");
                        // Grabbing the length
                        String fileData = streamFromServer.readLine();
                        //byte[] receiveServerData = new byte[2048];
                        //DatagramPacket receiveServerLength = new DatagramPacket(receiveServerData, receiveServerData.length);
                        //cacheSocketSendCommandToServer.receive(receiveServerLength);
                        //String fileData = new String(receiveServerLength.getData(), 0, receiveServerLength.getLength());
                        String[] lengthArray = fileData.split(":");
                        int integerSizeBytes = Integer.parseInt(lengthArray[1]);
                        System.out.println(integerSizeBytes);
                        break;
                    }*/
                    System.out.println("Getting length");
                    // Grabbing the length
                    //while(true){
                    String fileData = streamFromServer.readLine();
                    String[] lengthArray = fileData.split(":");
                    int integerSizeBytes = Integer.parseInt(lengthArray[1]);
                    System.out.println(integerSizeBytes);
                    
                    // We have to receive the file through a UDP connection
                    // WE HAVE TO CLOSE THIS UDP SOCKET TO AVOID ERRORS
                    //cacheSocketReceiveFileFromServer.connect(new InetSocketAddress(serverSendGetName, serverPortRecvServer));
                    // THIS IS A CLIENT SOCKET TO RECEIVE FILE FROM SERVER
                    //DatagramSocket cacheSocketReceiveFileFromServer = new DatagramSocket(serverPortRecvServer);// UDP RECEIVE FILE FROM SERVER
                    DatagramSocket cacheSocketReceiveFileFromServer = new DatagramSocket();
                    String initializationMessage = "Ready!";
                    // Preparing message to send
                    byte[] initializationData = initializationMessage.getBytes();
                    // SEND READY MESSAGE !!!!
                    // Making it into a packet
                    DatagramPacket initializationDataPacket = new DatagramPacket(initializationData, initializationData.length, InetAddress.getByName(serverSendGetName), serverPortRecvServer);
                    //cacheSocketReceiveFileFromServer.send(new DatagramPacket(initializationMessage.getBytes(), initializationMessage.length(), new InetSocketAddress(serverSendGetName, serverPortRecvServer)));
                    cacheSocketReceiveFileFromServer.send(initializationDataPacket);
                    int runningChunkSize = 0;
                    // Creating destination file and opening it
                    FileWriter fileToWrite = new FileWriter(file_path, true);
                    // Preparing to receive server response
                    byte[] receiveChunkData = new byte[1000];
                    while (true) {
                        System.out.println("Getting data!");
                        // byte[] receiveChunkData = new byte[1000];
                        // Listening for server data 
                        DatagramPacket receiveChunkPacket = new DatagramPacket(receiveChunkData, receiveChunkData.length);
                        cacheSocketReceiveFileFromServer.receive(receiveChunkPacket);
                        String fileChunkMessage = new String(receiveChunkPacket.getData(), 0, receiveChunkPacket.getLength());
                        // Getting the address of the server sending data
                        //InetAddress serverAddressChunk = receiveChunkPacket.getAddress();
                        // Getting port of the server
                        //int serverPortChunking = receiveChunkPacket.getPort();

                        // System.out.println(fileChunkMessage.getBytes("UTF-8").length);
                        runningChunkSize += 1000;
                        // HAving received chunk we need to respond to server
                        fileToWrite.write(fileChunkMessage); 
                        fileToWrite.write("\n");
                        String acknowledgement = "ACK";
                        byte[] sendAckToServer = acknowledgement.getBytes();
                        DatagramPacket sendAckToServerPacket = new DatagramPacket(sendAckToServer, sendAckToServer.length, InetAddress.getByName(serverSendGetName), serverPortRecvServer);
                        cacheSocketReceiveFileFromServer.send(sendAckToServerPacket);
                        // Might need to be an append or while loop
                        // fileToWrite.write(fileChunkMessage); 
                        // fileToWrite.write("\n");
                        if (runningChunkSize >= integerSizeBytes) {
                            System.out.println("I'm done!");
                            acknowledgement = "FIN!";
                            // Crafting the ACK to send to the server
                            byte[] sendFinMessage = acknowledgement.getBytes();
                            DatagramPacket finACKToServer = new DatagramPacket(sendFinMessage, sendFinMessage.length, InetAddress.getByName(serverSendGetName), serverPortRecvServer);
                            cacheSocketReceiveFileFromServer.send(finACKToServer);
                            // Writing last bits
                            fileToWrite.write(fileData);
                            fileToWrite.close();
                            cacheSocketReceiveFileFromServer.close();
                            break;
        
                        } else if (fileData.isEmpty()) {
                            acknowledgement = "Packets were lost!";
                            byte[] packageLost = acknowledgement.getBytes();
                            DatagramPacket packageLostPacket = new DatagramPacket(packageLost, packageLost.length, InetAddress.getByName(serverSendGetName), serverPortRecvServer);
                            cacheSocketReceiveFileFromServer.send(packageLostPacket);
                            break;
                        } else {
                            continue;
                        }
                    }
                
                }
                // WE COULD HAVE THIS IN AN ELSE
                // Sending data to the client
                while (true) {
                    // Receiving Ready! from client
                    byte[] receiveClientReady = new byte[1024];
                    // Listening
                    DatagramPacket receiveClientReadyMessage = new DatagramPacket(receiveClientReady, receiveClientReady.length);
                    // Actually receiving message
                    cacheSNWSocket.receive(receiveClientReadyMessage);
                    // Stringifying message
                    String clientMessage = new String(receiveClientReadyMessage.getData(),0,receiveClientReadyMessage.getLength());

                    //Socket clientSocketSNW = cacheSNWSocket.accept();
                    // BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocketSNW.getInputStream()));
                    // PrintWriter clientOut = new PrintWriter(clientSocketSNW.getOutputStream(), true);

                    //String clientResponse = clientIn.readLine();
                    // Checking if the server is ready.
                    if (clientMessage.equals("Ready!")) {
                        System.out.println("we have a connection to client SNW socket!");
                    }
                
                    // Grabbing array of chunks
                    List<Object> chunkDataResult = chunkData(cacheDirectory, filename);
                    int fileByteSize = (int) chunkDataResult.get(0);
                    List<String> fileChunkArray = (List<String>) chunkDataResult.get(1);

                    String stringFileSize = "LEN:" + fileByteSize;
                    // Preparing LEN
                    byte[] stringFileData = stringFileSize.getBytes();
                    // Makiing a packet and sending LEN
                    DatagramPacket stringfileDataPacket = new DatagramPacket(stringFileData, stringFileData.length, receiveClientReadyMessage.getAddress(), receiveClientReadyMessage.getPort());
                    cacheSNWSocket.send(stringfileDataPacket);
                    //clientSocketSNW.getOutputStream().write(stringFileSize.getBytes());
                    System.out.println("I have sent LEN!");
                    // Sending actual chunks
                    for (String chunk : fileChunkArray) {
                        byte[] sendChunkData = chunk.getBytes("UTF-8");
                        // clientSocketSNW.getOutputStream().write(sendData);
                        // Preparing a packet // THE READY MESSAGE HELPS US TO KNOW THE CLIENT"S IP AND PORT NUMBER
                        DatagramPacket sendChunkPacket = new DatagramPacket(sendChunkData, sendChunkData.length, receiveClientReadyMessage.getAddress(), receiveClientReadyMessage.getPort());
                        // Sending chunk
                        cacheSNWSocket.send(sendChunkPacket);
                        // Grabbing client response
                        byte[] clientResponse = new byte[1024];
                        DatagramPacket clientResponsePacket = new DatagramPacket(clientResponse, clientResponse.length);
                        // BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocketSNW.getInputStream()));
                        // Grabbing client response
                        cacheSNWSocket.receive(clientResponsePacket);
                        // Extracting the message
                        String clientResponseString = new String(clientResponsePacket.getData(), 0, clientResponsePacket.getLength());

                        if (clientResponseString.equals("ACK")) {
                            continue;
                        } else if (clientResponseString.equals("FIN!")) {
                            System.out.println("File sent to Client!");
                            clientSocket.close();
                            break;
                        }
                        else {
                            ;
                        }
                    }
                    /*
                    System.out.println("We're outside for loop after client send!");
                    byte[] clientResponse = new byte[1024];

                    while(true){
                        DatagramPacket clientResponsePacket = new DatagramPacket(clientResponse, clientResponse.length);
                        // BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocketSNW.getInputStream()));
                        // Grabbing client response
                        cacheSNWSocket.receive(clientResponsePacket);
                        String clientResponseStringFIN = new String(clientResponsePacket.getData(), 0, clientResponsePacket.getLength());
                        System.out.println(clientResponseStringFIN);
                        // String clientResponseFIN = cacheSNWSocket.readLine();
                        if (clientResponseStringFIN.equals("FIN!")) {
                            System.out.println("File sent to client!");
                            break;
                            //client.close();
                            }
                        else {// We don't do anything here
                            ;
                            }
                    }
                    break;
                    */
                    // We don't get to FIN
                    /*
                    byte[] clientResponseFIN = new byte[1024];
                    DatagramPacket clientResponsePacketFIN = new DatagramPacket(clientResponseFIN, clientResponseFIN.length);
                    cacheSNWSocket.receive(clientResponsePacketFIN);
                    
                    // BufferedReader clientStreamIn = new BufferedReader(new InputStreamReader(cacheSocketSNW.getInputStream()));
                    // Extracting the message
                    String clientResponseStringFIN = new String(clientResponsePacketFIN.getData(), 0, clientResponsePacketFIN.getLength());
                    System.out.println(clientResponseStringFIN);
                    // String clientResponseFIN = cacheSNWSocket.readLine();
                    if (clientResponseStringFIN.equals("FIN!")) {
                        System.out.println("File sent to client!");
                        break;
                        //client.close();
                        }
                    else {// We don't do anything here
                        ;
                    }
                    break;
                    */
                    //break;
                }
                //continue;
            }
            else {
                ;
            }
            continue;
        }// This is the main loop
    
    }
    //---------------------------METHOD TO SEND THE GET COMMAND---------------------------------------------//
    
    
    public void snw_get_command(String clientDirectory, String cacheIp, int cachePortNumber, String serverIp, int serverPortNumber, String getCommand, DatagramSocket clientSocketSNW) throws IOException {
        // Getting cache ip and port number
        String cacheName = cacheIp;
        int cachePort = cachePortNumber;
        String serverName = serverIp;
        int serverPort = serverPortNumber;

        // Creating a client socket TCP to send get command
        Socket clientSocket= new Socket(cacheName, cachePort);
        PrintWriter streamGetToCache = new PrintWriter(clientSocket.getOutputStream(), true);
        // Sending get command
        streamGetToCache.println(getCommand);
        // To create file in client
        String message = getCommand;
        String[] commandAndFile = snw_extract_command_and_file(getCommand);
        String filename = commandAndFile[1];
        clientSocket.getOutputStream().write(message.getBytes());
        String file_path = clientDirectory + File.separator + filename;
        // Path filePath = Paths.get(clientDirectory, filename);

        // Getting the file to write data to ready
        FileWriter fileToWrite = new FileWriter(file_path);
        //System.out.println("File created successfully!");
        // BufferedWriter fileToWrite = Files.newBufferedWriter(file_Path);

        // Prechecking where the file lives
        String cacheDirectory = "cache_fl";
        String serverDirectory = "server_fl";
        //String pathName = Paths.get(myClient.path, filename).toString();
        Path filePathCache = Paths.get(cacheDirectory, filename);
        Path filePathServer = Paths.get(serverDirectory, filename);

        String sourceMessage;
        //File(file_path).exists()
        if (Files.exists(filePathCache)) {
            sourceMessage = "Cache Respone: File delivered from cache.";
        } else if (Files.exists(filePathServer)) {
            sourceMessage = "Server Respone: File delivered from server.";
        } else {
            sourceMessage = "Error: File is neither in cache nor server!";
        }

        String initializationMessage = "Ready!";
        byte[] initialisationByte = initializationMessage.getBytes();
        // Check port number
        DatagramPacket initialisationBytePacket = new DatagramPacket(initialisationByte, initialisationByte.length, InetAddress.getByName(cacheName), cachePort + 100); 
        clientSocketSNW.send(initialisationBytePacket);
        // Receiving response from cache UDP
        byte[] receiveFileFromCache = new byte[1024];
        DatagramPacket receiveFilePacket = new DatagramPacket(receiveFileFromCache, receiveFileFromCache.length);


        //clientSocketSNW.getOutputStream().write(initializationMessage.getBytes());

        // Getting the message
        // BufferedReader streamFileFromCache = new BufferedReader(new InputStreamReader(clientSocketSNW.getInputStream()));
        String fileSize;
        int integerSizeBytes = 0;
        while (true) {
            //System.out.println("Getting length");
            clientSocketSNW.receive(receiveFilePacket);
            // Getting the contents
            String cacheResponseLen = new String(receiveFilePacket.getData(), 0, receiveFilePacket.getLength());
            fileSize = cacheResponseLen;
            if (fileSize != null) {
                String[] lengthArray = fileSize.split(":");
                integerSizeBytes = Integer.parseInt(lengthArray[1]);
                //System.out.println(integerSizeBytes);
                break;
            }
            // DEAD CODE
            /* 
            else{
                System.out.println("'Error in receiving file length!'");
                break;
            }
            */
        }

        int runningChunkSize = 0;
        //  fileToWrite = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND);
        while (true) {
            //System.out.println("Getting data!");
            // Receiving chunk
            clientSocketSNW.receive(receiveFilePacket);
            String fileData = new String(receiveFilePacket.getData(), 0, receiveFilePacket.getLength());
            //String fileData = new String(cacheResponseLen.get)
            // streamFileFromCache.readLine();
            // System.out.println(fileData.getBytes().length);

            runningChunkSize += 1000;
            fileToWrite.write(fileData);
            fileToWrite.write("\n");
            String acknowledgement = "ACK";
            // clientSocketSNW.getOutputStream().write(acknowledgement.getBytes());
            // Sending ACK to server
            byte[] ackMessage = acknowledgement.getBytes();
            DatagramPacket sendACKPacket = new DatagramPacket(ackMessage, ackMessage.length, InetAddress.getByName(cacheName), cachePort + 100); 
            clientSocketSNW.send(sendACKPacket);
            //fileToWrite.write(fileData);
            //fileToWrite.write("\n");
            // String remaining = acknowledgement.

            if (runningChunkSize >= integerSizeBytes) {
                String responseFin = "FIN!";
                byte[] finMessage = responseFin.getBytes();
                DatagramPacket sendFINPacket = new DatagramPacket(finMessage, finMessage.length, InetAddress.getByName(cacheName), cachePort + 100);
                //System.out.println(finMessage);
                clientSocketSNW.send(sendFINPacket);
                //clientSocketSNW.getOutputStream().write(response.getBytes());
                // Getting last chunk
                //fileToWrite.write(fileData);
                fileToWrite.close();
                break;
            }
            /* 
            } 
            else if (fileData == null) {
                String responseLost = "Packets were lost!";
                byte[] responseMessage = responseLost.getBytes();
                DatagramPacket sendResponsePacket = new DatagramPacket(responseMessage, responseMessage.length, InetAddress.getByName(cacheName), cachePort);
                clientSocketSNW.send(sendResponsePacket);
                //clientSocketSNW.getOutputStream().write(response.getBytes());
                break;
            }
            */
        }
        System.out.println(sourceMessage);
    }
}
    
    

