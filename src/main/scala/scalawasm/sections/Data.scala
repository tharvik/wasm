package scalawasm.sections

import scalawasm.BinaryEncoding
import scalawasm.binary.Types._

final case class data_segment(index: Int, offset: init_expr, data: Seq[Byte]) extends BinaryEncoding {
  override def toBinary = {
    varuint32(index).pack #:::
      offset.toBinary #:::
      varuint32(data.size).pack #:::
      data.toStream
  }
}
