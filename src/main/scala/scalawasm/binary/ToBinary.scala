package scalawasm.binary

import scalawasm.Config.enableSpecCompat
import scalawasm.ast.Binary.Opcode.Load.SizeAndSign
import scalawasm.ast.Binary.Opcode._
import scalawasm.ast.Binary.{Section => BSec, Signature => BSig}
import scalawasm.ast.{Binary => B, Type => AT}
import scalawasm.{ast => A}
import scalawasm.binary.LEB128.Type._
import scalawasm.binary.{ToBinary => This}

object ToBinary {

  private def log2(x: Long): Int = (Math.log(x) / Math.log(2)).toInt // TODO toInt

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

  private def toBinary(mi: MemoryImmediate): Stream[Byte] =
    varuint32(if (mi.align == 0) 0 else log2(mi.align)).pack #::: // TODO really packing like this?
      varuint32(mi.offset.toInt).pack // TODO toInt

  private def toBinary(expr: Seq[B.Opcode]): Stream[Byte] = expr.flatMap(toBinary).toStream
  private def toBinary(opcode: B.Opcode): Stream[Byte] = {
    def typeOffset(t: AT): Int = t match {
      case AT.i32 => 0
      case AT.i64 => 1
      case AT.f32 => 2
      case AT.f64 => 3
    }

    def signOffset(s: A.Sign): Int = s match {
      case A.Sign.Signed => 0
      case A.Sign.Unsigned => 1
    }

    def opTypeOffset(t: AT, ids: Int*) = op(ids(typeOffset(t)))
    def opTypeOffsetFloat(t: AT, ids: Int*) = op(ids(typeOffset(t) - 2))
    def opTypeOffsetInt(t: AT, ids: Int*) = opTypeOffset(t, ids:_*)
    def opTypeAndSignOffsetInt(t: AT, s: A.Sign, ids: Int*) = op(ids(typeOffset(t)) + signOffset(s))
    def opTypeAndSignOffsetFloat(t: AT, s: A.Sign, ids: Int*) = op(ids(typeOffset(t) - 2) + signOffset(s))

    def op(id: Int) = uint8(id toByte).pack

    def countOfCmpOpForIntegralType: Int = 11
    def countOfCmpOpForFloatingType: Int = 6

    opcode match {
      case Unreachable => op(0x00)
      case Nop => op(0x01)
      case Block(t) => op(0x02) #::: Type.toBinary(t)
      case Loop(t) => op(0x03) #::: Type.toBinary(t)
      case If(t) => op(0x04) #::: Type.toBinary(t)
      case Else => op(0x05)
      case End => op(0x0b)
      case Br(i) => op(0x0c) #::: varuint32(i).pack
      case BrIf(i) => op(0x0d) #::: varuint32(i).pack
      case BrTable(targets, default) => op(0x0e) #:::
        varuint32(targets.size).pack #:::
        targets.flatMap(varint32(_).pack).toStream #:::
        varuint32(default).pack
      case Return => op(0x0f)

      case Call(i) => op(0x10) #::: varuint32(i).pack
      case CallIndirect(i) => op(0x11) #::: varuint32(i).pack #::: varuint1(0).pack

      case Drop => op(0x1a)
      case Select => op(0x1b)

      case GetLocal(i) => op(0x20) #::: varuint32(i).pack
      case SetLocal(i) => op(0x21) #::: varuint32(i).pack
      case TeeLocal(i) => op(0x22) #::: varuint32(i).pack
      case GetGlobal(i) => op(0x23) #::: varuint32(i).pack
      case SetGlobal(i) => op(0x24) #::: varuint32(i).pack

      case Load(t, None, mi) => op(0x28 + typeOffset(t)) #::: toBinary(mi)
      case Load(t, Some(SizeAndSign(size, sign)), mi) =>
        def opOffset(base: Int): Stream[Byte] = op((log2(size) - 3) * 2 + base + signOffset(sign))
        val computedOp = t match {
          case AT.i32 => opOffset(0x2c)
          case AT.i64 => opOffset(0x30)
        }
        computedOp #::: toBinary(mi)

      case Store(t, None, mi) => op(0x36 + typeOffset(t)) #::: toBinary(mi)
      case Store(t, Some(size), mi) =>
        def opOffset(base: Int): Stream[Byte] = op(base + log2(size) - 3)
        val computedOp = t match {
          case AT.i32 => opOffset(0x3a)
          case AT.i64 => opOffset(0x3c)
        }
        computedOp #::: toBinary(mi)

      case CurrentMemory => op(0x3f) #::: varuint1(0).pack
      case GrowMemory => op(0x40) #::: varuint1(0).pack

      case Const(AT.i32, A.Value.Integral(v)) => op(0x41) #::: varint32(v.toInt).pack
      case Const(AT.i64, A.Value.Integral(v)) => op(0x42) #::: varint64(v).pack
      case Const(AT.f32, A.Value.Floating(v)) => op(0x43) #::: uint32(java.lang.Float.floatToIntBits(v.toFloat)).pack
      case Const(AT.f64, A.Value.Floating(v)) => op(0x44) #::: uint64(java.lang.Double.doubleToLongBits(v)).pack

      case EqualZero(t) => op(0x45 + typeOffset(t) * countOfCmpOpForIntegralType)
      case Equal(t) => t match {
        case AT.i32 | AT.i64 => op(0x46 + typeOffset (t) * countOfCmpOpForIntegralType)
        case AT.f32 | AT.f64 => op(0x5b + (typeOffset(t) - 2) * countOfCmpOpForFloatingType)
      }
      case NotEqual(t) => t match {
        case AT.i32 | AT.i64 => op(0x47 + typeOffset(t) * countOfCmpOpForIntegralType)
        case AT.f32 | AT.f64 => op(0x5c + (typeOffset(t) - 2) * countOfCmpOpForFloatingType)
      }
      case LessThan(t, Some(s)) => op(0x48 + typeOffset(t) * countOfCmpOpForIntegralType + signOffset(s))
      case GreaterThan(t, Some(s)) => op(0x4a + typeOffset(t) * countOfCmpOpForIntegralType + signOffset(s))
      case LessOrEqual(t, Some(s)) => op(0x4c + typeOffset(t) * countOfCmpOpForIntegralType + signOffset(s))
      case GreaterOrEqual(t, Some(s)) => op(0x4e + typeOffset(t) * countOfCmpOpForIntegralType + signOffset(s))
      case LessThan(t, None) => op(0x5d + (typeOffset(t) - 2) * countOfCmpOpForFloatingType)
      case GreaterThan(t, None) => op(0x5e + (typeOffset(t) - 2) * countOfCmpOpForFloatingType)
      case LessOrEqual(t, None) => op(0x5f + (typeOffset(t) - 2) * countOfCmpOpForFloatingType)
      case GreaterOrEqual(t, None) => op(0x60 + (typeOffset(t) - 2) * countOfCmpOpForFloatingType)

      case CountLeadingZeros(t) => opTypeOffsetInt(t, 0x67, 0x79)
      case CountTrailingZeros(t) => opTypeOffsetInt(t, 0x68, 0x7a)
      case CountNumberOneBits(t) => opTypeOffsetInt(t, 0x69, 0x7b)
      case Add(t) => opTypeOffset(t, 0x6a, 0x7c, 0x92, 0xa0)
      case Substract(t) => opTypeOffset(t, 0x6b, 0x7d, 0x93, 0xa1)
      case Multiply(t) => opTypeOffset(t, 0x6c, 0x7e, 0x94, 0xa2)
      case Divide(t, Some(s)) => opTypeAndSignOffsetInt(t, s, 0x6d, 0x7f)
      case Remainder(t, s) => opTypeAndSignOffsetInt(t, s, 0x6f, 0x81)
      case And(t) => opTypeOffsetInt(t, 0x71, 0x83)
      case Or(t) => opTypeOffsetInt(t, 0x72, 0x84)
      case Xor(t) => opTypeOffsetInt(t, 0x73, 0x85)
      case ShiftLeft(t) => opTypeOffsetInt(t, 0x74, 0x86)
      case ShiftRight(t, s) => opTypeAndSignOffsetInt(t, s, 0x75, 0x87)
      case RotateLeft(t) => opTypeOffsetInt(t, 0x77, 0x89)
      case RotateRight(t) => opTypeOffsetInt(t, 0x78, 0x8a)

      //case Truncate(AT.i32, _, None) => op(0x8f) // TODO droping arg

      case Absolute(t) => opTypeOffsetFloat(t, 0x8b, 0x99)
      case Negative(t) => opTypeOffsetFloat(t, 0x8c, 0x9a)
      case Ceiling(t) => opTypeOffsetFloat(t, 0x8d, 0x9b)
      case Floor(t) => opTypeOffsetFloat(t, 0x8e, 0x9c)
      //case Truncate(AT.i32, _, None) => opTypeOffsetFloat(t, 0x9d) // TODO droping arg
      case Nearest(t) => opTypeOffsetFloat(t, 0x90, 0x9e)
      case Sqrt(t) => opTypeOffsetFloat(t, 0x91, 0x9f)
      case Divide(t, None) => opTypeOffsetFloat(t, 0x95, 0xa3)
      case Min(t) => opTypeOffsetFloat(t, 0x96, 0xa4)
      case Max(t) => opTypeOffsetFloat(t, 0x97, 0xa5)
      case CopySign(t) => opTypeOffsetFloat(t, 0x98, 0xa6)

      case Wrap(_, _) => op(0xa7) // TODO dropping arg
      case Truncate(AT.i32, t, s) => opTypeAndSignOffsetFloat(t, s, 0xa8, 0xaa)
      case Extend(AT.i64, AT.i32, s) => op(0xac + signOffset(s))
      case Truncate(AT.i64, t, s) => opTypeAndSignOffsetFloat(t, s, 0xae, 0xb0)
      case Convert(AT.f32, t, s) => opTypeAndSignOffsetInt(t, s, 0xb2, 0xb4)
      case Demote(AT.f32, AT.f64) => op(0xb6)
      case Convert(AT.f64, t, s) => opTypeAndSignOffsetInt(t, s, 0xb7, 0xb9)
      case Promote(AT.f64, AT.f32) => op(0xbb)

      case Reinterpret(AT.f64, AT.f32) => op(0xbb)
    }
  }

  private def toBinary(s: BSec): Stream[Byte] = {
    val (id: Option[Int], content: Seq[Any], payload: Stream[Byte]) = s match {
      case BSec.Type(types) =>
        (Some(1), types,
          varuint32(types.length).pack #:::
            types.flatMap { Signature.toBinary }.toStream)
      case BSec.Import(entries) =>
        (Some(2), entries,
          varuint32(entries.length).pack #:::
            entries.flatMap { i =>
              pack(i.module) #:::
                pack(i.field) #:::
                toBinaryTail(i) }.toStream)
      case BSec.Function(indexes) =>
        (Some(3), indexes, varuint32(indexes.length).pack #:::
          indexes.map(_.toByte).toStream)
      /*case AS.Table(entries) =>
        varuint32(entries.length).pack #:::
          (entries flatMap { B.Type.toBinary } toStream)*/
      case BSec.Memory(memories) =>
        (Some(5), memories, varuint32(memories.length).pack #:::
          memories.flatMap { m => Signature.toBinary(m) }.toStream)
      case BSec.Global(globals) =>
        (Some(6), globals, varuint32(globals.length).pack #:::
          globals.flatMap { case (g, expr) => Signature.toBinary(g) #::: toBinary(expr) }.toStream)
      case BSec.Export(exports) =>
        (Some(7), exports, varuint32(exports.length).pack #:::
          exports.flatMap { case (field, k, i) => pack(field) #::: toBinary(k) #::: varuint32(i).pack }.toStream)
      case BSec.Start(start) =>
        (Some(8), start.toList, if (start.isDefined) varuint32(start.get).pack else Stream.empty)
      /*case AS.Element(entries) =>
        varuint32(entries.size).pack #:::
          (entries flatMap { toBinary } toStream)*/
      case BSec.Code(codes) =>
        (Some(10), codes,
          varuint32(codes.size).pack #:::
            codes.flatMap { case (types, expr) =>
              val body = varuint32(types.size).pack #:::
                types.flatMap { case (count, t) =>
                  varuint32(count).pack #:::
                    Type.toBinary(t)
                }.toStream #::: toBinary(expr)

              (if (enableSpecCompat) varuint32CompatPack(body.size)
              else varuint32(body.size).pack) #:::
                body
          }.toStream)
      /*case AS.Data(entries) =>
        varuint32(entries.size).pack #:::
          (entries flatMap { toBinary } toStream)*/
    }

    // TODO quirk spec#625
    val size: Stream[Byte] =
      if (enableSpecCompat)
        varuint32CompatPack(payload.length)
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
