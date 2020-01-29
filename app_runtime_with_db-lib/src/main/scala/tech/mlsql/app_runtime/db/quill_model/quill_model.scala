package tech.mlsql.app_runtime.db.quill_model

case class DictStore(id: Int, var name: String, var value: String, var dictType: Int)

object DictType extends Enumeration {
  type DictType = Value
  val DB = Value(0)
  val SYSTEM_CONFIG = Value(1)
  val INSTANCE_TO_DB = Value(2)
}
