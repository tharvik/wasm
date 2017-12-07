package scalawasm.binary

import scalawasm.ast
import scalawasm.binary.LEB128.Type._

object Opcode {
  import scalawasm.ast.Opcode.Trait._

  def toBinary(o: ast.Opcode): Stream[Byte] = o match {
    case v: ControlFlow => toBinary(v)
    case v: Variable => toBinary(v)
  }

  def toBinary(o: ControlFlow): Stream[Byte] = o match {
      case Unreachable => Stream(0x00)
      case Nop => Stream(0x01)
      case Block(sig) => Stream(0x02 toByte) #::: Type.Dispatcher.toBinary(sig)
      case Loop(sig) => Stream(0x03 toByte) #::: Type.Dispatcher.toBinary(sig)
      case If(sig) => Stream(0x04 toByte) #::: Type.Dispatcher.toBinary(sig)
      case Else => Stream(0x05)
      case End => Stream(0x0b toByte)
      // TODO case Br(relative_depth: Int) => Stream(0x0c toByte) #::: varuint32(relative_depth).pack
      // TODO case BrIf(relative_depth: Int) => Stream(0x0d toByte) #::: varuint32(relative_depth).pack
      /* TODO case BrTable(targets: Seq[Int], default: Int) =>
        Stream(0x0e toByte) #:::
          varuint32(targets.size).pack #:::
          targets.flatMap {varuint32(_).pack}.toStream #:::
          varuint32(default).pack*/
      case Return => Stream(0x0f toByte)
    }
  /*
  sealed trait Call extends Opcode
  sealed trait Parametric extends Opcode
  sealed trait Variable extends Opcode {
  sealed trait Memory extends Opcode
  sealed trait Constant extends Opcode
  sealed trait Comparison extends Opcode
  sealed trait Numeric extends Opcode
  sealed trait Conversion extends Opcode
  sealed trait Reinterpretation extends Opcode
 */

  def toBinary(v: Variable): Stream[Byte] = {
    val (op, index) = v match {
      case GetLocal(i: Int) => (0x20, i)
      case SetLocal(i: Int) => (0x21, i)
      case TeeLocal(i: Int) => (0x22, i)
      case GetGlobal(i: Int) => (0x23, i)
      case SetGlobal(i: Int) => (0x24, i)
    }
    Stream(op toByte) #::: varuint32(index).pack
  }
}



  /*

// TODO auto function_index
final case class Call(function_index: Int) extends Opcode.Call {
  def opcode = 0x10
  override def toBinary = super.toBinary #::: varuint32(function_index).pack
}
// TODO auto type_index
final case class CallIndirect(type_index: Int) extends Opcode.Call {
  def opcode = 0x11
  override def toBinary =
    super.toBinary #:::
      varuint32(type_index).pack #:::
      varuint1(0).pack
}

final case object Drop extends Parametric {
  def opcode = 0x1a toByte
}
final case object Select extends Parametric {
  def opcode = 0x1b toByte
}


// TODO better than `flags`
final case class memory_immediate(flags: Int, offset: Int) extends BinaryEncoding {
  override def toBinary = varuint32(flags).pack #::: varuint32(offset).pack
}

// TODO unable to have it private?
sealed trait MemoryImmediate extends Memory {
  def location: memory_immediate
  override def toBinary = super.toBinary #::: location.toBinary
}

object i32 {
  final case class Load(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x28
  }
  final case class Load8Signed(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x2c toByte
  }
  final case class Load8Unsigned(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x2d toByte
  }
  final case class Load16Signed(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x2e toByte
  }
  final case class Load16Unsigned(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x2f toByte
  }
  final case class Store(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x36
  }
  final case class Store8(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x3a toByte
  }
  final case class Store16(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x3b toByte
  }

  final case class Const(value: Int) extends Constant {
    def opcode = 0x41 toByte
    override def toBinary = super.toBinary #::: varint32(value).pack
  }

  final case object EqualZero extends Comparison {
    def opcode = 0x45
  }
  final case object Equal extends Comparison {
    def opcode = 0x46
  }
  final case object NotEqual extends Comparison {
    def opcode = 0x47
  }
  final case object LessThanSigned extends Comparison {
    def opcode = 0x48
  }
  final case object LessThanUnsigned extends Comparison {
    def opcode = 0x49
  }
  final case object GreaterThanSigned extends Comparison {
    def opcode = 0x4a toByte
  }
  final case object GreaterThanUnsigned extends Comparison {
    def opcode = 0x4b toByte
  }
  final case object LessOrEqualSigned extends Comparison {
    def opcode = 0x4c toByte
  }
  final case object LessOrEqualUnsigned extends Comparison {
    def opcode = 0x4d toByte
  }
  final case object GreaterOrEqualSigned extends Comparison {
    def opcode = 0x4e toByte
  }
  final case object GreaterOrEqualUnsigned extends Comparison {
    def opcode = 0x4f toByte
  }

  final case object CountLeadingZeros extends Comparison {
    def opcode = 0x67
  }
  final case object CountTrailingZeros extends Comparison {
    def opcode = 0x68
  }
  final case object CountNumberOneBits extends Comparison {
    def opcode = 0x69
  }
  final case object Add extends Comparison {
    def opcode = 0x6a toByte
  }
  final case object Subtracte extends Comparison {
    def opcode = 0x6b toByte
  }
  final case object Multiply extends Comparison {
    def opcode = 0x6c toByte
  }
  final case object DivideSigned extends Comparison {
    def opcode = 0x6d toByte
  }
  final case object DivideUnsigned extends Comparison {
    def opcode = 0x6e toByte
  }
  final case object RemainderSigned extends Comparison {
    def opcode = 0x6f toByte
  }
  final case object RemainderUnsigned extends Comparison {
    def opcode = 0x70
  }
  final case object And extends Comparison {
    def opcode = 0x71
  }
  final case object Or extends Comparison {
    def opcode = 0x72
  }
  final case object Xor extends Comparison {
    def opcode = 0x73
  }
  final case object ShiftLeft extends Comparison {
    def opcode = 0x74
  }
  final case object ShiftRightSigned extends Comparison {
    def opcode = 0x75
  }
  final case object ShiftRightUnsigned extends Comparison {
    def opcode = 0x76
  }
  final case object RotateLeft extends Comparison {
    def opcode = 0x77
  }
  final case object RotateRight extends Comparison {
    def opcode = 0x78
  }

  final case object WrapFromInt64 extends Comparison {
    def opcode = 0xa7 toByte
  }
  final case object TruncateSignedFromFloat32 extends Comparison {
    def opcode = 0xa8 toByte
  }
  final case object TruncateUnsignedFromFloat32 extends Comparison {
    def opcode = 0xa9 toByte
  }
  final case object TruncateSignedFromFloat64 extends Comparison {
    def opcode = 0xaa toByte
  }
  final case object TruncateUnsignedFromFloat64 extends Comparison {
    def opcode = 0xab toByte
  }

  final case object ReinterpretFromFloat32 extends Comparison {
    def opcode = 0xab toByte
  }
}

object i64 {
  final case class Load(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x29
  }
  final case class Load8Signed(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x30
  }
  final case class Load8Unsigned(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x31
  }
  final case class Load16Signed(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x32
  }
  final case class Load16Unsigned(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x33
  }
  final case class Load32Signed(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x34
  }
  final case class Load32Unsigned(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x35
  }
  final case class Store(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x37
  }
  final case class Store8(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x3c toByte
  }
  final case class Store16(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x3d toByte
  }
  final case class Store32(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x3e toByte
  }

  final case class Const(value: Long) extends Constant {
    def opcode = 0x42 toByte
    override def toBinary = super.toBinary #::: varint64(value).pack
  }

  final case object EqualZero extends Comparison {
    def opcode = 0x50
  }
  final case object Equal extends Comparison {
    def opcode = 0x51
  }
  final case object NotEqual extends Comparison {
    def opcode = 0x52
  }
  final case object LessThanSigned extends Comparison {
    def opcode = 0x53
  }
  final case object LessThanUnsigned extends Comparison {
    def opcode = 0x54
  }
  final case object GreaterThanSigned extends Comparison {
    def opcode = 0x55
  }
  final case object GreaterThanUnsigned extends Comparison {
    def opcode = 0x56
  }
  final case object LessOrEqualSigned extends Comparison {
    def opcode = 0x57
  }
  final case object LessOrEqualUnsigned extends Comparison {
    def opcode = 0x58
  }
  final case object GreaterOrEqualSigned extends Comparison {
    def opcode = 0x59
  }
  final case object GreaterOrEqualUnsigned extends Comparison {
    def opcode = 0x5a toByte
  }

  final case object CountLeadingZeros extends Comparison {
    def opcode = 0x79
  }
  final case object CountTrailingZeros extends Comparison {
    def opcode = 0x7a toByte
  }
  final case object CountNumberOneBits extends Comparison {
    def opcode = 0x7b toByte
  }
  final case object Add extends Comparison {
    def opcode = 0x7c toByte
  }
  final case object Subtracte extends Comparison {
    def opcode = 0x7d toByte
  }
  final case object Multiply extends Comparison {
    def opcode = 0x7e toByte
  }
  final case object DivideSigned extends Comparison {
    def opcode = 0x7f toByte
  }
  final case object DivideUnsigned extends Comparison {
    def opcode = 0x80 toByte
  }
  final case object RemainderSigned extends Comparison {
    def opcode = 0x81 toByte
  }
  final case object RemainderUnsigned extends Comparison {
    def opcode = 0x82 toByte
  }
  final case object And extends Comparison {
    def opcode = 0x83 toByte
  }
  final case object Or extends Comparison {
    def opcode = 0x84 toByte
  }
  final case object Xor extends Comparison {
    def opcode = 0x85 toByte
  }
  final case object ShiftLeft extends Comparison {
    def opcode = 0x86 toByte
  }
  final case object ShiftRightSigned extends Comparison {
    def opcode = 0x87 toByte
  }
  final case object ShiftRightUnsigned extends Comparison {
    def opcode = 0x88 toByte
  }
  final case object RotateLeft extends Comparison {
    def opcode = 0x89 toByte
  }
  final case object RotateRight extends Comparison {
    def opcode = 0x8a toByte
  }

  final case object ExtendSignedFromInt32 extends Comparison {
    def opcode = 0xac toByte
  }
  final case object ExtendUnsignedFromInt32 extends Comparison {
    def opcode = 0xad toByte
  }
  final case object TruncateSignedFromFloat32 extends Comparison {
    def opcode = 0xae toByte
  }
  final case object TruncateUnsignedFromFloat32 extends Comparison {
    def opcode = 0xaf toByte
  }
  final case object TruncateSignedFromFloat64 extends Comparison {
    def opcode = 0xb0 toByte
  }
  final case object TruncateUnsignedFromFloat64 extends Comparison {
    def opcode = 0xb1 toByte
  }

  final case object Reinterpret extends Comparison {
    def opcode = 0xbd toByte
  }
}

object f32 {
  final case class Load(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x2a toByte
  }
  final case class Store(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x38
  }

  final case class Const(value: Float) extends Constant {
    import java.lang.Float
    def opcode = 0x43 toByte
    override def toBinary = super.toBinary #::: uint32(Float.floatToIntBits(value)).pack
  }

  final case object Equal extends Comparison {
    def opcode = 0x5b toByte
  }
  final case object NotEqual extends Comparison {
    def opcode = 0x5c toByte
  }
  final case object LessThan extends Comparison {
    def opcode = 0x5d toByte
  }
  final case object GreaterThan extends Comparison {
    def opcode = 0x5e toByte
  }
  final case object LessOrEqual extends Comparison {
    def opcode = 0x5f toByte
  }
  final case object GreaterOrEqual extends Comparison {
    def opcode = 0x60
  }

  final case object Absolute extends Numeric {
    def opcode = 0x8b toByte
  }
  final case object Negative extends Numeric {
    def opcode = 0x8c toByte
  }
  final case object Ceiling extends Numeric {
    def opcode = 0x8d toByte
  }
  final case object Floor extends Numeric {
    def opcode = 0x8e toByte
  }
  final case object Truncate extends Numeric {
    def opcode = 0x8f toByte
  }
  final case object Nearest extends Numeric {
    def opcode = 0x90 toByte
  }
  final case object Sqrt extends Numeric {
    def opcode = 0x91 toByte
  }
  final case object Add extends Numeric {
    def opcode = 0x92 toByte
  }
  final case object Substract extends Numeric {
    def opcode = 0x93 toByte
  }
  final case object Multiply extends Numeric {
    def opcode = 0x94 toByte
  }
  final case object Divide extends Numeric {
    def opcode = 0x95 toByte
  }
  final case object Min extends Numeric {
    def opcode = 0x96 toByte
  }
  final case object Max extends Numeric {
    def opcode = 0x97 toByte
  }
  final case object CopySign extends Numeric {
    def opcode = 0x98 toByte
  }

  final case object ConvertSignedFromInt32 extends Conversion {
    def opcode = 0xb2 toByte
  }
  final case object ConvertUnsignedFromInt32 extends Conversion {
    def opcode = 0xb3 toByte
  }
  final case object ConvertSignedFromInt64 extends Conversion {
    def opcode = 0xb4 toByte
  }
  final case object ConvertUnsignedFromInt64 extends Conversion {
    def opcode = 0xb5 toByte
  }
  final case object DemoteFromFloat64 extends Conversion {
    def opcode = 0xb6 toByte
  }

  final case object ReinterpretFromInt32 extends Reinterpretation {
    def opcode = 0xbe toByte
  }
}

object f64 {
  final case class Load(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x2b toByte
  }
  final case class Store(location: memory_immediate) extends MemoryImmediate {
    def opcode = 0x39
  }

  final case class Const(value: Double) extends Constant {
    def opcode = 0x44 toByte
    override def toBinary = {
      import java.lang.Double
      super.toBinary #:::
        uint64(Double.doubleToLongBits(value)).pack
    }
  }

  final case object Equal extends Comparison {
    def opcode = 0x61 toByte
  }
  final case object NotEqual extends Comparison {
    def opcode = 0x62 toByte
  }
  final case object LessThan extends Comparison {
    def opcode = 0x63 toByte
  }
  final case object GreaterThan extends Comparison {
    def opcode = 0x64 toByte
  }
  final case object LessOrEqual extends Comparison {
    def opcode = 0x65 toByte
  }
  final case object GreaterOrEqual extends Comparison {
    def opcode = 0x66
  }

  final case object Absolute extends Numeric {
    def opcode = 0x99 toByte
  }
  final case object Negative extends Numeric {
    def opcode = 0x9a toByte
  }
  final case object Ceiling extends Numeric {
    def opcode = 0x9b toByte
  }
  final case object Floor extends Numeric {
    def opcode = 0x9c toByte
  }
  final case object Truncate extends Numeric {
    def opcode = 0x9d toByte
  }
  final case object Nearest extends Numeric {
    def opcode = 0x9e toByte
  }
  final case object Sqrt extends Numeric {
    def opcode = 0x9f toByte
  }
  final case object Add extends Numeric {
    def opcode = 0xa0 toByte
  }
  final case object Substract extends Numeric {
    def opcode = 0xa1 toByte
  }
  final case object Multiply extends Numeric {
    def opcode = 0xa2 toByte
  }
  final case object Divide extends Numeric {
    def opcode = 0xa3 toByte
  }
  final case object Min extends Numeric {
    def opcode = 0xa4 toByte
  }
  final case object Max extends Numeric {
    def opcode = 0xa5 toByte
  }
  final case object CopySign extends Numeric {
    def opcode = 0xa6 toByte
  }

  final case object ConvertSignedFromInt32 extends Conversion {
    def opcode = 0xb7 toByte
  }
  final case object ConvertUnsignedFromInt32 extends Conversion {
    def opcode = 0xb8 toByte
  }
  final case object ConvertSignedFromInt64 extends Conversion {
    def opcode = 0xb9 toByte
  }
  final case object ConvertUnsignedFromInt64 extends Conversion {
    def opcode = 0xba toByte
  }
  final case object PromoteFromFloat32 extends Conversion {
    def opcode = 0xbb toByte
  }

  final case object ReinterpretFromInt64 extends Reinterpretation {
    def opcode = 0xbf toByte
  }
}

final case object CurrentMemory extends Memory {
  def opcode = 0x3f toByte
  override def toBinary = super.toBinary #::: varuint1(0).pack
}

final case object GrowMemory extends Memory {
  def opcode = 0x40
  override def toBinary = super.toBinary #::: varuint1(0).pack
}
*/
