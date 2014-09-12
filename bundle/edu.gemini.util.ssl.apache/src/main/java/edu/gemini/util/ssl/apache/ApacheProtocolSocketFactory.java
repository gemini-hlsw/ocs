package edu.gemini.util.ssl.apache;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocketFactory;

public final class ApacheProtocolSocketFactory implements SecureProtocolSocketFactory {
    private final SSLSocketFactory factory;

    public ApacheProtocolSocketFactory(SSLSocketFactory delegate) {
        this.factory = delegate;
    }

    @Override public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return factory.createSocket(socket, s, i, b);
    }

    @Override public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException, UnknownHostException {
        return factory.createSocket(s, i, inetAddress, i2);
    }

    @Override public Socket createSocket(String s, int i) throws IOException {
        return factory.createSocket(s, i);
    }
}
