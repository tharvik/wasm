package scalawasm.text

import scalawasm.binary.LEB128.Type._

import scalawasm.text.{BinaryAst => B}
import scalawasm.text.BinaryAst.{Section => BSec}
import scalawasm.text.BinaryAst.{Signature => BSig}
import scalawasm.text.BinaryAst.{Type => BT}

object ToBinary {

  object Type {
    def toBinary(o: B.Type): Stream[Byte] = varint7(o match {
      case BT.Value.i32 => -0x01
      case BT.Value.i64 => -0x02
      case BT.Value.f32 => -0x03
      case BT.Value.f64 => -0x04
      case BT.AnyFunction => -0x10
      case BT.Function => -0x20
      case BT.Empty => -0x40
    }) pack

    def toBinary(f: BSig.Function): Stream[Byte] = {
      toBinary(BT.Function) #:::
        varuint32(f.params size).pack #:::
        f.params.flatMap { toBinary }.toStream #:::
        varuint1(if (f.returns.nonEmpty) 1 else 0).pack #:::
        f.returns.flatMap { toBinary }.toStream
    }
  }

  private def toBinary(s: BSec): Stream[Byte] = {
    val (id: Option[Int], payload: Stream[Byte]) = s match {
      case BSec.Type(types) =>
        if (types.nonEmpty)
          (Some(0x01), varuint32(types.length).pack #:::
            (types flatMap { Type.toBinary } toStream))
        else
          (None, Stream.empty)
      /*case AS.Import(entries: Seq[AS.Content.import_entry])  =>
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
          (entries flatMap { toBinary } toStream)*/
    }

    val w = varuint32(payload.length).pack toList
    val r = varint32(payload.length).pack toList
    val p = payload toList

    id.map { i =>
      varuint7(i toShort).pack #:::
        varuint32(payload.length).pack #:::
        payload
    } getOrElse Stream.empty
  }

  def apply(m: B.Preamble): Stream[Byte] = {
    val magic = Seq(0x00, 'a', 's', 'm')
    val version = 1

    magic.map(_.toByte).toStream #:::
      uint32(version).pack #:::
      (m.sections flatMap toBinary).toStream
  }
}
