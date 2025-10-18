#include <iostream>
#include <string>
#include <cstring>
#include <vector>
#include <sstream>
#include <arpa/inet.h>
#include <unistd.h>
#include <bitset>
#include <fcntl.h>

using namespace std;

string ip_to_bit(string ip) {
    stringstream ss(ip);
    string segment;
    string bitstring;
    int num;

    // Split by '.'
    while (getline(ss, segment, '.')) {
        num = stoi(segment);  // convert each segment to int
        bitstring += bitset<8>(num).to_string();  // convert to 8-bit binary
    }

    return bitstring;  // 32-bit string
}

string port_to_bit(int port) {
    if (port < 0 || port > 65535)
        throw out_of_range("Port number must be between 0 and 65535");

    return bitset<16>(port).to_string();
}


string int_to_5bit(int val) {
    bitset<5> bits(val); // 5-bit representation
    return bits.to_string();
}

string bits_to_ip(const string& bits) {
    // Split 32-bit string into 4 octets
    string ip;
    for (int i = 0; i < 32; i += 8) {
        int octet = stoi(bits.substr(i, 8), nullptr, 2);
        ip += to_string(octet);
        if (i < 24) ip += '.';
    }
    return ip;
}

int bits_to_int(const string& bits) {
    return stoi(bits, nullptr, 2);
}

struct nbr{
    char name;
    string ipaddr;
    int port;
    int cost;

    nbr(char n, string ip, int p, int c){
        name = n;
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
    int seqnum;
    char myname;
    vector<nbr> neighbours;
    vector<vector<pair<int, int> > > adjlist;   // adjlist[i] has a list of all VNs adjacent to i+'a' as <j, cost>
    vector<int> latest_seq;

    int vnSocket;// TCP socket
    int udpSocket;
    sockaddr_in myAddr{};

    VN(string onip, int onport, string myip, int myport)
        : ONIPAddr(onip), ONPort(onport), myIP(myip), myPort(myport) {
            seqnum = 0;
            myname = '?';
            adjlist.resize(26);
            latest_seq.resize(26, -1);
            vnSocket = -1;
            udpSocket = socket(AF_INET, SOCK_DGRAM, 0);
            if (udpSocket < 0) {
                perror("Socket creation failed");
                throw runtime_error("Failed to create UDP socket");
            }

            // Bind to the specified IP and port
            myAddr.sin_family = AF_INET;
            myAddr.sin_port = htons(myPort);
            myAddr.sin_addr.s_addr = inet_addr(myIP.c_str());

            if (bind(udpSocket, (struct sockaddr*)&myAddr, sizeof(myAddr)) < 0) {
                perror("Bind failed");
                close(udpSocket);
                throw runtime_error("Failed to bind UDP socket");
            }

            cout << "[VN] UDP socket initialized and bound to "
                << myIP << ":" << myPort << endl;

        }

    
    bool connect_to_oracle() {
        vnSocket = socket(AF_INET, SOCK_STREAM, 0);
        if (vnSocket < 0) {
            perror("Socket creation failed");
            return false;
        }

        sockaddr_in serverAddress{};
        serverAddress.sin_family = AF_INET;
        serverAddress.sin_port = htons(ONPort);

        if (inet_pton(AF_INET, ONIPAddr.c_str(), &serverAddress.sin_addr) <= 0) {
            perror("Invalid address / Address not supported");
            close(vnSocket);
            vnSocket = -1;
            return false;
        }
        cout<<"Attempting to connect to Oracle Node at " << ONIPAddr << ":" << ONPort << "...\n";
        if (connect(vnSocket, (struct sockaddr*)&serverAddress, sizeof(serverAddress)) < 0) {
            perror("Connection Failed");
            close(vnSocket);
            vnSocket = -1;
            return false;
        }

        // Send initial connect message
        string connect_message = ip_to_bit(myIP) + port_to_bit(myPort) + "\n";
        send(vnSocket, connect_message.c_str(), connect_message.size(), 0);

        cout << "Connected to Oracle Node at " << ONIPAddr << ":" << ONPort << endl;
        return true;
    }

    void make_socket_nonblocking(int sock) {
        int flags = fcntl(sock, F_GETFL, 0);
        if (flags == -1) {
            perror("fcntl F_GETFL");
            return;
        }
        if (fcntl(sock, F_SETFL, flags | O_NONBLOCK) == -1) {
            perror("fcntl F_SETFL O_NONBLOCK");
        }
    }

    void listen_for_on_messages() {
        if (vnSocket < 0) {
            return;
        }

        // Ensure socket is non-blocking
        make_socket_nonblocking(vnSocket);

        char buffer[4096];
        int bytesReceived = recv(vnSocket, buffer, sizeof(buffer) - 1, 0);

        if (bytesReceived > 0) {
            buffer[bytesReceived] = '\0';
            std::string response(buffer);

            // Optional: remove trailing newline if present
            if (!response.empty() && response.back() == '\n') {
                response.pop_back();
            }

            std::cout << "\nReceived from Oracle (" << bytesReceived << " bytes): " << response << std::endl;

            parse_neighbours(response);
            send_my_lsp();
        } 
        else if (bytesReceived == 0) {
            std::cout << "Oracle node closed the connection" << std::endl;
            exit(1);
        } 
        else {
            if (errno != EWOULDBLOCK && errno != EAGAIN) {
                perror("recv() error");
            }
            // EWOULDBLOCK/EAGAIN → no data available, do nothing
        }
    }

    void send_my_lsp() {
        string my_message = my_lsp(); // get your LSP as a bitstring
        forward_to_nbrs(my_message);
        seqnum++;
    }

    void forward_to_nbrs(string& message){
        const char* msg_cstr = message.c_str();
        size_t msg_len = message.size();

        for (auto& nbr : neighbours) {
            sockaddr_in nbrAddr{};
            nbrAddr.sin_family = AF_INET;
            nbrAddr.sin_port = htons(nbr.port);

            if (inet_pton(AF_INET, nbr.ipaddr.c_str(), &nbrAddr.sin_addr) <= 0) {
                cerr << "Invalid neighbour IP: " << nbr.ipaddr << endl;
                continue;
            }

            int sentBytes = sendto(udpSocket, msg_cstr, msg_len, 0,
                                (struct sockaddr*)&nbrAddr, sizeof(nbrAddr));
            if (sentBytes < 0) {
                perror(("Failed to send LSP to " + string(1, nbr.name)).c_str());
            } else {
                // cout << "Sent LSP (" << sentBytes << " bytes) to "
                //     << nbr.name << " at " << nbr.ipaddr << ":" << nbr.port << endl;
            }
        }
    }

    void listen_for_vn_messages() {
        char buffer[4096];
        sockaddr_in senderAddr{};
        socklen_t senderLen = sizeof(senderAddr);

        // Non-blocking receive (or blocking, depending on your preference)
        // cout << "gonna try to recv from udp " << endl;
        int bytesReceived = recvfrom(udpSocket, buffer, sizeof(buffer) - 1, MSG_DONTWAIT,
                                    (struct sockaddr*)&senderAddr, &senderLen);
        // cout << "udp recv call returned " << endl;

        if (bytesReceived > 0) {
            buffer[bytesReceived] = '\0';
            string msg(buffer);

            // Extract sender IP and port for info
            char senderIP[INET_ADDRSTRLEN];
            inet_ntop(AF_INET, &(senderAddr.sin_addr), senderIP, INET_ADDRSTRLEN);
            int senderPort = ntohs(senderAddr.sin_port);

            // cout << "\nReceived from VN " << senderIP << ":" << senderPort
            //     << " (" << bytesReceived << " bytes): " << msg << endl;

            // parse or handle VN message as needed
            parse_lsp(msg);  // takes care of forwarding the lsp to nbrs as well
        }
        else if (bytesReceived == -1) {
            if (errno != EWOULDBLOCK && errno != EAGAIN) {
                perror("recvfrom() error");
            }
            // else: no data available, do nothing (non-blocking)
        }
    }

    void close_connection() {
        if (vnSocket >= 0) {
            close(vnSocket);
            vnSocket = -1;
            cout << "Connection closed manually.\n";
        }
    }

    void print_state(){
        cout << "Currently Known Graph to me (Virtual Node "<<myname<<")\n";
        for(int i=0; i<26; i++){
            if(!adjlist[i].empty()){
                cout << char(i+'a') << " -> ";
                for(auto& nbr: adjlist[i]){
                    cout << "(" << char(nbr.first+'a') << "," << nbr.second << ") ";
                }
                cout << endl;
            }
        }
        cout << endl;
    }


private:
    void parse_neighbours(string& bitmsg) {
        const int ENTRY_BITS = 5 + 32 + 16 + 16; // 69 bits per neighbour

        if (bitmsg.size() % ENTRY_BITS != 0) {
            cerr << "Invalid bitstring length: " << bitmsg.size() << endl;
            return;
        }

        int N = bitmsg.size() / ENTRY_BITS;
        neighbours.clear();
        adjlist[myname-'a'].clear();

        for (int i = 0; i < N; ++i) {
            int base = i * ENTRY_BITS;

            string idx_bits  = bitmsg.substr(base, 5);
            string ip_bits   = bitmsg.substr(base + 5, 32);
            string port_bits = bitmsg.substr(base + 37, 16);
            string cost_bits = bitmsg.substr(base + 53, 16);

            char name   = bits_to_int(idx_bits) + 'a';
            string ip   = bits_to_ip(ip_bits);
            int port    = bits_to_int(port_bits);
            int cost    = bits_to_int(cost_bits);

            if(ip == myIP && port == myPort){
                myname = name;
                cout << "I am VN " << myname << endl;
                break;
            }
        }

        for (int i = 0; i < N; ++i) {
            int base = i * ENTRY_BITS;

            string idx_bits  = bitmsg.substr(base, 5);
            string ip_bits   = bitmsg.substr(base + 5, 32);
            string port_bits = bitmsg.substr(base + 37, 16);
            string cost_bits = bitmsg.substr(base + 53, 16);

            char name   = bits_to_int(idx_bits) + 'a';
            string ip   = bits_to_ip(ip_bits);
            int port    = bits_to_int(port_bits);
            int cost    = bits_to_int(cost_bits);

            if(name == myname){
                continue;
            }

            neighbours.emplace_back(nbr(name, ip, port, cost));
            adjlist[myname-'a'].push_back(make_pair(int(name - 'a'), cost));
        }

        cout << "Parsed Neighbours:\n";
        for (auto& n : neighbours)
            cout << n.name << " (" << n.ipaddr << "," << n.port << ") = " << n.cost << endl;

        cout << endl;
    }

    void parse_lsp(string& msg) {
        if (msg.size() < 10) { // 5 bits origin + 5 bits seqnum
            cerr << "LSP message too short\n";
            return;
        }

        // Extract origin ID
        string origin_bits = msg.substr(0, 5);
        int originID = bits_to_int(origin_bits); // 0-25
        if(originID == myname - 'a') return;

        // Extract sequence number
        string seq_bits = msg.substr(5, 5);
        int seqnum = bits_to_int(seq_bits);

        // Check if this LSP is newer
        if (seqnum > latest_seq[originID]) {
            // cout << "Received newer LSP from VN " << char('a' + originID)
            //     << " with seqnum " << seqnum << endl;

            // Update latest sequence number
            latest_seq[originID] = seqnum;

            // Extract the rest of the LSP (link state info)
            string link_state_bits = msg.substr(10);

            // Process link_state_bits as needed
            adjlist[originID].clear();
            int base = 0;
            while(base < link_state_bits.size()-1){
                int nbrname = bits_to_int(link_state_bits.substr(base, 5));
                int cost = bits_to_int(link_state_bits.substr(base+5, 16));
                adjlist[originID].push_back(make_pair(nbrname, cost));
                // cout<<"Neighbour name: "<<nbrname<<" cost: "<<cost<<endl;
                base += 21;
            }
            forward_to_nbrs(msg);

        } else {
            // cout << "Received old or duplicate LSP from VN "
            //     << char('a' + originID) << " (seqnum " << seqnum << ")\n";
        }
    }

    string my_lsp(){
        string lsp_msg = int_to_5bit(int(myname - 'a')) + int_to_5bit(seqnum);

        for(auto& nbr: neighbours){
            lsp_msg += int_to_5bit(int(nbr.name - 'a')) + port_to_bit(nbr.cost);
        }

        return lsp_msg;
    }
};



int main(int argc, char* argv[]) {
    if (argc != 5) {
        std::cerr << "Usage: " << argv[0] 
                  << " <OracleIP> <OraclePort> <VNIP> <VNPort>\n";
        return 1;
    }

    std::string oracleip = argv[1];
    int oracleport = std::stoi(argv[2]);
    std::string vnip = argv[3];
    int vnport = std::stoi(argv[4]);

    std::cout << "Oracle IP: " << oracleip << ", Port: " << oracleport << "\n";
    std::cout << "VN IP: " << vnip << ", Port: " << vnport << "\n";


    VN node(oracleip, oracleport, vnip, vnport);
    node.connect_to_oracle();
    long long counter = 0;
    long long bignum = 5e6;
    long long lspnum = 1e6;
    long long psnum = 1e5;

    while(true){
        node.listen_for_on_messages();
        node.listen_for_vn_messages();

        if(!counter%lspnum && node.myname != '?'){
            node.send_my_lsp();
        }

        if(!counter%psnum && node.myname != '?'){
            node.print_state();
        }

        counter = (counter + 1)%bignum;
    }

    return 0;
}