package com.justinmichaud.remotesupport.client.tunnel;

import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class PeerConnection {

    public final ServiceManager serviceManager;
    private final TlsConnection tlsConnection;

    private final Logger logger;

    public PeerConnection(String alias, String partnerAlias, Socket baseSocket, boolean server)
            throws GeneralSecurityException, IOException, OperatorCreationException {
        logger = LoggerFactory.getLogger("Peer Connection");

        logger.debug("Creating Peer Connection");

        tlsConnection = new TlsConnection(alias, partnerAlias, baseSocket,
                new File(alias.replaceAll("\\W+", "") + "_private.jks"),
                new File(alias.replaceAll("\\W+", "") + "_trusted.jks"), server);
        serviceManager = new ServiceManager(tlsConnection, this::onStopped);
    }

    public void openServerPort(int id, int localPort, int remotePort) throws IOException {
        serviceManager.addService(new LocalTunnelServerService(id, serviceManager, localPort, remotePort));
    }

    public void openRemoteServerPort(int theirPort, int ourPort) throws IOException {
        serviceManager.controlService.requestPeerOpenServerPort(theirPort, ourPort);
    }

    public boolean isRunning() {
        return serviceManager.isRunning() || !tlsConnection.isClosed();
    }

    private void onStopped() {
        if (!isRunning()) return;

        try {
            tlsConnection.close();
        } catch (IOException e) {
            logger.error("Error closing tls connection:", e);
        }
    }

    public void stop() {
        if (!isRunning()) return;

        serviceManager.stop();
    }
}