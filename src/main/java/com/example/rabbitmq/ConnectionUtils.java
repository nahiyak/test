package com.example.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ConnectionUtils {

    public static Connection newConnection(){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("myuser");
        factory.setPassword("mypass");
        factory.setVirtualHost("/");
        factory.setHost("localhost");
        factory.setPort(5672);

        try {
            return factory.newConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setChannle(Channel channel, String userName, String type, String exchangeName, String queueName) throws IOException {
        channel.exchangeDeclare(exchangeName, "topic", true);
        channel.exchangeDeclare(exchangeName +".join.noti", "topic", true);


        String joinQueue = queueName + ".join";
        String offerQueue = queueName + ".answer";
        String answerQueue = queueName + ".offer";
        String candidateQueue = queueName + ".candidate";
        String leaveQueue = queueName + ".leave";
        String joinNotiQueue = queueName + ".join.noti";

        channel.queueDeclare(offerQueue, true, true, false, null);
        channel.queueDeclare(answerQueue, true, true, false, null);
        channel.queueDeclare(candidateQueue, true, true, false, null);
        channel.queueDeclare(joinQueue, true, true, false, null);
        channel.queueDeclare(leaveQueue, true, true, false, null);
        channel.queueDeclare(joinNotiQueue, true, true, false, null);


        channel.queueBind(offerQueue, exchangeName, type + "." + userName + ".offer");
        channel.queueBind(answerQueue, exchangeName, type + "." + userName + ".answer");
        channel.queueBind(candidateQueue, exchangeName, type + "." + userName + ".candidate");
        channel.queueBind(joinQueue, exchangeName, type + "." + userName + ".join");
        channel.queueBind(leaveQueue, exchangeName, type + "." + userName + ".leave");
        channel.queueBind(joinNotiQueue, exchangeName + ".join.noti", type + "." + userName + ".noti");


        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String routingKey = envelope.getRoutingKey();

                ObjectMapper resMsg = new ObjectMapper();
                Map resMap = resMsg.readValue(body, Map.class);

                System.out.println(resMap);
                System.out.println(routingKey);

                if(routingKey != null) {

                    String[] key = routingKey.split("\\.");

                    String type = key[0];
                    String userName = key[1];
                    String command = key[2];

                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, String> msgMap = new HashMap<>();

                    if(type.equals("caller")){
                        if(command.equals("noti")){
                            // send offer callee
                            msgMap.put("from", userName);
                            msgMap.put("sdp", "offerSDP");
                            channel.basicPublish("webrtc", "callee." + resMap.get("from") + ".offer", null, objectMapper.writeValueAsBytes(msgMap));
                        }else if(command.equals("answer")){
                            // send candidate callee
                            msgMap.put("from", userName);
                            msgMap.put("sdp", "answerSDP");
                            channel.basicPublish("webrtc", "callee." + resMap.get("from") + ".candidate", null, objectMapper.writeValueAsBytes(msgMap));
                        }else if(command.equals("candidate")){
                            //
                        }
                    }else{
                        if(command.equals("offer")){
                            // send answer caller
                            // send offer callee
                            msgMap.put("from", userName);
                            msgMap.put("sdp", "answerSDP");
                            channel.basicPublish("webrtc", "caller." + resMap.get("from") + ".answer", null, objectMapper.writeValueAsBytes(msgMap));
                        }else if(command.equals("candidate")){
                            // send candidate caller
                            // send offer callee
                            msgMap.put("from", userName);
                            msgMap.put("candidate", "candidateMSG");
                            channel.basicPublish("webrtc", "caller." + resMap.get("from") + ".candidate", null, objectMapper.writeValueAsBytes(msgMap));
                        }
                    }

                }
            }
        };


        channel.basicConsume(offerQueue, consumer);
        channel.basicConsume(answerQueue, consumer);
        channel.basicConsume(candidateQueue, consumer);
        channel.basicConsume(joinQueue, consumer);
        channel.basicConsume(leaveQueue, consumer);
        channel.basicConsume(joinNotiQueue, consumer);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("from", userName);

        channel.basicPublish(exchangeName, type + "." + userName + ".join" , null, objectMapper.writeValueAsBytes(msgMap));

    }
}
