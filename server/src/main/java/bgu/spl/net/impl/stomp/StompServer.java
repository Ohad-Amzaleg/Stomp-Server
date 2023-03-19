package bgu.spl.net.impl.stomp;

import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.srv.Server;

import java.util.Scanner;

public class StompServer {

    public static void main(String[] args) {
        //take input from keyboard
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Enter a server type:");
//        String line = scanner.nextLine();
//
//        if (line == "tpc") {
            // you can use any server...
            Server.threadPerClient(7777, //port
                    () -> new StompProtocol(), //protocol factory
                    LineMessageEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }

//        else if (line == "reactor") {
//             Server.reactor(
//                     Runtime.getRuntime().availableProcessors(),
//                     7777, //port
//                     () -> new StompProtocol(), //protocol factory
//                     LineMessageEncoderDecoder::new //message encoder decoder factory
//             ).serve();
//
//        }
//    }
}
