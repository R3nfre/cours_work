package com.example.course_work;

import com.example.course_work.server.MainServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerEntryPoint {
    public static void main(String[] args)
    {
        ExecutorService exc = Executors.newFixedThreadPool(2);
        MainServer server = new MainServer("localhost", 8433);
        exc.execute(server::start);
    }
}
