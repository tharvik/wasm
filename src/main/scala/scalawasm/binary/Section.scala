package scalawasm.binary

import scalawasm.ast.Opcode.End
import scalawasm.{ast => A}
import scalawasm.{binary => B}
import scalawasm.ast.{Section => AS}
import scalawasm.binary.LEB128.Type._

object Section {

  def toBinary(s: A.Section): Stream[Byte] = {
    val namePacked: Array[Byte] = s.name map { _.getBytes("UTF-8") } getOrElse Array()

    val payload: Stream[Byte] = s match {
    case AS.Type(entries: Seq[A.Type.Function]) =>
      varuint32(entries.length).pack #:::
        (entries flatMap { B.Type.toBinary } toStream)
      case AS.Import(entries: Seq[AS.Content.import_entry])  =>
          varuint32(entries.length).pack #:::
            (entries flatMap { toBinary } toStream)
      case AS.Function(indices) =>
          varuint32(indices.length).pack #:::
            (indices flatMap { varuint32(_).pack } toStream)
      case AS.Table(entries) =>
          varuint32(entries.length).pack #:::
            (entries flatMap { B.Type.toBinary } toStream)
      case AS.Memory(entries) =>
          varuint32(entries.length).pack #:::
            (entries flatMap { B.Type.toBinary } toStream)
      case AS.Global(globals) =>
            varuint32(globals.length).pack #:::
              (globals flatMap { B.Section.toBinary } toStream)
      case AS.Export(field, kind, index) =>
          val fieldUTF8 = field.getBytes("UTF-8")
          varuint32(fieldUTF8.length).pack #:::
            fieldUTF8.toStream #:::
            B.Type.toBinary(kind) #:::
            varuint32(index).pack
      case AS.Start(index) =>
          varuint32(index).pack
      case AS.Element(entries) =>
          varuint32(entries.size).pack #:::
            (entries flatMap { toBinary } toStream)
      case AS.Code(bodies) =>
          varuint32(bodies.size).pack #:::
            (bodies flatMap { toBinary } toStream)
      case AS.Data(entries) =>
          varuint32(entries.size).pack #:::
            (entries flatMap { toBinary } toStream)
    }

    varuint7(s.id).pack #:::
      varuint32(payload.length).pack #:::
      varuint32(namePacked.length).pack #:::
      namePacked.toStream #:::
      payload
  }

  def toBinary(d: AS.Content.data_segment): Stream[Byte] =
    varuint32(d.index).pack #:::
      toBinary(d.offset) #:::
      varuint32(d.data.size).pack #:::
      (d.data flatMap { B.Opcode.toBinary } toStream)

  def toBinary(e: AS.Content.elem_segment): Stream[Byte] =
    varuint32(0).pack #:::
      toBinary(e.offset) #:::
      varuint32(e.elems size).pack #:::
      e.elems.flatMap { varuint32(_) pack }.toStream

  def toBinary(f: AS.Content.function_body): Stream[Byte] =
    varuint32(f.code.size).pack #:::
      varuint32(f.locals.size).pack #:::
      f.locals.flatMap { toBinary } .toStream #:::
      f.code.toStream #:::
      B.Opcode.toBinary(End)

  def toBinary(f: AS.Content.local_entry): Stream[Byte] =
    varuint32(f.count).pack #:::
      B.Type.Dispatcher.toBinary(f.tpe)

  def toBinary(e: AS.Content.init_expr): Stream[Byte] = ??? // TODO implement it

  def toBinary(f: AS.Content.global_variable): Stream[Byte] =
    B.Type.toBinary(f.varType) #::: toBinary(f.init)

  object import_entry {
    def toBinary(module: String, field: String): Stream[Byte] = {
      val moduleUTF8 = module.getBytes("UTF-8")
      val fieldUTF8 = field.getBytes("UTF-8")

      varuint32(moduleUTF8.length).pack #:::
        moduleUTF8.toStream #:::
        varuint32(fieldUTF8.length).pack #:::
        fieldUTF8.toStream
    }

    def toBinary(e: AS.Content.import_entry_function): Stream[Byte] =
      import_entry.toBinary(e.module, e.field) #:::
        B.Type.toBinary(A.Type.external_kind.Function) #:::
        varuint32(e.funcTypeIndex).pack

    def toBinary(e: AS.Content.import_entry_table): Stream[Byte] =
      import_entry.toBinary(e.module, e.field) #:::
        B.Type.toBinary(A.Type.external_kind.Table) #:::
        B.Type.toBinary(e.importType)

    def toBinary(e: AS.Content.import_entry_memory): Stream[Byte] =
      import_entry.toBinary(e.module, e.field) #:::
        B.Type.toBinary(A.Type.external_kind.Memory) #:::
        B.Type.toBinary(e.importType)

    def toBinary(e: AS.Content.import_entry_global): Stream[Byte] =
      import_entry.toBinary(e.module, e.field) #:::
        B.Type.toBinary(A.Type.external_kind.Global) #:::
        B.Type.toBinary(e.importType)
  }
  def toBinary(entry: AS.Content.import_entry): Stream[Byte] = entry match {
    case e: AS.Content.import_entry_function => B.Section.import_entry.toBinary(e)
    case e: AS.Content.import_entry_table => B.Section.import_entry.toBinary(e)
    case e: AS.Content.import_entry_memory => B.Section.import_entry.toBinary(e)
    case e: AS.Content.import_entry_global => B.Section.import_entry.toBinary(e)
  }
}
