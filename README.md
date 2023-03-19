# Stomp-Server
A community-led world cup update subscription service. That allow users to subscribe to a game channel and report and receive reports about the game to and from the other subscribed users
The STOMP server services and a client, which a user can use in
order to interact with the rest of the users. The server is implemented
in Java and support both Thread-Per-Client (TPC) and the Reactor,
choosing which one according to arguments given on startup. The client is
implemented in C++.
All communication between the clients and the server will be according to
STOMP ‘Simple-Text-Oriented-Messaging-Protocol’
