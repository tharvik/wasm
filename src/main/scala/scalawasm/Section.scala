package scalawasm

import scalawasm.sections.import_entry
import scalawasm.binary.Alias._
import scalawasm.binary.func_type

abstract class Section(id: Int, name: Option[String] = None) extends Term {
  require((id == 0 && name.isDefined) || (id > 0 && name.isEmpty))

  def toBinary(payload: Stream[Byte]): Stream[Byte] = {
    val namePacked: Array[Byte] = name map { _.toCharArray() map { b => b.toByte } } getOrElse(Array())
    val nameStream: Stream[Byte] = namePacked.toStream

    varuint7(id).pack #:::
      varuint32(payload.length).pack #:::
      varuint32(namePacked.length).pack #:::
      nameStream #:::
      payload
  }
}

object Section {
  //case class Custom(name: String) extends Section(0x00, Some(name))
  final case class Type(entries: Seq[func_type]) extends Section(0x01) {
    override def toBinary = toBinary {
      varuint32(entries.length).pack #:::
        (entries flatMap { _.toBinary } toStream)
    }
  }

  final case class Import(entries: Seq[import_entry]) extends Section(0x02) {
    override def toBinary = toBinary {
      varuint32(entries.length).pack #:::
        (entries flatMap { _.toBinary } toStream)
    }
  }

  // TODO add others
}
