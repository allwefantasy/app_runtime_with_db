package tech.mlsql.app_runtime.plugin

import tech.mlsql.app_runtime.db.action.{AddDBAction, AddProxyAction, LoadDBAction}
import tech.mlsql.serviceframework.platform.{Plugin, PluginItem}

/**
 * 18/9/2021 WilliamZhu(allwefantasy@gmail.com)
 */
class DBPluginDesc extends Plugin {
  override def entries: List[PluginItem] = {
    List(
      AddDBAction.plugin,
      LoadDBAction.plugin,
      AddProxyAction.plugin
    )
  }
}
