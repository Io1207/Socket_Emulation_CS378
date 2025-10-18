import java.net.*;
// import java.nio.Buffer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;
import java.time.Clock;
import java.time.Instant;
// import java.time.ZoneId;
import java.util.stream.Collectors;

abstract class Nodes{
    int type;
    String id;
    int address;

    String repAsString(){
        return "Nodes represented as string";
    }
}

public class OracleNode extends Nodes{
    OracleNode(String name){
        type = 0;
        id = name;
    }

    @Override
    String repAsString(){
        return (type + " type node");
    }

    ServerSocket initSocket() throws IOException{
        ServerSocket serverSocket = new ServerSocket(5000); // Listen on port 500
        System.out.println("started, waiting for VN");
        return serverSocket;
    }
}

//write a function to read the input from the file (filename given as input) and return the content as a list of strings
class Readfile{
    List<String> readFile(String filename) throws IOException {
    List<String> lines = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.isEmpty() || line.charAt(0) == '#'){
            continue;
        }
        lines.add(line);
    }
    reader.close();
    return lines;
}
}

class PleaseWorkLikeAnON{

    static List<Quintuplet<String, Socket, String, Integer, PrintWriter>> clients = new ArrayList<>();
    static String nodeNames = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static int numVNs;
    static int time_poll = 60000; 

    static List<List<Integer>> extractAdjacencyMatrix(List<String> configFileContents){
        
        int n = configFileContents.size() + 1;

        //create an n x n adjacency matrix
        List<List<Integer>> adjMatrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjMatrix.add(new ArrayList<>(Collections.nCopies(n, 0)));
        }

        // i am given upper triangular matrix in the config file
        // I need to convert it to a full adjacency matrix
        for(int i = 0; i < n; i++) {
            String line = configFileContents.get(i);
            line = line.strip();
            String[] parts = line.split(" ");

            for(int j = n-1; j >= i; j--){
                if (i == j){
                    adjMatrix.get(i).set(i,0);
                    continue;
                }
                
                int weight = Integer.parseInt(parts[j-i-1]);
                if (i > j){
                    adjMatrix.get(i).set(j, weight);
                    adjMatrix.get(j).set(i, weight);
                }

                // adjMatrix.get(i).set(i + j,weight); // set the weight in the upper triangular part
                // adjMatrix.get(i + j).set(i,weight); // set the weight in the lower triangular part
            }
        }

        return adjMatrix;
    }

    static String encodedIP(Integer j){
        // take the 4 blocks of an ip adress separated by a .
        // convert each block to a 8 bit binary string
        // concatenate the 4 binary strings to get a 32 bit binary string
        StringBuilder binaryIP = new StringBuilder();

        String[] blocks = clients.get(j).getThird().split("\\.");

        for (String block : blocks) {
            String binaryBlock = Integer.toBinaryString(Integer.parseInt(block,2));
            while (binaryBlock.length() < 8) {
                binaryBlock = "0"  +  binaryBlock;
            }
            binaryIP.append(binaryBlock);
        }
        return binaryIP.toString();
    }

    static String encodedPort(Integer j){
        int port = clients.get(j).getFourth() ;
        String binaryPort = Integer.toBinaryString(port);
        while (binaryPort.length() < 16){
            binaryPort = "0" + binaryPort;
        }
        return binaryPort;
    }

    static String encodedWeight(Integer weight){
        String binaryWeight = Integer.toBinaryString(weight);
        while (binaryWeight.length() < 16){
            binaryWeight = "0" + binaryWeight;
        }
        return binaryWeight;
    }

    static List<String> readConfig(String filename) throws IOException{
        Readfile rf = new Readfile();

        // reading the file for the first time
        List<String> configLines = rf.readFile(filename);
        for(String line : configLines){
            System.out.println(line);
        }
        return configLines;
    }

    // create graph messages to be sent to VNs 
    static List<Quadruplet<String, String, String, String>> createMessages(List<List<Integer>> adjMatrix){
        List<Quadruplet<String, String, String, String>> messages = new ArrayList<>(adjMatrix.size());
        Quadruplet<String, String, String, String> placeholder = null;

        for(int i = 0; i < adjMatrix.size(); i++){
            for(int j = 0; j < adjMatrix.get(i).size(); j++){
                if(adjMatrix.get(i).get(j) >=  0){
                    placeholder = new Quadruplet<> (Integer.toBinaryString(j)+"", encodedIP(j), encodedPort(j), 
                                  encodedWeight(adjMatrix.get(i).get(j)));
                }
                messages.add(placeholder);
            }
        }
        return messages;
    }

    public static void main(String[] args) throws IOException {
        // oracle node, socket being initialised
        OracleNode x = new OracleNode("ON_1");
        ServerSocket serverSocket = x.initSocket();
        
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter config file name:");
        String filename = sc.nextLine();
        sc.close();

        // reading the file for the first time
        List<String> configLines = readConfig(filename);

        numVNs = configLines.size(); // starting with 0 connections
        int numConnections = 0;

        // make connections with all VNs (shoutout to Ram and Optiver)
        while (numConnections < numVNs){
            System.out.println("Waiting for vn connection...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("vn connected: "  +  clientSocket.getInetAddress());
            System.out.println("vn name: "  +  clientSocket.getInetAddress());
            
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String inputLine;
            String IP;
            int UDP_port_num;

            while ((inputLine = in.readLine()) != null) {
                System.out.println("vn says: "  +  inputLine);

                IP = Integer.toString(Integer.parseInt(inputLine.substring(0,8),2))  +  "."  +  
                     Integer.toString(Integer.parseInt(inputLine.substring(8,16),2))  +  "."  + 
                     Integer.toString(Integer.parseInt(inputLine.substring(16,24),2))  +  "."  + 
                     Integer.toString(Integer.parseInt(inputLine.substring(24,32),2));
                UDP_port_num = Integer.parseInt(inputLine.substring(32),2);

                out.println("on received: "  +  inputLine);
                if (inputLine.equals("stop")) {
                    break;
                }
            
                in.close();
                clients.add(new Quintuplet<> (nodeNames.charAt(numConnections) + "", clientSocket, IP, UDP_port_num, out));
                numConnections ++;
            }
        }

        System.out.println("All VNs connected.") ;

        // ------------------------------------------------------------
        // now going into the main loop of readin config file and send messages

        boolean run = true;
        List<String> messages = createMessages(extractAdjacencyMatrix(configLines)).stream().map(Quadruplet::toString).collect(Collectors.toList());
        PrintWriter out;

        
        for (String msg: messages){
            System.out.println("Message to be sent: " + msg);
        }

        System.exit(0);

        // sending neighbors for the first time
        for (int i=0; i < numConnections; i++){
            out = clients.get(i).fifth;
            for (String msg: messages){
                out.println(msg);
            }
        }
        
        Clock clock = Clock.systemUTC();
        Instant start=clock.instant();

        List<String> lastUpdatedConfig = configLines;

        while (run){
            long elapsedMillis = java.time.Duration.between(start, clock.instant()).toMillis();

            if (elapsedMillis >= time_poll){
                // reread the config file
                configLines = readConfig(filename);

                if (configLines.equals(lastUpdatedConfig)){

                    System.out.println("No changes in config file");
                    System.out.println("Starting sleep since no changes detected :((");
                    try {
                        // Pause execution
                        Thread.sleep(time_poll);
                    } catch (InterruptedException e) {
                        // Handle the case where the thread is interrupted while sleeping
                        System.err.println("Thread was interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt(); // Re-interrupt the thread
                    }
                    System.out.println("Resuming execution after sleep.");
                    start = clock.instant(); // reset the timer
                    continue;
                    
                }
                else {
                    // config changed — recompute and resend
                    System.out.println("Config file changed, recomputing...");
                    List<List<Integer>> adj = extractAdjacencyMatrix(configLines);
                    messages = createMessages(adj).stream()
                                                    .map(Quadruplet::toString)
                                                    .collect(Collectors.toList());

                    for (int i = 0; i < numConnections; i++) {
                        out = clients.get(i).getFifth();
                        for (String msg : messages) {
                            out.println(msg);
                        }
                    }

                    lastUpdatedConfig = new ArrayList<>(configLines);
                }

                // reset timer
                start = clock.instant();
            }

            // optional: sleep a tiny bit to reduce CPU usage
            try { Thread.sleep(200); } catch (InterruptedException e) { break; }
        }
    }
}
//                 lastUpdatedConfig = configLines;
//                 messages = createMessages(extractAdjacencyMatrix(configLines)).stream().map(Quadruplet::toString).collect(Collectors.toList());

//                 // sending neighbors again
//                 for (int i=0; i < numConnections; i++){
//                     out = clients.get(i).fifth;
//                     for (String msg: messages){
//                         out.println(msg);
//                     }
//                 }
//                 start = clock.instant(); // reset the timer
//             }
//         }
//     }
// }