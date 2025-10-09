#include <iostream>
#include <iostream>
#include <cstring>      // For memset
#include <sys/socket.h> // For socket(), connect()
#include <netinet/in.h> // For sockaddr_in
#include <arpa/inet.h>  // For inet_pton()
#include <unistd.h>     // For close()

class VN{
    public:
    int id;

    VN(int id):id(id){}

    bool operator==(const VN &other) const {
        return id == other.id;
    }

    void talk_to_on(int port){
        int vnSocket = socket(AF_INET, SOCK_STREAM, 0);
        sockaddr_in serverAddress;
        serverAddress.sin_family = AF_INET;
        serverAddress.sin_port = htons(port);
        serverAddress.sin_addr.s_addr = INADDR_ANY;

        connect(vnSocket, (struct sockaddr*)&serverAddress, sizeof(serverAddress));

        const char* message = "Hello, server!";
        send(vnSocket, message, strlen(message), 0);

        close(vnSocket);
    }

};

int main(){
    VN vn1(1);
    vn1.talk_to_on(5000);
    return 0;
}