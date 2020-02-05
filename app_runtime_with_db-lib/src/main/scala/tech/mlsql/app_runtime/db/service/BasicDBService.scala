package tech.mlsql.app_runtime.db.service

import net.csdn.ServiceFramwork
import net.csdn.common.settings.Settings
import net.csdn.jpa.QuillDB.ctx
import net.csdn.jpa.QuillDB.ctx._
import tech.mlsql.app_runtime.db.quill_model.DictType.DictType
import tech.mlsql.app_runtime.db.quill_model.{DictStore, DictType}

object BasicDBService {

  def isDBSupport = {
    val settings = ServiceFramwork.injector.getInstance(classOf[Settings])

    !settings.getAsBoolean(settings.get("mode") + ".datasources.mysql.disable", false)
  }

  def adminToken = {
    ServiceFramwork.injector.getInstance(classOf[Settings]).get("admin_token")
  }

  def canAccess(token: String) = {
    adminToken == token
  }

  def fetchDB(name: String) = {
    fetch(name, DictType.DB)
  }

  def fetch(name: String, dictType: DictType) = {
    ctx.run(lazyFetch(name, dictType)).headOption
  }

  def addItem(name: String, value: String, dictType: DictType) = {
    fetch(name, dictType) match {
      case Some(_) => ctx.run(lazyFetch(name, dictType).update(_.value -> lift(value)))
      case None => ctx.run(ctx.query[DictStore].insert(
        _.name -> lift(name),
        _.value -> lift(value),
        _.dictType -> lift(dictType.id)))
    }
  }

  def lazyFetch(name: String, dictType: DictType) = {
    quote {
      ctx.query[DictStore].filter { p =>
        p.name == lift(name) && p.dictType == lift(dictType.id)
      }
    }
  }
}
