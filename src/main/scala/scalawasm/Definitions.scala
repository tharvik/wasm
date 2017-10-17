package scalawasm

import scalawasm.Utils.{packToUint32, uint32ToStream}
import scalawasm.binary.Types.uint32


trait BinaryEncoding {
  def toBinary: Stream[Byte]
}

abstract class Term extends BinaryEncoding






final case class Preamble(sections: Seq[Section]) extends Term {
  override def toBinary = {
    val MagicNumber: uint32 = (0x00, 'a', 's', 'm')
    val Version: uint32 = packToUint32(1)

    uint32ToStream(MagicNumber) #:::
    uint32ToStream(Version) #:::
    (sections flatMap {s => s toBinary}).toStream
  }
}








// TODO final case class init_expr()
// TODO do not use uint/varuint/... outside
