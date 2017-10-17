package scalawasm.sections

import scalawasm.BinaryEncoding
import scalawasm.binary.Types._
import scalawasm.binary.End

final case class function_body(locals: Seq[local_entry], code: Seq[Byte]) extends BinaryEncoding {
  override def toBinary = {
    // FIXME is it code.size? with or without the "end" byte
    varuint32(code.size).pack #:::
      varuint32(locals.size).pack #:::
      locals.flatMap{_.toBinary}.toStream #:::
      code.toStream #:::
      Stream(End().opcode)
  }
}

final case class local_entry(count: Int, tpe: value_type) extends BinaryEncoding {
  override def toBinary = {
    varuint32(count).pack #:::
      tpe.toBinary
  }
}
