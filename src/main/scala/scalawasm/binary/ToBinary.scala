package scalawasm.binary

import scalawasm.Config
import scalawasm.ast.Binary.Opcode._
import scalawasm.ast.Binary.{Section => BSec, Signature => BSig}
import scalawasm.ast.{Binary => B, Type => AT}
import scalawasm.{ast => A}
import scalawasm.binary.LEB128.Type._
import scalawasm.binary.{ToBinary => This}

object ToBinary {
  private def toBinary(rl: A.ResizableLimits) =
    varuint1(if (rl.maximum.isDefined) 1 else 0).pack #:::
      varuint32(rl.initial).pack #:::
      rl.maximum.map(varuint32(_).pack).getOrElse(Stream())

  object Type {
    def toBinary(t: AT): Stream[Byte] = {
      def op(i: Short): Stream[Byte] =
        varint7(i).pack

      t match {
        case AT.i32 => op(-0x01)
        case AT.i64 => op(-0x02)
        case AT.f32 => op(-0x03)
        case AT.f64 => op(-0x04)
        case AT.AnyFunction => op(-0x10)
        case AT.Empty => op(-0x40)
      }
    }
  }

  private def toBinary(k: A.Kind): Stream[Byte] = uint8(k match {
    case A.Kind.Function => 0
    case A.Kind.Table => 1
    case A.Kind.Memory => 2
    case A.Kind.Global => 3
  }).pack

  object Signature {
    private def op(i: Short): Stream[Byte] = varint7(i).pack

    def toBinary(f: BSig.Function): Stream[Byte] =
        op(-0x20) #:::
          varuint32(f.params size).pack #:::
          f.params.flatMap { Type.toBinary }.toStream #:::
          varuint1(if (f.returns.nonEmpty) 1 else 0).pack #:::
          f.returns.flatMap { Type.toBinary }.toStream

    def toBinary(g: BSig.Global): Stream[Byte] = {
      Type.toBinary(g.content_type) #:::
        varuint1(if (g.mutability) 1 else 0).pack
    }

    def toBinary(t: BSig.Table): Stream[Byte] =
        Type.toBinary(t.elem_type) #:::
          This.toBinary(t.resizable_limits)

    def toBinary(m: BSig.Memory): Stream[Byte] =
      This.toBinary(m.resizable_limits)
  }


  private def toBinaryTail(imp: BSig.Import): Stream[Byte] = {
    val (kind, tail) = imp match {
      case BSig.Import.Function(_, _, i) => (A.Kind.Function, varuint32(i).pack)
      case BSig.Import.Table(_, _, s) => (A.Kind.Table, Signature.toBinary(s))
      case BSig.Import.Memory(_, _, s) => (A.Kind.Memory, Signature.toBinary(s))
      case BSig.Import.Global(_, _, s) => (A.Kind.Global, Signature.toBinary(s))
    }

    toBinary(kind) #::: tail
  }

  def pack(s: String): Stream[Byte] = {
    val bytes = s.getBytes("UTF-8")

    varuint32(bytes.length).pack #:::
      bytes.toStream
  }

  private def toBinary(expr: Seq[B.Opcode]): Stream[Byte] = expr.flatMap(toBinary).toStream #::: toBinary(B.Opcode.End)
  private def toBinary(opcode: B.Opcode): Stream[Byte] = {
    def typeOffset(t: A.Type): Int = t match {
      case A.Type.i32 => 0
      case A.Type.i64 => 1
      case A.Type.f32 => 2
      case A.Type.f64 => 3
    }

    def op(id: Int) = uint8(id toByte).pack

    opcode match {
      case Unreachable => op(0x00)
      case Nop => op(0x01)
      case Block(t) => op(0x02) #::: Type.toBinary(t)
      case Loop(t) => op(0x03) #::: Type.toBinary(t)
      case If(t) => op(0x04) #::: Type.toBinary(t)
      case Else => op(0x05)
      case End => op(0x0b)
      // TODO add others
      case Const(A.Type.i32, A.Value.Integral(v)) => op(0x41) #::: varint32(v.toInt).pack
      case Const(A.Type.i64, A.Value.Integral(v)) => op(0x42) #::: varint64(v).pack
      case Const(A.Type.f32, A.Value.Floating(v)) => op(0x43) #::: uint32(java.lang.Float.floatToIntBits(v.toFloat)).pack
      case Const(A.Type.f64, A.Value.Floating(v)) => op(0x44) #::: uint64(java.lang.Double.doubleToLongBits(v)).pack
    }
  }

  private def toBinary(s: BSec): Stream[Byte] = {
    val (id: Option[Int], content: Seq[Any], payload: Stream[Byte]) = s match {
      // TODO better than wrapping in if/else
      case BSec.Type(types) =>
        (Some(0x01), types,
          varuint32(types.length).pack #:::
            types.flatMap { Signature.toBinary }.toStream)
      case BSec.Import(entries) =>
        (Some(0x02), entries,
          varuint32(entries.length).pack #:::
            entries.flatMap { i =>
              pack(i.module) #:::
                pack(i.field) #:::
                toBinaryTail(i) }.toStream)
      case BSec.Function(indexes) =>
        (Some(0x03), indexes, varuint32(indexes.length).pack #:::
          indexes.map(_.toByte).toStream)
      /*case AS.Table(entries) =>
        varuint32(entries.length).pack #:::
          (entries flatMap { B.Type.toBinary } toStream)
      case AS.Memory(entries) =>
        varuint32(entries.length).pack #:::
          (entries flatMap { B.Type.toBinary } toStream)*/
      case BSec.Global(globals) =>
        (Some(0x06), globals, varuint32(globals.length).pack #:::
          globals.flatMap { case (g, expr) => Signature.toBinary(g) #::: toBinary(expr) }.toStream)
      case BSec.Export(exports) =>
        (Some(0x07), exports, varuint32(exports.length).pack #:::
          exports.flatMap { case (field, k, i) => pack(field) #::: toBinary(k) #::: varuint32(i).pack }.toStream)
      /*case AS.Start(index) =>
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
        ((0 to 3).map { i => ((payload.length >> (i * 7)) & 0x7F) | 0x80 } :+ 0x00).map(_.toByte).toStream
      else
        varuint32(payload.length).pack

    id.map { i =>
      if (content.isEmpty)
        Stream.empty
      else
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
