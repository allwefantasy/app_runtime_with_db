package tech.mlsql.app_runtime.plugin

import tech.mlsql.app_runtime.db.action.{AddDBAction, AddProxyAction, LoadDBAction}
import tech.mlsql.serviceframework.platform._

class PluginDesc extends Plugin {
  override def entries: List[PluginItem] = {
    List(
      AddDBAction.plugin,
      LoadDBAction.plugin,
      AddProxyAction.plugin
    )
  }

  def registerForTest() = {
    val pluginLoader = PluginLoader(Thread.currentThread().getContextClassLoader, this)
    entries.foreach { item =>
      AppRuntimeStore.store.registerAction(item.name, item.clzzName, pluginLoader)
    }
  }
}
