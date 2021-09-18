package tech.mlsql.app_runtime.db.action

import net.csdn.jpa.QuillDB
import net.csdn.jpa.QuillDB.ctx
import net.csdn.jpa.QuillDB.ctx._
import org.apache.http.client.fluent.{Form, Request}
import tech.mlsql.app_runtime.db.quill_model.{DictStore, DictType}
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction
import tech.mlsql.serviceframework.platform.form.{Editor, FormParams, Input, KV}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 21/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class AddDBAction extends DBBaseAction {
  override def _run(params: Map[String, String]): String = {
    val instanceNameOpt = params.get(AddDBAction.Params.INSTANCE_NAME.name)
    val dbName = params(AddDBAction.Params.DB_NAME.name)
    val dbConfig = params.get(AddDBAction.Params.DB_CONFIG.name) match {
      case Some(item) => item
      case None => AddDBAction.dbTemplate(params(AddDBAction.Params.HOST.name),
        params(AddDBAction.Params.PORT.name),
        dbName,
        params(AddDBAction.Params.USER_NAME.name),
        params(AddDBAction.Params.PASSWORD.name)
      )
    }

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
    JSONTool.toJsonStr(ctx.run(ctx.query[DictStore]))
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(AddDBAction.Params).toList.reverse)
  }

}

object AddDBAction {

  object Params {
    val ADMIN_TOKEN = Input("admin_token", "")
    val INSTANCE_NAME = Input("instanceName", "")
    val DB_NAME = Input("dbName", "")

    val HOST = Input("host", "")
    val PORT = Input("port", "")
    val USER_NAME = Input("username", "")
    val PASSWORD = Input("password", "")

    val DB_CONFIG = Editor("dbConfig", values = List(), valueProvider = Option(() => {
      List(KV(Option("yaml"), Option("")))
    }))
  }

  def dbTemplate(host: String, port: String, database: String, username: String, password: String) = {
    s"""
       |${database}:
       |  host: ${host}
       |  port: ${port}
       |  database: ${database}
       |  username: ${username}
       |  password: ${password}
       |  initialSize: 8
       |  disable: false
       |  removeAbandoned: true
       |  testWhileIdle: true
       |  removeAbandonedTimeout: 30
       |  maxWait: 100
       |  filters: stat,log4j
       |""".stripMargin
  }

  def action = "add/db"

  def plugin = PluginItem(action, classOf[AddDBAction].getName, PluginType.action, None)
}

class LoadDBAction extends CustomAction {
  override def run(params: Map[String, String]): String = {

    BasicDBService.fetchDB(params("name")) match {
      case Some(db) => QuillDB.createNewCtxByNameFromStr(db.name, db.value)
    }
    JSONTool.toJsonStr(Map())
  }
}

object LoadDBAction {

  object Params {
    val DB_NAME = Input("name", "")
  }

  def action="db/load"
  def plugin = PluginItem(action, classOf[LoadDBAction].getName, PluginType.action, None)

}

class AddProxyAction extends DBBaseAction {
  override def _run(params: Map[String, String]): String = {

    BasicDBService.fetch(params("name"), DictType.INSTANCE_TO_INSTANCE_PROXY) match {
      case Some(db) =>
        ctx.run(BasicDBService.lazyFetch(params("name"), DictType.INSTANCE_TO_INSTANCE_PROXY).
          update(_.value -> lift(params("value"))))
      case None =>
        BasicDBService.addItem(params("name"), params("value"), DictType.INSTANCE_TO_INSTANCE_PROXY)
    }
    JSONTool.toJsonStr(ctx.run(ctx.query[DictStore]))
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(AddProxyAction.Params).toList.reverse)
  }
}

object AddProxyAction {

  object Params {
    val ADMIN_TOKEN = Input("admin_token", "")
    val INSTANCE_NAME = Input("name", "")
    val INSTANCE_URL = Input("value", "")
  }

  def action = "addProxy"

  def plugin = PluginItem(action, classOf[AddProxyAction].getName, PluginType.action, None)
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

