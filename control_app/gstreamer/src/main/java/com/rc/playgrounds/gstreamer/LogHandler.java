package com.rc.playgrounds.gstreamer;

import java.util.function.Consumer;

public class LogHandler {
    static {
        System.loadLibrary("rtsp-example");
    }

    private static LogHandler instance;
    Consumer<String> logger = (m) -> {};

    public static synchronized LogHandler getInstance() {
        if (instance == null) {
            instance = new LogHandler();
        }
        return instance;
    }

    public void handleLog(String log) {
        logger.accept(log);
    }
}
