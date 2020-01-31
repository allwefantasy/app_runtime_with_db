package tech.mlsql.app_runtime.db.action

import net.csdn.jpa.QuillDB
import net.csdn.jpa.QuillDB.ctx
import net.csdn.jpa.QuillDB.ctx._
import org.apache.http.client.fluent.{Form, Request}
import tech.mlsql.app_runtime.db.quill_model.{DictStore, DictType}
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 21/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class AddDBAction extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val instanceNameOpt = params.get("instanceName")
    val dbName = params("dbName")
    val dbConfig = params("dbConfig")


    def updateOrInsertRelatedInstance = {
      instanceNameOpt match {
        case Some(instanceName) =>
          BasicDBService.fetch(instanceName, DictType.INSTANCE_TO_DB) match {
            case Some(instanceRef) =>
              ctx.run(ctx.query[DictStore].filter { p =>
                p.name == lift(instanceName) && p.dictType == lift(DictType.INSTANCE_TO_DB.id)
              }.update(_.value -> lift(dbName)))
            case None =>
              ctx.run(ctx.query[DictStore].insert(
                lift(DictStore(0, instanceName, dbName, DictType.INSTANCE_TO_DB.id))))
          }

        case None =>

      }
    }

    BasicDBService.fetchDB(dbName) match {
      case Some(dbDict) =>
        ctx.transaction {
          ctx.run(ctx.query[DictStore].filter(_.name == lift(dbDict.name)).
            update(_.value -> lift(dbConfig)))
          updateOrInsertRelatedInstance
        }


      case None =>
        ctx.transaction {
          ctx.run(ctx.query[DictStore].insert(
            _.name -> lift(dbName),
            _.value -> lift(dbConfig),
            _.dictType -> lift(DictType.DB.id)))
          updateOrInsertRelatedInstance
        }

    }
    JSONTool.toJsonStr(Map())
  }

}

class LoadDBAction extends CustomAction {
  override def run(params: Map[String, String]): String = {
    BasicDBService.fetchDB(params("name")) match {
      case Some(db) => QuillDB.createNewCtxByNameFromStr(db.name, db.value)
    }
    JSONTool.toJsonStr(Map())
  }
}

class AddProxyAction extends CustomAction {
  override def run(params: Map[String, String]): String = {
    BasicDBService.fetch(params("name"), DictType.INSTANCE_TO_INSTANCE_PROXY) match {
      case Some(db) =>
        ctx.run(BasicDBService.lazyFetch(params("name"), DictType.INSTANCE_TO_INSTANCE_PROXY).
          update(_.value -> lift(params("value"))))
      case None =>
        BasicDBService.addItem(params("name"), params("value"), DictType.INSTANCE_TO_INSTANCE_PROXY)
    }
    JSONTool.toJsonStr(Map())
  }
}

object AddProxyAction {
  def plugin = PluginItem(action, classOf[AddProxyAction].getName, PluginType.action, None)

  def action = "addProxy"
}

class BasicActionProxy(pluginName: String) {
  lazy val current_url = {
    val item = BasicDBService.fetch(pluginName, DictType.INSTANCE_TO_INSTANCE_PROXY)
    if (item.isEmpty) throw new RuntimeException(s"You have not configure proxy for ${pluginName} yet," +
      s", this means you can not access it vise http in other plugin")
    item.head.value
  }

  def run(action: String, params: Map[String, String]): String = {
    val form = Form.form()
    form.add("action", action)
    params.filterNot(_._1 == "action").foreach(f => form.add(f._1, f._2))
    val res = Request.Post(current_url).connectTimeout(10 * 1000)
      .socketTimeout(10 * 1000).bodyForm(form.build())
      .execute().returnContent().asString()
    return res
  }
}

object BasicDBActionProxy {
  lazy val proxy = new BasicActionProxy("app_runtime_with_db")
}

