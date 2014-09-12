package edu.gemini.util.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
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

    private static KeyStore loadTrustStore() {
        try {
            final KeyStore    ks = KeyStore.getInstance(KeyStore.getDefaultType());

            // This key store contains only the public key so the password
            // protection is sort of irrelevant and doesn't need to be secret.
            final InputStream is = GemSslSocketFactory.class.getResourceAsStream("gemTrustStore.jks");
            ks.load(is, "_Curry1!".toCharArray());

            return ks;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private static SSLSocketFactory create() {
        try {
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(loadTrustStore());

            final SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);

            return ctx.getSocketFactory();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}

