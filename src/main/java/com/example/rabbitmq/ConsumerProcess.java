package com.example.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConsumerProcess {
    public static void main(String[] args) throws IOException {

        Connection connection =  ConnectionUtils.newConnection("localhost", 5674);

        Channel channel = connection.createChannel();

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                ObjectMapper resMsg = new ObjectMapper();
                Map resMap = resMsg.readValue(body, Map.class);

                String routingKey = envelope.getRoutingKey();

                System.out.println(resMap);
                System.out.println(routingKey);

                if(routingKey != null) {

                    String[] key = routingKey.split("\\.");

                    String type = key[0];
                    String userName = key[1];
                    String command = key[2];

                    System.out.println("type = " + type + ", userName = " + userName + ", command = " + command + ", message = " + resMap);

                    if(command.equals("join")) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, String> msgMap = new HashMap<>();
                        msgMap.put("from", userName);
                        String routeKey = "";
                        if (type.equals("caller")) {
                            routeKey = "callee." + userName.replace("launcher", "mplayer") + ".noti" ;
                        } else {
                            routeKey = "caller." + userName.replace("mplayer", "launcher") + ".noti" ;
                        }
                        channel.basicPublish("webrtc.join.noti", routeKey, null, objectMapper.writeValueAsBytes(msgMap));
                    }
                }
            }
        };


        channel.basicConsume("webrtc",  true, consumer);
    }
}
