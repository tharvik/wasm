package scalawasm.sections

import scalawasm.BinaryEncoding
import scalawasm.binary.Types.global_type

// FIXME temporary to document correctly
sealed trait init_expr extends BinaryEncoding

final case class global_variable(varType: global_type, init: init_expr) extends BinaryEncoding {
  override def toBinary = varType.toBinary #::: init.toBinary
}
