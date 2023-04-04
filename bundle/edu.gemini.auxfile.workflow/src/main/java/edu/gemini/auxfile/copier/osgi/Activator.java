package edu.gemini.auxfile.copier.osgi;

import edu.gemini.auxfile.copier.AuxFileCopier;
import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.auxfile.copier.CopyConfig;
import edu.gemini.auxfile.copier.impl.AuxFileCopierImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.HashMap;
import java.util.Map;

public class Activator implements BundleActivator {
    private ServiceRegistration<AuxFileCopier> _reg;

    public synchronized void start(BundleContext ctx) throws Exception {
        final Map<AuxFileType, CopyConfig> configs = new HashMap<>();
        for (AuxFileType t : AuxFileType.values()) {
            configs.put(t, OsgiCopyConfig.create(t, ctx));
        }

        final AuxFileCopier cp = new AuxFileCopierImpl(configs);
        _reg = ctx.registerService(AuxFileCopier.class, cp, null);
    }

    public synchronized void stop(BundleContext ctx) throws Exception {
        _reg.unregister();
        _reg = null;
    }
}
