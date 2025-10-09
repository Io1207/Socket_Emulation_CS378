// import java.util.*;
import java.net.*;
import java.io.*;

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
        ServerSocket serverSocket = new ServerSocket(5000); // Listen on port 5000
        System.out.println("started, waiting for VN");
        return serverSocket;
    }
}


class test{
    public static void main(String[] args) throws IOException {
        OracleNode x = new OracleNode("god node");
        ServerSocket serverSocket = x.initSocket();
        Socket clientSocket = serverSocket.accept(); 
        System.out.println("vn connected: " + clientSocket.getInetAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            System.out.println("vn says: " + inputLine);
            out.println("on received: " + inputLine);
            if (inputLine.equals("stop")) {
                break;
            }
        }

        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
        System.out.println("Server stopped.");
    }
}
