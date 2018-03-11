package tl1.asv.projet.db;

import java.util.HashMap;

public class ClientsDatabase {

    public static final HashMap<String, String> clients = new HashMap<>();

    /**
     * Checks if a client has been found with both tokens.
     *
     * @param fcm
     * @param token
     * @return
     */
    public static boolean checkPair(String fcm, String token) {
        return clients.get(fcm) != null && clients.get(fcm).equals(token);
    }
}
