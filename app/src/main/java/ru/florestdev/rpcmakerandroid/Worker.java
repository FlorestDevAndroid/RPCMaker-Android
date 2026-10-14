package ru.florestdev.rpcmakerandroid;

import com.my.kizzyrpc.KizzyRPC;
import com.my.kizzyrpc.model.Activity;
import com.my.kizzyrpc.model.Assets;
import com.my.kizzyrpc.model.Timestamps;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Worker {

    private static final String APPLICATION_ID = "1559894676306395156";

    private final KizzyRPC rpc;
    private final ExecutorService executor;

    public Worker(String discordToken) {
        if (discordToken == null || discordToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Discord token cannot be null or empty");
        }
        this.rpc = new KizzyRPC(discordToken);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RpcWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public void runActivity(String name,
                            String state,
                            String details,
                            String type,
                            String largeImage,
                            String largeText,
                            String smallImage,
                            String smallText,
                            List<String> buttons) {

        executor.execute(() -> {
            try {
                Timestamps timestamps = new Timestamps(System.currentTimeMillis(), null);

                Assets assets = null;
                if (notEmpty(largeImage) || notEmpty(largeText)
                        || notEmpty(smallImage) || notEmpty(smallText)) {
                    assets = new Assets(
                            notEmpty(largeImage) ? largeImage : null,
                            notEmpty(largeText) ? largeText : null,
                            notEmpty(smallImage) ? smallImage : null,
                            notEmpty(smallText) ? smallText : null
                    );
                }

                Activity activity = new Activity(
                        notEmpty(name) ? name : "RPCMaker",
                        notEmpty(state) ? state : null,
                        notEmpty(details) ? details : null,
                        parseType(type),
                        timestamps,
                        assets,
                        buttons,            // List<String>
                        null,               // metadata
                        APPLICATION_ID
                );

                rpc.setActivity(activity, "online", System.currentTimeMillis());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void clearActivity() {
        executor.execute(() -> {
            try {
                Activity activity = new Activity(
                        null, null, null, 0,
                        null, null, null, null,
                        APPLICATION_ID
                );
                rpc.setActivity(activity, "online", System.currentTimeMillis());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private int parseType(String type) {
        if (type == null) return 0;
        switch (type.toLowerCase()) {
            case "playing":   return 0;
            case "streaming": return 1;
            case "listening": return 2;
            case "watching":  return 3;
            case "competing": return 5;
            default:
                try {
                    return Integer.parseInt(type);
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            rpc.closeRPC();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}