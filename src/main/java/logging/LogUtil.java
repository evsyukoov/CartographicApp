package logging;

import bot.Client;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogUtil {

    public static void log(Level level, String name, Client client, Throwable e) {
        Logger logger = Logger.getLogger(name);
        logger.log(level, String.format("From: %s\nClient : %s\nState: %d\nError: %s\nStack trace: %s\n",
                name, client.getName(),
                client.getState(), e.getMessage(), e.getCause()));
    }

    public static void log(Level level, String name, Throwable e) {
        Logger logger = Logger.getLogger(name);
        logger.log(level, String.format("From: %s\nError: %s\nStack trace: %s\n",
                name, e.getMessage(), e.getCause()));
    }

    public static void log(String name, Client client) {
        Logger logger = Logger.getLogger(name);
        logger.info(String.format("From: %s\nClient: %s\nState : %d\n",
                name,client.getName(), client.getState()));
    }

    public static void log(String name, Client client, String message) {
        Logger logger = Logger.getLogger(name);
        logger.info(String.format("From: %s\nClient: %s\nState : %d\nInfo: %s\n",
                name, client.getName(), client.getState(), message));
    }
}
