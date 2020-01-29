package tech.mlsql.app_runtime.db.action

import net.csdn.jpa.QuillDB
import net.csdn.jpa.QuillDB.ctx
import net.csdn.jpa.QuillDB.ctx._
import tech.mlsql.app_runtime.db.quill_model.{DictStore, DictType}
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction

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

