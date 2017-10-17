package scalawasm.sections

import scalawasm.binary.Types.varuint32
import scalawasm.BinaryEncoding

final case class elem_segment(offset: init_expr, elems: Seq[Int]) extends BinaryEncoding {
  // TODO require(offset.return == i32)
  override def toBinary = {
    varuint32(0).pack #:::
      offset.toBinary #:::
      varuint32(elems size).pack #:::
      elems.flatMap {varuint32(_) pack}.toStream
  }
}
