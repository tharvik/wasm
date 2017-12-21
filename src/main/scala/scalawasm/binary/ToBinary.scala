package scalawasm.binary

import scalawasm.Config
import scalawasm.ast.Binary.{Section => BSec, Signature => BSig, Type => BT}
import scalawasm.ast.{Binary => B}
import scalawasm.binary.LEB128.Type._

object ToBinary {
  object Type {

    def toBinary(rl: B.ResizableLimits) =
      varuint1(if (rl.maximum.isDefined) 1 else 0).pack #:::
        varuint32(rl.initial).pack #:::
        rl.maximum.map(varuint32(_).pack).getOrElse(Stream())

    def toBinary(t: B.Type): Stream[Byte] = {
      def op(i: Short): Stream[Byte] =
        varint7(i).pack

      t match {
        case BT.i32 => op(-0x01)
        case BT.i64 => op(-0x02)
        case BT.f32 => op(-0x03)
        case BT.f64 => op(-0x04)
        case BT.AnyFunction => op(-0x10)
        case f: BT.Function =>
          op(-0x20) #:::
            varuint32(f.params size).pack #:::
            f.params.flatMap { toBinary }.toStream #:::
            varuint1(if (f.returns.nonEmpty) 1 else 0).pack #:::
            f.returns.flatMap { toBinary }.toStream
        case BT.Empty => op(-0x40)
        case BT.Global(v, m) =>
          toBinary(v) #:::
            varuint1(if (m) 1 else 0).pack
        case BT.Table(t, rl) =>
          toBinary(t) #:::
            toBinary(rl)
        case BT.Memory(rl) => toBinary(rl)
      }
    }
  }

  private def toBinaryTail(imp: BSig.Import): Stream[Byte] = imp match {
    case BSig.Import.Function(_, _, i) => varuint32(i).pack
    case BSig.Import.Table(_, _, t) => Type.toBinary(t)
    case BSig.Import.Memory(_, _, t) => Type.toBinary(t)
    case BSig.Import.Global(_, _, t) => Type.toBinary(t)
  }
  private def toBinary(k: BT.Kind): Stream[Byte] = uint8(k match {
    case BT.Kind.Function => 0
    case BT.Kind.Table => 1
    case BT.Kind.Memory => 2
    case BT.Kind.Global => 3
  }).pack

  def pack(s: String): Stream[Byte] = {
    val bytes = s.getBytes("UTF-8")

    varuint32(bytes.length).pack #:::
      bytes.toStream
  }

  private def toBinary(s: BSec): Stream[Byte] = {
    val (id: Option[Int], payload: Stream[Byte]) = s match {
      case BSec.Type(types) =>
        if (types.nonEmpty)
          (Some(0x01),
            varuint32(types.length).pack #:::
              types.flatMap { Type.toBinary }.toStream)
        else
          (None, Stream.empty)
      case BSec.Import(entries)  =>
        if (entries.nonEmpty)
          (Some(0x02),
            varuint32(entries.length).pack #:::
              entries.flatMap { i =>
                pack(i.module) #:::
                  pack(i.field) #:::
                  toBinary(i.kind) #:::
                  toBinaryTail(i) }.toStream)
        else
          (None, Stream.empty)
      /*case AS.Function(indices) =>
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

    // TODO quirk spec#625
    val size: Stream[Byte] =
      if (Config.enableSpecCompat)
        Stream(0x80 | payload.length, 0x80, 0x80, 0x80, 0x00) map { i => i.toByte }
      else
        varuint32(payload.length).pack

    id.map { i =>
      varuint7(i toShort).pack #:::
        size #::: payload
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
