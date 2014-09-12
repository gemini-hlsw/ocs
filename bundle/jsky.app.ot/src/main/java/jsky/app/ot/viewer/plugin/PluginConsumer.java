package jsky.app.ot.viewer.plugin;

import jsky.app.ot.plugin.OtActionPlugin;

/**
 * Instances which want to be informed about the arrival and departure of plugins need to implement this interface
 * and register themselves with the plugin registry.
 */
public interface PluginConsumer {

    /** Called by plugin registry on arrival of a new plugin. */
    void install(OtActionPlugin plugin);

    /** Called by plugin registry on departure of a plugin. */
    void uninstall(OtActionPlugin plugin);
}
