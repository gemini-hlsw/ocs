package edu.gemini.util.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to an SSL Socket Factory that trusts our self-signed
 * certificate.
 */
public final class GemSslSocketFactory {
    private static final Logger LOG = Logger.getLogger(GemSslSocketFactory.class.getName());

    private GemSslSocketFactory() {}

    private static SSLSocketFactory factory = null;

    public static synchronized SSLSocketFactory get() {
        if (factory == null) {
            factory = create();
        }
        return factory;
    }

    // This is a trust manager that will not verify the certs on the chain
    // Since this is used as a client I claim that we don't need to verify the
    // cert chain and the communication will be encrypted anyway.
    private static TrustManager[] createTrustAllManagers() {
        return new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
    }

    private static SSLSocketFactory create() {
        try {
            final SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, createTrustAllManagers(), null);

            return ctx.getSocketFactory();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}

