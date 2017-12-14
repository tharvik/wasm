package scalawasm.ast

trait Opcode
object Opcode {
  /*sealed trait ControlFlow extends Opcode
  sealed trait CallTrait extends Opcode // TODO ...Trait
  sealed trait Parametric extends Opcode
  sealed trait VariableTrait extends Opcode // TODO ...Trait
  sealed trait Memory extends Opcode
  sealed trait Constant extends Opcode
  sealed trait Comparison extends Opcode
  sealed trait Numeric extends Opcode
  sealed trait Conversion extends Opcode
  sealed trait Reinterpretation extends Opcode

  // TODO unable to have it private?
  sealed trait MemoryImmediate extends Memory

  final case object Unreachable extends ControlFlow
  final case object Nop extends ControlFlow
  final case class Block(sig: Type.Trait.Block) extends ControlFlow
  final case class Loop(sig: Type.Trait.Block) extends ControlFlow
  final case class If(sig: Type.Trait.Block) extends ControlFlow
  final case object Else extends ControlFlow
  final case object End extends ControlFlow
  final case class Br(label: Variable) extends ControlFlow
  final case class BrIf(label: Variable) extends ControlFlow
  final case class BrTable(labels: Seq[Variable], default: Variable) extends ControlFlow
  final case object Return extends ControlFlow

  // TODO auto type_index
  final case class Call(function: Variable) extends CallTrait
  final case class CallIndirect(type_index: Variable) extends CallTrait
  final case object Drop extends Parametric
  final case object Select extends Parametric
  final case class GetLocal(index: Variable) extends VariableTrait
  final case class SetLocal(index: Variable) extends VariableTrait
  final case class TeeLocal(index: Variable) extends VariableTrait
  final case class GetGlobal(index: Variable) extends VariableTrait
  final case class SetGlobal(index: Variable) extends VariableTrait

  // TODO better than `flags`
  final case class memory_immediate(flags: Int, offset: Int)

  object i32 {
    final case class Load(location: memory_immediate) extends MemoryImmediate
    final case class Load8Signed(location: memory_immediate) extends MemoryImmediate
    final case class Load8Unsigned(location: memory_immediate) extends MemoryImmediate
    final case class Load16Signed(location: memory_immediate) extends MemoryImmediate
    final case class Load16Unsigned(location: memory_immediate) extends MemoryImmediate
    final case class Store(location: memory_immediate) extends MemoryImmediate
    final case class Store8(location: memory_immediate) extends MemoryImmediate
    final case class Store16(location: memory_immediate) extends MemoryImmediate

    final case class Const(value: Int) extends Constant

    final case object EqualZero extends Comparison
    final case object Equal extends Comparison
    final case object NotEqual extends Comparison
    final case object LessThanSigned extends Comparison
    final case object LessThanUnsigned extends Comparison
    final case object GreaterThanSigned extends Comparison
    final case object GreaterThanUnsigned extends Comparison
    final case object LessOrEqualSigned extends Comparison
    final case object LessOrEqualUnsigned extends Comparison
    final case object GreaterOrEqualSigned extends Comparison
    final case object GreaterOrEqualUnsigned extends Comparison

    final case object CountLeadingZeros extends Comparison
    final case object CountTrailingZeros extends Comparison
    final case object CountNumberOneBits extends Comparison
    final case object Add extends Comparison
    final case object Subtracte extends Comparison
    final case object Multiply extends Comparison
    final case object DivideSigned extends Comparison
    final case object DivideUnsigned extends Comparison
    final case object RemainderSigned extends Comparison
    final case object RemainderUnsigned extends Comparison
    final case object And extends Comparison
    final case object Or extends Comparison
    final case object Xor extends Comparison
    final case object ShiftLeft extends Comparison
    final case object ShiftRightSigned extends Comparison
    final case object ShiftRightUnsigned extends Comparison
    final case object RotateLeft extends Comparison
    final case object RotateRight extends Comparison

    final case object WrapFromInt64 extends Comparison
    final case object TruncateSignedFromFloat32 extends Comparison
    final case object TruncateUnsignedFromFloat32 extends Comparison
    final case object TruncateSignedFromFloat64 extends Comparison
    final case object TruncateUnsignedFromFloat64 extends Comparison

    final case object ReinterpretFromFloat32 extends Comparison
  }

  object i64 {
    final case class Load(location: memory_immediate) extends MemoryImmediate
    final case class Load8Signed(location: memory_immediate) extends MemoryImmediate
    final case class Load8Unsigned(location: memory_immediate) extends MemoryImmediate
    final case class Load16Signed(location: memory_immediate) extends MemoryImmediate
    final case class Load16Unsigned(location: memory_immediate) extends MemoryImmediate
    final case class Load32Signed(location: memory_immediate) extends MemoryImmediate
    final case class Load32Unsigned(location: memory_immediate) extends MemoryImmediate
    final case class Store(location: memory_immediate) extends MemoryImmediate
    final case class Store8(location: memory_immediate) extends MemoryImmediate
    final case class Store16(location: memory_immediate) extends MemoryImmediate
    final case class Store32(location: memory_immediate) extends MemoryImmediate

    final case class Const(value: Long) extends Constant

    final case object EqualZero extends Comparison
    final case object Equal extends Comparison
    final case object NotEqual extends Comparison
    final case object LessThanSigned extends Comparison
    final case object LessThanUnsigned extends Comparison
    final case object GreaterThanSigned extends Comparison
    final case object GreaterThanUnsigned extends Comparison
    final case object LessOrEqualSigned extends Comparison
    final case object LessOrEqualUnsigned extends Comparison
    final case object GreaterOrEqualSigned extends Comparison
    final case object GreaterOrEqualUnsigned extends Comparison

    final case object CountLeadingZeros extends Comparison
    final case object CountTrailingZeros extends Comparison
    final case object CountNumberOneBits extends Comparison
    final case object Add extends Comparison
    final case object Subtracte extends Comparison
    final case object Multiply extends Comparison
    final case object DivideSigned extends Comparison
    final case object DivideUnsigned extends Comparison
    final case object RemainderSigned extends Comparison
    final case object RemainderUnsigned extends Comparison
    final case object And extends Comparison
    final case object Or extends Comparison
    final case object Xor extends Comparison
    final case object ShiftLeft extends Comparison
    final case object ShiftRightSigned extends Comparison
    final case object ShiftRightUnsigned extends Comparison
    final case object RotateLeft extends Comparison
    final case object RotateRight extends Comparison

    final case object ExtendSignedFromInt32 extends Comparison
    final case object ExtendUnsignedFromInt32 extends Comparison
    final case object TruncateSignedFromFloat32 extends Comparison
    final case object TruncateUnsignedFromFloat32 extends Comparison
    final case object TruncateSignedFromFloat64 extends Comparison
    final case object TruncateUnsignedFromFloat64 extends Comparison

    final case object ReinterpretFromFloat64 extends Comparison
  }

  object f32 {
    final case class Load(location: memory_immediate) extends MemoryImmediate
    final case class Store(location: memory_immediate) extends MemoryImmediate

    final case class Const(value: Float) extends Constant

    final case object Equal extends Comparison
    final case object NotEqual extends Comparison
    final case object LessThan extends Comparison
    final case object GreaterThan extends Comparison
    final case object LessOrEqual extends Comparison
    final case object GreaterOrEqual extends Comparison

    final case object Absolute extends Numeric
    final case object Negative extends Numeric
    final case object Ceiling extends Numeric
    final case object Floor extends Numeric
    final case object Truncate extends Numeric
    final case object Nearest extends Numeric
    final case object Sqrt extends Numeric
    final case object Add extends Numeric
    final case object Substract extends Numeric
    final case object Multiply extends Numeric
    final case object Divide extends Numeric
    final case object Min extends Numeric
    final case object Max extends Numeric
    final case object CopySign extends Numeric

    final case object ConvertSignedFromInt32 extends Conversion
    final case object ConvertUnsignedFromInt32 extends Conversion
    final case object ConvertSignedFromInt64 extends Conversion
    final case object ConvertUnsignedFromInt64 extends Conversion
    final case object DemoteFromFloat64 extends Conversion

    final case object ReinterpretFromInt32 extends Reinterpretation
  }

  object f64 {
    final case class Load(location: memory_immediate) extends MemoryImmediate
    final case class Store(location: memory_immediate) extends MemoryImmediate

    final case class Const(value: Double) extends Constant

    final case object Equal extends Comparison
    final case object NotEqual extends Comparison
    final case object LessThan extends Comparison
    final case object GreaterThan extends Comparison
    final case object LessOrEqual extends Comparison
    final case object GreaterOrEqual extends Comparison

    final case object Absolute extends Numeric
    final case object Negative extends Numeric
    final case object Ceiling extends Numeric
    final case object Floor extends Numeric
    final case object Truncate extends Numeric
    final case object Nearest extends Numeric
    final case object Sqrt extends Numeric
    final case object Add extends Numeric
    final case object Substract extends Numeric
    final case object Multiply extends Numeric
    final case object Divide extends Numeric
    final case object Min extends Numeric
    final case object Max extends Numeric
    final case object CopySign extends Numeric

    final case object ConvertSignedFromInt32 extends Conversion
    final case object ConvertUnsignedFromInt32 extends Conversion
    final case object ConvertSignedFromInt64 extends Conversion
    final case object ConvertUnsignedFromInt64 extends Conversion
    final case object PromoteFromFloat32 extends Conversion

    final case object ReinterpretFromInt64 extends Reinterpretation
  }

  final case object CurrentMemory extends Memory
  final case object GrowMemory extends Memory*/
}
