#include <iostream>
#include <string>
#include <cstring>
#include <vector>
#include <sstream>
#include <arpa/inet.h>
#include <unistd.h>
using namespace std;


struct nbr{
    string ipaddr;
    string port;
    int cost;

    nbr(string ip, string p, int c){
        ipaddr = ip;
        port = p;
        cost = c;
    }
};

class VN {
public:
    string ONIPAddr;
    int ONPort;
    string myIP;
    int myPort;
    vector<nbr> neighbours; 

    VN(string onip, int onport, string myip, int myport) {
        ONIPAddr = onip;
        ONPort = onport;
        myIP = myip;
        myPort = myport;
    }

    void send_connect() {
        int vnSocket = socket(AF_INET, SOCK_STREAM, 0);
        if (vnSocket < 0) {
            perror("Socket creation failed");
            return;
        }

        sockaddr_in serverAddress{};
        serverAddress.sin_family = AF_INET;
        serverAddress.sin_port = htons(ONPort);

        if (inet_pton(AF_INET, ONIPAddr.c_str(), &serverAddress.sin_addr) <= 0) {
            perror("Invalid address/ Address not supported");
            close(vnSocket);
            return;
        }

        if (connect(vnSocket, (struct sockaddr*)&serverAddress, sizeof(serverAddress)) < 0) {
            perror("Connection Failed");
            close(vnSocket);
            return;
        }

        string message = "CONNECT " + myIP + ":" + to_string(myPort);
        send(vnSocket, message.c_str(), message.size(), 0);
        cout << "Sent Connect message to Oracle Node: " << message << endl;

        // --- Wait for neighbour information from Oracle ---
        char buffer[2048] = {0};
        int bytesReceived = recv(vnSocket, buffer, sizeof(buffer) - 1, 0);
        if (bytesReceived > 0) {
            buffer[bytesReceived] = '\0';
            string response(buffer);
            cout << "Received from Oracle: " << response << endl;

            parse_neighbours(response);
        } else {
            cout << "No message received from Oracle." << endl;
        }

        close(vnSocket);
    }

private:
    void parse_neighbours(const string& msg) {
        // Expecting format: "NEIGHBOURS ip1:port1:cost1,ip2:port2:cost2,..."
        if (msg.rfind("NEIGHBOURS", 0) != 0) {
            cout << "Unexpected message format: " << msg << endl;
            return;
        }

        string data = msg.substr(10); // Skip "NEIGHBOURS"
        stringstream ss(data);
        string token;

        while (getline(ss, token, ',')) {
            stringstream entry(token);
            string ipPort, costStr;
            vector<string> parts;
            string part;

            stringstream inner(token);
            while (getline(inner, part, ':')) parts.push_back(part);

            if (parts.size() == 3) {
                string neighbour = parts[0] + ":" + parts[1];
                int cost = stoi(parts[2]);
                neighbours.push_back(nbr(parts[0], parts[1], cost));
            }
        }

        cout << "Parsed Neighbours:" << endl;
        for (auto& nbr : neighbours) {
            cout << nbr.ipaddr << " " << nbr.port << " " << nbr.cost << endl;
        }
    }
};


int main() {
    string oracleip;
    string vnip;
    int oracleport;
    int vnport;

    cout<<"Oracle Node IP Address: ";
    cin>>oracleip;
    cout<<"Oracle Node Port: ";
    cin>>oracleport;

    cout<<"Virtual Node IP Address: ";
    cin>>vnip;
    cout<<"Virtual Node Port: ";
    cin>>vnport;

    VN node(oracleip, oracleport, vnip, vnport);
    node.send_connect();

    return 0;
}