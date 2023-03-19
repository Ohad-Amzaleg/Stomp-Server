package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.*;

public class StompProtocol implements StompMessagingProtocol<String> {

    private static StompConnections<String> connections=new StompConnections<>();

    //Map Topic to Subscribers
    private static HashMap<String, HashMap<Integer, Integer>> subscriptions = new HashMap<>();

    //Users and passwords of the system
    private static HashMap<String, String> users = new HashMap<>();

    //Logged in users
    private static HashMap<String, Integer> LoggedIn = new HashMap<>();

    private static int messageID = 0;

    //Subscription Id
    private HashMap<Integer, String> SubscriptionIdToTopic;


    private int connectionId;

    private String userName;

    private boolean shouldTerminate;

    @Override
    public void start(int connectionId, ConnectionHandler<String> handler) {
        this.connectionId = connectionId;
        this.SubscriptionIdToTopic=new HashMap<>();
        connections.add(connectionId,handler);
    }

    @Override
    public void process(String message) {
        System.out.println("Message received: " + message);
        // Parse the message and handle the STOMP command
        String[] lines = message.split("\n");
        String command = lines[0];
        Map<String, String> headers = new HashMap<>();
        String body = null;

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                for (int j = i + 1; j < lines.length; j++) {
                    if(lines[j]!="\0")
                        body += lines[j];
                }
                break;
            }

            //Split the headers to (header,value)
            String[] parts = lines[i].split(": ");
            headers.put(parts[0], parts[1]);
        }
        switch (command) {
            case "CONNECT":
                handleConnect(headers);
                break;
            case "SUBSCRIBE":
                synchronized (users.get(userName)) {
                    handleSubscribe(headers);
                }
                break;
            case "SEND":
                handleSend(headers, body);
                break;
            case "UNSUBSCRIBE":
                synchronized (users.get(userName)) {
                    handleUnsubscribe(headers);
                }
                break;
            case "DISCONNECT":
                synchronized (users.get(userName)) {
                    handleDisconnect();
                }
                break;
        }
    }

    private void handleConnect(Map<String, String> headers) {
        String UserName = headers.get("login");
        String Password = headers.get("passcode");

        //Check if the user is a new user
        if (!users.containsKey(UserName)) {
            users.put(UserName, Password);
            LoggedIn.put(UserName, connectionId);
            userName = UserName;
            connections.send(connectionId, "CONNECTED \n" + "version: " + headers.get("accept-version") + "\n\n" + '\0');
        }

        //Check if user enter wrong passcode
        else if (!users.get(UserName).equals(Password)) {
            connections.send(connectionId, "ERROR \n" + "message: Wrong password" + "\n\n" + '\0');
        }

        //Check if user is already logged in
        else if (LoggedIn.containsKey(UserName)) {
            connections.send(connectionId, "ERROR \n" + "message: User already logged in" + "\n\n" + '\0');
        }

        //User is already registered and enter the correct password
        else {
            LoggedIn.put(UserName, connectionId);
            connections.send(connectionId, "CONNECTED \n" + "version: " + headers.get("accept-version") + "\n\n" + '\0');
        }

    }

    private void handleSubscribe(Map<String, String> headers) {
        // Add the client to the list of subscribers for the given destination
        String destination = headers.get("destination");
        String SubscriptionId = headers.get("id");
        String receipt = headers.get("receipt");

        //Check if the topic exist and user already subscribe to it
        if (subscriptions.containsKey(destination) && subscriptions.get(destination).containsKey(connectionId)) {

            //Send error message
            connections.send(connectionId, "ERROR" + "\n" + "message: Already subscribed to " + destination + "\n\n" + '\0');
        }

        //Check if the topic exist and user didn't subscribe to it
        else if (subscriptions.containsKey(destination) && !subscriptions.get(destination).containsKey(connectionId)) {
            //Add the user to the topic
            subscriptions.get(destination).put(connectionId, Integer.parseInt(SubscriptionId));

            //Add the subscription id to the map
            SubscriptionIdToTopic.put(Integer.parseInt(SubscriptionId), destination);

            //Send the receipt
            if(receipt!=null)
                connections.send(connectionId, "RECEIPT" + "\n" + "receipt-id: " + receipt + "\n\n" + '\0');
        }

        //Check if the topic is not exist
        else {
            HashMap<Integer, Integer> temp = new HashMap<>();

            //Create the topic and add the user to the subscribers list
            temp.put(connectionId, Integer.parseInt(SubscriptionId));
            subscriptions.put(destination, temp);


            //Add the subscription id to the map
            SubscriptionIdToTopic.put(Integer.parseInt(SubscriptionId), destination);

            //Send the receipt
            if(receipt!=null)
                connections.send(connectionId, "RECEIPT\nreceipt-id:" + SubscriptionId + "\n\n"+ '\0');
        }


    }

    private void handleSend(Map<String, String> headers, String body) {
        // Forward the message to all subscribers of the given destination
        String destination = headers.get("destination");
        String response = "MESSAGE\n" + "subscription :" + headers.get("subscription") +
                "\n" +"message - id :"+messageID++ +
                "destination :" + destination + "\n\n" + body + "\n" + "\u0000";

        //Check if topic exist
        if (subscriptions.containsKey(destination)) {
            //Check if user subscribe to the topic
            if (subscriptions.get(destination).containsKey(connectionId)) {
                //Send the message to all the subscribers
                for (Map.Entry<Integer, Integer> subscriber : subscriptions.get(destination).entrySet()) {
                    connections.send(subscriber.getKey(), response);
                }
            }

            //User didn't subscribe to the topic
            else {
                connections.send(connectionId, "ERROR\n" + "message: You are not subscribed to " + destination + "\n\n" + '\0');

            }
        }

        //Topic is not exist
        else {
            connections.send(connectionId, "ERROR\n" + "message: Topic " + destination + " is not exist" + "\n\n" + '\0');
        }

    }

    private void handleUnsubscribe(Map<String, String> headers) {
        // Remove the client from the list of subscribers for the given destination
        String destination = SubscriptionIdToTopic.get(headers.get("id"));
        String receipt = headers.get("receipt");

        //Check if topic exist
        if (subscriptions.containsKey(destination)) {
            //Check if user subscribe to the topic
            if (subscriptions.get(destination).containsKey(connectionId)) {
                //Remove the user from the subscribers list
                subscriptions.get(destination).remove(connectionId);
                SubscriptionIdToTopic.remove(headers.get("id"));

                //Send the receipt
                if(receipt!=null)
                    connections.send(connectionId, "RECEIPT\nreceipt-id:" + headers.get("id") + "\n\n"+ '\0');
            }

        }

        //Topic is not exist
        else {
            connections.send(connectionId, "ERROR\n" + "message: Topic " + destination + " is not exist" + "\n\n" + '\0');
        }

    }

    private void handleDisconnect() {
        // Get this user SubscriptionsId
        Set<Map.Entry<Integer, String>> userSubscriptions = SubscriptionIdToTopic.entrySet();

        //Remove the user from the subscribers list
        for (Map.Entry<Integer, String> subscription : userSubscriptions) {
            String destination = subscription.getValue();
            subscriptions.get(destination).remove(connectionId);
        }

        //Remove the user from the logged in list
        LoggedIn.remove(userName);

        //Close the connection
        connections.disconnect(connectionId);

        //Send the receipt
        shouldTerminate=true;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}

