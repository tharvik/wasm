package scalawasm.sections

import scalawasm._
import scalawasm.binary._
import scalawasm.binary.Alias._

sealed trait import_entry extends BinaryEncoding {
  val module: String
  val field: String

  override def toBinary = {
    val moduleUTF8 = module.getBytes("UTF-8")
    val fieldUTF8 = field.getBytes("UTF-8")

    varuint32(moduleUTF8.length).pack #:::
      moduleUTF8.toStream #:::
      varuint32(fieldUTF8.length).pack #:::
      fieldUTF8.toStream
  }
}


// TODO auto-discovery of index
final case class import_entry_function(module: String, field: String, funcTypeIndex: Int) extends import_entry {
  override def toBinary = {
    super.toBinary #:::
      external_kind.Function.toBinary #:::
      varuint32(funcTypeIndex).pack
  }
}

final case class import_entry_table(module: String, field: String, importType: table_type) extends import_entry {
  override def toBinary = {
    super.toBinary #:::
      external_kind.Table.toBinary #:::
      importType.toBinary
  }
}

final case class import_entry_memory(module: String, field: String, importType: memory_type) extends import_entry {
  override def toBinary = {
    super.toBinary #:::
      external_kind.Memory.toBinary #:::
      importType.toBinary
  }
}

final case class import_entry_global(module: String, field: String, importType: global_type) extends import_entry {
  override def toBinary = {
    super.toBinary #:::
      external_kind.Memory.toBinary #:::
      importType.toBinary
  }
}
