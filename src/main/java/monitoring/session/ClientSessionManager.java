package monitoring.session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class ClientSessionManager {
    private ConcurrentHashMap<UUID, ClientSession> clientSessions = new ConcurrentHashMap<>();

    private static ClientSessionManager instance;

    public static ClientSessionManager instance() {
        if (instance == null) {
            instance = new ClientSessionManager();
        }
        return instance;
    }

    private ClientSessionManager() {
    }

    public void addSession(ClientSession session) {
        clientSessions.put(session.sessionId, session);
    }

    public ClientSession getSession(UUID sessionId) {
        return clientSessions.get(sessionId);
    }
}
