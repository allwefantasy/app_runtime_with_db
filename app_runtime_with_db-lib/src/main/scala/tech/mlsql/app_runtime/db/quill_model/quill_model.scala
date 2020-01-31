package tech.mlsql.app_runtime.db.quill_model

case class DictStore(id: Int, var name: String, var value: String, var dictType: Int)

object DictType extends Enumeration {
  type DictType = Value
  val DB = Value(0)
  val SYSTEM_CONFIG = Value(1)
  // 实例（一组）到数据库
  val INSTANCE_TO_DB = Value(2)
  // 实例（一组）到另外一组实例代理
  val INSTANCE_TO_INSTANCE_PROXY = Value(3)
}
