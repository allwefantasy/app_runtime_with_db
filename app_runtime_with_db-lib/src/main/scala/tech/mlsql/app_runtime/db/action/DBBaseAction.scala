package tech.mlsql.app_runtime.db.action

import tech.mlsql.serviceframework.platform.form.Input
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction}

abstract class DBBaseAction extends CustomAction {
  override def run(params: Map[String, String]): String = {
    if (params.contains(DBBaseAction.Params.HELP.name)) {
      _help()
    }
    else {
      if (!BasicDBService.canAccess(params("admin_token"))) {
        render(400, "admin_token is required")
      }
      _run(params)
    }
  }

  def _run(params: Map[String, String]): String

  def _help(): String

  def render(status: Int, content: String): String = {
    val context = ActionContext.context()
    render(context.httpContext.response, status, content)
    ""
  }

  def renderEmpty(): String = {
    render(200, JSONTool.toJsonStr(List(Map())))
    ""
  }


  def paramEmptyAsNone(params: Map[String, String], name: String) = {
    params.get(name) match {
      case Some(value) => if (value.isEmpty) None else Some(value)
      case None => None
    }
  }
}

object DBBaseAction {

  object Params {
    val HELP = Input("__HELP__", "")
    val ADMIN_TOKEN = Input("admin_token", "")
  }

}
