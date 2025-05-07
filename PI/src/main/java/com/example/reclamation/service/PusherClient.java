package com.example.reclamation.service;

import com.pusher.rest.Pusher;

public class PusherClient {
    private static Pusher pusher;
    static {
        pusher = new Pusher(
            "1948270",
            "5e84f2b708f7b43445d8",
            "1ca118fab150835dc063"
        );
        pusher.setCluster("eu");
        pusher.setEncrypted(true);
    }
    public static Pusher get() {
        return pusher;
    }
}