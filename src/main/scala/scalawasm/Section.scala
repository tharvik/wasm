package scalawasm

import scalawasm.sections._
import scalawasm.binary.Types._

abstract class Section(id: Short, name: Option[String] = None) extends Term {
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
  final case class Type(entries: Seq[func_type]) extends Section(1) {
    override def toBinary = toBinary {
      varuint32(entries.length).pack #:::
        (entries flatMap { _.toBinary } toStream)
    }
  }

  final case class Import(entries: Seq[import_entry]) extends Section(2) {
    override def toBinary = toBinary {
      varuint32(entries.length).pack #:::
        (entries flatMap { _.toBinary } toStream)
    }
  }

  // TODO auto discovery of indices
  final case class Function(indices: Seq[Int]) extends Section(3) {
    override def toBinary = toBinary {
      varuint32(indices.length).pack #:::
        (indices flatMap { e => varuint32(e).pack } toStream)
    }
  }

  final case class Table(entries: Seq[table_type]) extends Section(4) {
    require(entries.size <= 1)
    override def toBinary = toBinary {
      varuint32(entries.length).pack #:::
        (entries flatMap { _.toBinary } toStream)
    }
  }

  final case class Memory(entries: Seq[memory_type]) extends Section(5) {
    require(entries.size <= 1)
    override def toBinary = toBinary {
      varuint32(entries.length).pack #:::
        (entries flatMap { _.toBinary } toStream)
    }
  }

  final case class Global(globals: Seq[global_variable]) extends Section(6) {
    override def toBinary = toBinary {
      varuint32(globals.length).pack #:::
        (globals flatMap { _.toBinary } toStream)
    }
  }

  // TODO auto compute index
  final case class Export(field: String, kind: external_kind, index: Int) extends Section(7) {
    override def toBinary = toBinary {
      val fieldUTF8 = field.getBytes("UTF-8")

      varuint32(fieldUTF8.length).pack #:::
        fieldUTF8.toStream #:::
        kind.toBinary #:::
        varuint32(index).pack
    }
  }

  final case class Start(index: Int) extends Section(8) {
    override def toBinary = toBinary(varuint32(index).pack)
  }

  final case class Element(entries: Seq[elem_segment]) extends Section(9) {
    override def toBinary = toBinary {
      varuint32(entries.size).pack #:::
        entries.flatMap{_.toBinary}.toStream
    }
  }

  // TODO "The count of function declared in the function section and function
  // bodies defined in this section must bethe same and the ith declaration
  // corresponds to the ith function body"
  final case class Code(bodies: Seq[function_body]) extends Section(10) {
    override def toBinary = toBinary {
      varuint32(bodies.size).pack #:::
        bodies.flatMap{_.toBinary}.toStream
    }
  }

  final case class Data(entries: Seq[data_segment]) extends Section(11) {
    override def toBinary = toBinary {
      varuint32(entries.size).pack #:::
        entries.flatMap { _.toBinary }.toStream
    }
  }

  // TODO add "name" extra-section
}
