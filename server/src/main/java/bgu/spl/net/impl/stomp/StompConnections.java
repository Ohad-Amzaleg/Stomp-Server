package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.HashMap;
import java.util.Map;

public class StompConnections<T> implements Connections<T> {
    private Map<Integer, ConnectionHandler<T>> connections;

    public StompConnections() {
        connections = new HashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (connections.containsKey(connectionId)) {
            connections.get(connectionId).send(msg);
        }
        return true;
    }

    @Override
    public void send(String channel, T msg) {

    }

    @Override
    public void disconnect(int connectionId) {
        // Remove the connection with the given connectionId from the list of active connections
        connections.remove(connectionId);
    }

    public void add(int connectionId, ConnectionHandler<T> handler) {
        connections.put(connectionId, handler);

    }


}