import java.net.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

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
        type=0;
        id=name;
    }

    @Override
    String repAsString(){
        return (type+" type node");
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
        if (line.isEmpty() || line.charAt(0)=='#'){
            continue;
        }
        lines.add(line);
    }
    reader.close();
    return lines;
}
}

class WorkLikeAnON{
    List<List<Integer>> extractAdjacencyMatrix(String configFileContents){
        int n=configFileContents.length();
        //create an n x n adjacency matrix
        List<List<Integer>> adjMatrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjMatrix.add(new ArrayList<>(Collections.nCopies(n, 0)));
        }

        // i am given upper triangular matrix in the config file
        // I need to convert it to a full adjacency matrix
        for(int i=0;i<n;i++){
            String line=configFileContents.get(i);
            String[] parts=line.split(" ");
            for(int j=0;j<parts.length;j++){
                int weight=Integer.parseInt(parts[j]);
                adjMatrix.get(i).set(i+j,weight); // set the weight in the upper triangular part
                adjMatrix.get(i+j).set(i,weight); // set the weight in the lower triangular part
            }
        }

        return adjMatrix;
    }

    public static void main(String[] args) throws IOException {
        // oracle node, socket being initialised
        OracleNode x = new OracleNode("ON_1");
        ServerSocket serverSocket = x.initSocket();
        
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter config file name:");
        String filename=sc.nextLine();

        Readfile rf = new Readfile();

        // reading the file for the first time
        List<String> configLines = rf.readFile(filename);
        for(String line : configLines){
            System.out.println(line);
        }

        boolean run=true;
        int maxConnections=configLines.size(); // starting with 0 connections
        int numConnections=0;
        String nodeNames="ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        List<Pair<Socket,String>> clients=new ArrayList<>();

        // make connections with all VNs (shoutout to Ram and Optiver)
        while(numConnections<maxConnections){
            System.out.println("Waiting for vn connection...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("vn connected: " + clientSocket.getInetAddress());
            clients.add(new Pair<> (clientSocket, nodeNames.charAt(numConnections)+""));
            numConnections++;
        }

        List<BufferedReader> ins=new ArrayList<>(numConnections);
        List<PrintWriter> outs=new ArrayList<>(numConnections);

        // read IP from clients
        List<String> IpAdresses=new ArrayList<>();
        String inputLine;
        int target;
        String ip;
        for (Pair<Socket,String> client : clients) {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getFirst().getInputStream()));
            PrintWriter out = new PrintWriter(client.getFirst().getOutputStream(), true);
            while ((inputLine = in.readLine()) != null) {
                System.out.println("vn says: " + inputLine);
                target=inputLine.indexOf(":");
                ip=inputLine.substring(8,target); // message is the form of CONNECT Ip_Addr:Port  (Mindful of the 1 space please)
                IpAdresses.add(ip);
                out.println("on received: " + inputLine);
                if (inputLine.equals("stop")) {
                    break;
                }
            }
        }
        
        // main loop which runs while the server is running
        Clock clock = Clock.system(ZoneId.of("IST"));
        Instant start = clock.instant();
        while (run) {
            // in this loop I want to send out info to all clients
            // for (Pair<Socket,String> client : clients) {
            //     PrintWriter out = new PrintWriter(client.getFirst().getOutputStream(), true);
            //     out.println("Connection Accepted");
            // }

            // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            for (Pair<Socket,String> client : clients) {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getFirst().getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("vn says: " + inputLine);
                    out.println("on received: " + inputLine);
                    if (inputLine.equals("stop")) {
                        break;
                    }
                }
            }

            in.close();
            out.close();
            clientSocket.close();
            System.out.println("Client disconnected.");
            if (numConnections++ >=26) {
                System.out.println("Max connections reached, stopping server.");
                run=false;
            }
        }
        serverSocket.close();
        sc.close();
    }
}

// class test{
//     public static void main(String[] args) throws IOException {
//         OracleNode x = new OracleNode("ON_1");
//         ServerSocket serverSocket = x.initSocket();

//         // List<Socket> clients=new ArrayList<>();
        
//         // Scanner sc=new Scanner(System.in);
//         // System.out.println("Enter config file name:");
//         // String filename=sc.nextLine();

//         // Readfile rf = new Readfile();
//         // List<String> configLines = rf.readFile(filename);
//         // for(String line : configLines){
//         //     System.out.println(line);
//         // }


//         Socket clientSocket = serverSocket.accept(); 
//         System.out.println("vn connected: " + clientSocket.getInetAddress());

//         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

//         String inputLine;
//         while ((inputLine = in.readLine()) != null) {
//             System.out.println("vn says: " + inputLine);
//             out.println("on received: " + inputLine);
//             if (inputLine.equals("stop")) {
//                 break;
//             }
//         }

//         in.close();
//         out.close();
//         clientSocket.close();
//         serverSocket.close();
//         System.out.println("Server stopped.");
//     }
// }