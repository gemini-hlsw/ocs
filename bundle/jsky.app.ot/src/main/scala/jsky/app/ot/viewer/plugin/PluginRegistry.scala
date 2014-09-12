package jsky.app.ot.viewer.plugin

import jsky.app.ot.plugin.OtActionPlugin
import scala.collection.JavaConverters._
import jsky.app.ot.OTOptions

/**
 * Registry that knows all available plugins.
 * Instances which are interested in the availability of plugins can register themselves as plugin consumers.
 * Note that methods executed on Swing EDT are serialized and therefore don't need additional synchronization.
 */
object PluginRegistry {
  private var pluginSet: Set[OtActionPlugin] = Set.empty
  private var consumers: Set[PluginConsumer] = Set.empty

  /**
   * Gets all available plugins.
   * Only plugins that are accessible for the current user are returned. I assume this will need to be
   * modified once a finer access control structure is in place.
   */
  def plugins: Set[OtActionPlugin] =
    if (OTOptions.isStaffGlobally) pluginSet
    else Set.empty

  /**
   * Gets all available plugins ordered by their name. Use this if plugins should show up in the same order,
   * e.g. in UI elements like menus etc.
   * @return available plugins ordered by their names
   */
  def pluginsByName: Seq[OtActionPlugin] =
    plugins.toSeq.sortBy(_.name)

  def pluginsForJava: java.util.Set[OtActionPlugin] =
    plugins.asJava

  def pluginsByNameForJava: java.util.List[OtActionPlugin] =
    pluginsByName.asJava

  /** Registers a plugin consumer.
    * New consumers will not be notified of already registered plugins, only new arrivals will cause
    * install method to be called on consumer. */
  def registerConsumer(consumer: PluginConsumer) = synchronized {
    consumers += consumer
  }

  /** Unregisters a plugin consumer. */
  def unregisterConsumer(consumer: PluginConsumer) = synchronized {
    consumers -= consumer
  }

  /**
   * Adds a plugin.
   * Informs all registered consumers that a plugin is available. This code is executed on the Swing EDT!
   * @param plugin
   */
  def add(plugin: OtActionPlugin): Unit = {
    println("Adding plugin: " + plugin.name)
    if (!pluginSet.contains(plugin)) {
      pluginSet = pluginSet + plugin
      consumers.foreach { _.install(plugin) }
    }
  }

  /** Removes a plugin.
   * Informs all registered consumers that a plugin is no longer available. This code is executed on the Swing EDT!
   * @param plugin
   */
  def remove(plugin: OtActionPlugin): Unit = {
    println("Removing plugin: " + plugin.name)
    if (pluginSet.contains(plugin)) {
      pluginSet = pluginSet - plugin
      consumers.foreach { _.uninstall(plugin) }
    }
  }

}
