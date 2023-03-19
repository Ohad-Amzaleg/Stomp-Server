#pragma once

#include <map>
#include <vector>
#include <string>
#include <sstream>
#include <algorithm>
#include "../include/event.h"
#include "../include/StompProtocol.h"

using namespace ::std;
using std::map;
using std::string;

StompProtocol::StompProtocol() {

}


string StompProtocol::process(string line, string type) {
    if (type == "output")
        handleOutPut(line);

    else
        handleInput(line);

}


string StompProtocol::handleOutPut(string line) {
//Split the message to words
    vector<string> parts;
    stringstream ss(line);
    string chunk;

    while (getline(ss, chunk, ' ')) {
        parts.push_back(chunk);
    }

    string command = parts[0];
    string toSend;


    //Convert command to frame and send it
    if (command == "login") {
        string toSend =
                "CONNECT\naccept-version:1.2\nhost:" + parts[1] + "\nlogin:" + parts[2] + "\npasscode:" + parts[3] +
                "\n\n\0";
    }


    if (command == "join") {
        //generate random id
        int SubId = rand() % 1000000;

        //Send the desired topic to subscribe to and id of the subscription
        string toSend = "SUBSCRIBE\ndestination:" + parts[1] + "\nid:" + SubId + "\n\n\0";
        map.emplace(parts[1], SubId);

    }


    if (command == "exit") {
        //Find the id of the topic to unsubscribe from
        int SubId = map[ parts[1]];
        string id= to_string(SubId);

        string toSend = "UNSUBSCRIBE\nid:" + id + "\n\n\0";
    }

    if (command == "report") {
        //Go over events and order them by time
        names_and_events nameEvents = parseEventsFile(parts[1]);
        vector<Event> eventsVector = nameEvents.events;

        //Sort the events vector by time
        sort(eventsVector.begin(), eventsVector.end(), [](const Event &a, const Event &b) {
            return a.get_time() < b.get_time();
        });

        for (int i = 0; i < eventsVector.size(); i++) {
            //Send the event to the server in this format:
            string toSend = "SEND\ndestination:" + nameEvents.team_a_name+"_"+nameEvents.team_b_name + "\nuser:" + parts[2] + "\nteam a:" + eventsVector[i].get_team_a_name() +
                            "\nteam b:" + eventsVector[i].get_team_b_name() + "\nevent name:" + eventsVector[i].get_name() +
                            "\ntime:" + to_string(eventsVector[i].get_time()) + "\n" + to_string(eventsVector[i].get_game_updates().size()) +
                            "\ngeneral game updates:" + eventsVector[i].get_game_updates() + "\n" +
                            eventsVector[i].get_team_a_updates().size() + "\nteam a updates:" + eventsVector[i].get_team_a_updates() +
                            "\n" + eventsVector[i].get_team_b_updates().size() + "\nteam b updates:" + eventsVector[i].get_team_b_updates() +
                            "\n" + eventsVector[i].get_discription().size() + "\ndescription:" + eventsVector[i].get_discription() + "\n\n\0";
        }

    }

    if (command == "summary") {
        string toSend = "SEND\ndestination:" + parts[1] + "\n\n" + parts[2] + "\n\0";
    }

    if (command == "logout") {
        string toSend = "SEND\ndestination:" + parts[1] + "\n\n" + parts[2] + "\n\0";
    }

}

string StompProtocol::handleInput(string line) {
    vector<string> parts;
    stringstream ss(line);
    string chunk;

    while (getline(ss, chunk, ' ')) {
        parts.push_back(chunk);
    }

    string command = parts[0];
    string fromServer;


    if (command == "CONNECTED") {
        string fromServer = "CONNECTED\nversion:1.2\n\n\0";
    }

    if (command == "MESSAGE") {
        string fromServer = "MESSAGE\nsubscription:" + parts[1] + "\n\n" + parts[2] + "\n\0";
    }

    if (command == "RECEIPT") {
        string fromServer = "RECEIPT\nreceipt-id:" + parts[1] + "\n\n\0";
    }

    if (command == "ERROR") {
        string fromServer = "ERROR\nmessage:" + parts[1] + "\n\n\0";
    }



}


