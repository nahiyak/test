package com.example.rabbitmq;

import com.rabbitmq.client.*;


import java.util.UUID;


public class Caller implements  Runnable {

    private String userName;

    public Caller(String userName) {
        this.userName = userName;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try(Connection connection = ConnectionUtils.newConnection("localhost", 5672); Channel channel = connection.createChannel()){
            ConnectionUtils.setChannle(channel, userName, "caller", "webrtc", UUID.randomUUID().toString());

            Thread.sleep(360000);
        }catch (Exception e){

        }
    }

    public static void main(String[] args)  {

        for(int i = 1; i <=1; i++){
            new Thread(new Caller("launcher" + i)).start();
        }
    }
}
