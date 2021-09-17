package tech.mlsql.app_runtime.plugin

import net.csdn.ServiceFramwork
import net.csdn.bootstrap.Application
import tech.mlsql.common.utils.shell.command.ParamsUtil
import tech.mlsql.serviceframework.platform.{ AppRuntimeStore, Plugin, PluginItem, PluginLoader, PluginType}
import tech.mlsql.serviceframework.platform.app.AppManager
import scala.collection.JavaConverters._


object App {
  def main(args: Array[String]): Unit = {
    val applicationYamlName = "application.yml"
    ServiceFramwork.applicaionYamlName(applicationYamlName)
    ServiceFramwork.scanService.setLoader(classOf[App])
    ServiceFramwork.enableNoThreadJoin()

    val plugin = new PluginDesc
    plugin.registerForTest()

    Application.main(args)
    Thread.currentThread().join()

  }

}

class App {

}
