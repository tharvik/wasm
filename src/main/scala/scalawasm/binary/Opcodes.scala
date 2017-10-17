package scalawasm.binary

import scalawasm.BinaryEncoding
import scalawasm.binary.Types._
import scalawasm.binary.Opcode._

sealed trait Opcode extends BinaryEncoding {
  def opcode: Byte
  def toBinary = Stream(opcode)
}
object Opcode {
  sealed trait ControlFlow extends Opcode
  sealed trait Call extends Opcode
  sealed trait Parametric extends Opcode
  // TODO cross check index
  sealed trait Variable extends Opcode {
    def index: Int
    override def toBinary = super.toBinary #::: varuint32(index).pack
  }
  sealed trait Memory extends Opcode
  sealed trait Constant extends Opcode
  sealed trait Comparison extends Opcode
  sealed trait Numeric extends Opcode
  sealed trait Conversion extends Opcode
  sealed trait Reinterpretation extends Opcode
}

final case class Unreachable() extends ControlFlow {
  def opcode = 0x00
}
final case class Nop() extends ControlFlow {
  def opcode = 0x01
}
final case class Block(sig: block_type) extends ControlFlow {
  def opcode = 0x02
  override def toBinary = super.toBinary #::: sig.toBinary
}
final case class Loop(sig: block_type) extends ControlFlow {
  def opcode = 0x03
  override def toBinary = super.toBinary #::: sig.toBinary
}
final case class If(sig: block_type) extends ControlFlow {
  def opcode = 0x04
  override def toBinary = super.toBinary #::: sig.toBinary
}
final case class Else() extends ControlFlow {
  def opcode = 0x05
}
final case class End() extends ControlFlow {
  def opcode = 0x0b toByte
}
final case class Br(relative_depth: Int) extends ControlFlow {
  def opcode = 0x0c toByte
  override def toBinary = super.toBinary #::: varuint32(relative_depth).pack
}
final case class BrIf(relative_depth: Int) extends ControlFlow {
  def opcode = 0x0d toByte
  override def toBinary = super.toBinary #::: varuint32(relative_depth).pack
}
final case class BrTable(targets: Seq[Int], default: Int) extends ControlFlow {
  def opcode = 0x0e toByte
  override def toBinary =
    super.toBinary #:::
      varuint32(targets.size).pack #:::
      targets.flatMap {varuint32(_).pack}.toStream #:::
      varuint32(default).pack
}
final case class Return() extends ControlFlow {
  def opcode = 0x0f toByte
}

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

final case class Drop() extends Parametric {
  def opcode = 0x1a toByte
}
final case class Select() extends Parametric {
  def opcode = 0x1b toByte
}

final case class GetLocal(index: Int) extends Variable {
  def opcode = 0x20 toByte
}
final case class SetLocal(index: Int) extends Variable {
  def opcode = 0x21 toByte
}
final case class TeeLocal(index: Int) extends Variable {
  def opcode = 0x22 toByte
}
final case class GetGlobal(index: Int) extends Variable {
  def opcode = 0x23 toByte
}
final case class SetGlobal(index: Int) extends Variable {
  def opcode = 0x24 toByte
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

  final case class EqualZero() extends Comparison {
    def opcode = 0x45
  }
  final case class Equal() extends Comparison {
    def opcode = 0x46
  }
  final case class NotEqual() extends Comparison {
    def opcode = 0x47
  }
  final case class LessThanSigned() extends Comparison {
    def opcode = 0x48
  }
  final case class LessThanUnsigned() extends Comparison {
    def opcode = 0x49
  }
  final case class GreaterThanSigned() extends Comparison {
    def opcode = 0x4a toByte
  }
  final case class GreaterThanUnsigned() extends Comparison {
    def opcode = 0x4b toByte
  }
  final case class LessOrEqualSigned() extends Comparison {
    def opcode = 0x4c toByte
  }
  final case class LessOrEqualUnsigned() extends Comparison {
    def opcode = 0x4d toByte
  }
  final case class GreaterOrEqualSigned() extends Comparison {
    def opcode = 0x4e toByte
  }
  final case class GreaterOrEqualUnsigned() extends Comparison {
    def opcode = 0x4f toByte
  }

  final case class CountLeadingZeros() extends Comparison {
    def opcode = 0x67
  }
  final case class CountTrailingZeros() extends Comparison {
    def opcode = 0x68
  }
  final case class CountNumberOneBits() extends Comparison {
    def opcode = 0x69
  }
  final case class Add() extends Comparison {
    def opcode = 0x6a toByte
  }
  final case class Subtracte() extends Comparison {
    def opcode = 0x6b toByte
  }
  final case class Multiply() extends Comparison {
    def opcode = 0x6c toByte
  }
  final case class DivideSigned() extends Comparison {
    def opcode = 0x6d toByte
  }
  final case class DivideUnsigned() extends Comparison {
    def opcode = 0x6e toByte
  }
  final case class RemainderSigned() extends Comparison {
    def opcode = 0x6f toByte
  }
  final case class RemainderUnsigned() extends Comparison {
    def opcode = 0x70
  }
  final case class And() extends Comparison {
    def opcode = 0x71
  }
  final case class Or() extends Comparison {
    def opcode = 0x72
  }
  final case class Xor() extends Comparison {
    def opcode = 0x73
  }
  final case class ShiftLeft() extends Comparison {
    def opcode = 0x74
  }
  final case class ShiftRightSigned() extends Comparison {
    def opcode = 0x75
  }
  final case class ShiftRightUnsigned() extends Comparison {
    def opcode = 0x76
  }
  final case class RotateLeft() extends Comparison {
    def opcode = 0x77
  }
  final case class RotateRight() extends Comparison {
    def opcode = 0x78
  }

  final case class WrapFromInt64() extends Comparison {
    def opcode = 0xa7 toByte
  }
  final case class TruncateSignedFromFloat32() extends Comparison {
    def opcode = 0xa8 toByte
  }
  final case class TruncateUnsignedFromFloat32() extends Comparison {
    def opcode = 0xa9 toByte
  }
  final case class TruncateSignedFromFloat64() extends Comparison {
    def opcode = 0xaa toByte
  }
  final case class TruncateUnsignedFromFloat64() extends Comparison {
    def opcode = 0xab toByte
  }

  final case class ReinterpretFromFloat32() extends Comparison {
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

  final case class EqualZero() extends Comparison {
    def opcode = 0x50
  }
  final case class Equal() extends Comparison {
    def opcode = 0x51
  }
  final case class NotEqual() extends Comparison {
    def opcode = 0x52
  }
  final case class LessThanSigned() extends Comparison {
    def opcode = 0x53
  }
  final case class LessThanUnsigned() extends Comparison {
    def opcode = 0x54
  }
  final case class GreaterThanSigned() extends Comparison {
    def opcode = 0x55
  }
  final case class GreaterThanUnsigned() extends Comparison {
    def opcode = 0x56
  }
  final case class LessOrEqualSigned() extends Comparison {
    def opcode = 0x57
  }
  final case class LessOrEqualUnsigned() extends Comparison {
    def opcode = 0x58
  }
  final case class GreaterOrEqualSigned() extends Comparison {
    def opcode = 0x59
  }
  final case class GreaterOrEqualUnsigned() extends Comparison {
    def opcode = 0x5a toByte
  }

  final case class CountLeadingZeros() extends Comparison {
    def opcode = 0x79
  }
  final case class CountTrailingZeros() extends Comparison {
    def opcode = 0x7a toByte
  }
  final case class CountNumberOneBits() extends Comparison {
    def opcode = 0x7b toByte
  }
  final case class Add() extends Comparison {
    def opcode = 0x7c toByte
  }
  final case class Subtracte() extends Comparison {
    def opcode = 0x7d toByte
  }
  final case class Multiply() extends Comparison {
    def opcode = 0x7e toByte
  }
  final case class DivideSigned() extends Comparison {
    def opcode = 0x7f toByte
  }
  final case class DivideUnsigned() extends Comparison {
    def opcode = 0x80 toByte
  }
  final case class RemainderSigned() extends Comparison {
    def opcode = 0x81 toByte
  }
  final case class RemainderUnsigned() extends Comparison {
    def opcode = 0x82 toByte
  }
  final case class And() extends Comparison {
    def opcode = 0x83 toByte
  }
  final case class Or() extends Comparison {
    def opcode = 0x84 toByte
  }
  final case class Xor() extends Comparison {
    def opcode = 0x85 toByte
  }
  final case class ShiftLeft() extends Comparison {
    def opcode = 0x86 toByte
  }
  final case class ShiftRightSigned() extends Comparison {
    def opcode = 0x87 toByte
  }
  final case class ShiftRightUnsigned() extends Comparison {
    def opcode = 0x88 toByte
  }
  final case class RotateLeft() extends Comparison {
    def opcode = 0x89 toByte
  }
  final case class RotateRight() extends Comparison {
    def opcode = 0x8a toByte
  }

  final case class ExtendSignedFromInt32() extends Comparison {
    def opcode = 0xac toByte
  }
  final case class ExtendUnsignedFromInt32() extends Comparison {
    def opcode = 0xad toByte
  }
  final case class TruncateSignedFromFloat32() extends Comparison {
    def opcode = 0xae toByte
  }
  final case class TruncateUnsignedFromFloat32() extends Comparison {
    def opcode = 0xaf toByte
  }
  final case class TruncateSignedFromFloat64() extends Comparison {
    def opcode = 0xb0 toByte
  }
  final case class TruncateUnsignedFromFloat64() extends Comparison {
    def opcode = 0xb1 toByte
  }

  final case class Reinterpret() extends Comparison {
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

  final case class Equal() extends Comparison {
    def opcode = 0x5b toByte
  }
  final case class NotEqual() extends Comparison {
    def opcode = 0x5c toByte
  }
  final case class LessThan() extends Comparison {
    def opcode = 0x5d toByte
  }
  final case class GreaterThan() extends Comparison {
    def opcode = 0x5e toByte
  }
  final case class LessOrEqual() extends Comparison {
    def opcode = 0x5f toByte
  }
  final case class GreaterOrEqual() extends Comparison {
    def opcode = 0x60
  }

  final case class Absolute() extends Numeric {
    def opcode = 0x8b toByte
  }
  final case class Negative() extends Numeric {
    def opcode = 0x8c toByte
  }
  final case class Ceiling() extends Numeric {
    def opcode = 0x8d toByte
  }
  final case class Floor() extends Numeric {
    def opcode = 0x8e toByte
  }
  final case class Truncate() extends Numeric {
    def opcode = 0x8f toByte
  }
  final case class Nearest() extends Numeric {
    def opcode = 0x90 toByte
  }
  final case class Sqrt() extends Numeric {
    def opcode = 0x91 toByte
  }
  final case class Add() extends Numeric {
    def opcode = 0x92 toByte
  }
  final case class Substract() extends Numeric {
    def opcode = 0x93 toByte
  }
  final case class Multiply() extends Numeric {
    def opcode = 0x94 toByte
  }
  final case class Divide() extends Numeric {
    def opcode = 0x95 toByte
  }
  final case class Min() extends Numeric {
    def opcode = 0x96 toByte
  }
  final case class Max() extends Numeric {
    def opcode = 0x97 toByte
  }
  final case class CopySign() extends Numeric {
    def opcode = 0x98 toByte
  }

  final case class ConvertSignedFromInt32() extends Conversion {
    def opcode = 0xb2 toByte
  }
  final case class ConvertUnsignedFromInt32() extends Conversion {
    def opcode = 0xb3 toByte
  }
  final case class ConvertSignedFromInt64() extends Conversion {
    def opcode = 0xb4 toByte
  }
  final case class ConvertUnsignedFromInt64() extends Conversion {
    def opcode = 0xb5 toByte
  }
  final case class DemoteFromFloat64() extends Conversion {
    def opcode = 0xb6 toByte
  }

  final case class ReinterpretFromInt32() extends Reinterpretation {
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

  final case class Equal() extends Comparison {
    def opcode = 0x61 toByte
  }
  final case class NotEqual() extends Comparison {
    def opcode = 0x62 toByte
  }
  final case class LessThan() extends Comparison {
    def opcode = 0x63 toByte
  }
  final case class GreaterThan() extends Comparison {
    def opcode = 0x64 toByte
  }
  final case class LessOrEqual() extends Comparison {
    def opcode = 0x65 toByte
  }
  final case class GreaterOrEqual() extends Comparison {
    def opcode = 0x66
  }

  final case class Absolute() extends Numeric {
    def opcode = 0x99 toByte
  }
  final case class Negative() extends Numeric {
    def opcode = 0x9a toByte
  }
  final case class Ceiling() extends Numeric {
    def opcode = 0x9b toByte
  }
  final case class Floor() extends Numeric {
    def opcode = 0x9c toByte
  }
  final case class Truncate() extends Numeric {
    def opcode = 0x9d toByte
  }
  final case class Nearest() extends Numeric {
    def opcode = 0x9e toByte
  }
  final case class Sqrt() extends Numeric {
    def opcode = 0x9f toByte
  }
  final case class Add() extends Numeric {
    def opcode = 0xa0 toByte
  }
  final case class Substract() extends Numeric {
    def opcode = 0xa1 toByte
  }
  final case class Multiply() extends Numeric {
    def opcode = 0xa2 toByte
  }
  final case class Divide() extends Numeric {
    def opcode = 0xa3 toByte
  }
  final case class Min() extends Numeric {
    def opcode = 0xa4 toByte
  }
  final case class Max() extends Numeric {
    def opcode = 0xa5 toByte
  }
  final case class CopySign() extends Numeric {
    def opcode = 0xa6 toByte
  }

  final case class ConvertSignedFromInt32() extends Conversion {
    def opcode = 0xb7 toByte
  }
  final case class ConvertUnsignedFromInt32() extends Conversion {
    def opcode = 0xb8 toByte
  }
  final case class ConvertSignedFromInt64() extends Conversion {
    def opcode = 0xb9 toByte
  }
  final case class ConvertUnsignedFromInt64() extends Conversion {
    def opcode = 0xba toByte
  }
  final case class PromoteFromFloat32() extends Conversion {
    def opcode = 0xbb toByte
  }

  final case class ReinterpretFromInt64() extends Reinterpretation {
    def opcode = 0xbf toByte
  }
}

final case class CurrentMemory() extends Memory {
  def opcode = 0x3f toByte
  override def toBinary = super.toBinary #::: varuint1(0).pack
}

final case class GrowMemory() extends Memory {
  def opcode = 0x40
  override def toBinary = super.toBinary #::: varuint1(0).pack
}
