package scalawasm.ast

import scalawasm.ast.{Type => AT}

object Binary {
  final case class Preamble(sections: Seq[Section])

  sealed trait Opcode
  object Opcode {
    final case object Unreachable extends Opcode
    final case object Nop extends Opcode
    final case class Block(sig: AT.Block) extends Opcode
    final case class Loop(sig: AT.Block) extends Opcode
    final case class If(sig: AT.Block) extends Opcode
    final case object Else extends Opcode
    final case object End extends Opcode
    final case class Br(depth: Int) extends Opcode
    final case class BrIf(depth: Int) extends Opcode
    final case class BrTable(targets: Seq[Int], default: Int) extends Opcode
    final case object Return extends Opcode

    final case class Call(funcIndex: Int) extends Opcode
    final case class CallIndirect(typeIndex: Int) extends Opcode

    final case object Drop extends Opcode
    final case object Select extends Opcode

    final case class GetLocal(index: Int) extends Opcode
    final case class SetLocal(index: Int) extends Opcode
    final case class TeeLocal(index: Int) extends Opcode
    final case class GetGlobal(index: Int) extends Opcode
    final case class SetGlobal(index: Int) extends Opcode

    final case class MemoryImmediate(align: Int, offset: Long) extends Opcode
    final case class Load(type_ : Type, sizeAndSize: Option[Load.SizeAndSign], memoryImmediate: MemoryImmediate) extends Opcode
    object Load {
      final case class SizeAndSign(size: Long, sign: Sign)
    }
    final case class Store(type_ : Type, size: Option[Long], memoryImmediate: MemoryImmediate) extends Opcode
    final case object CurrentMemory extends Opcode
    final case object GrowMemory extends Opcode

    final case class Const(type_ : Type, value: Value) extends Opcode

    // integer unary
    final case class CountLeadingZeros(type_ : Type) extends Opcode
    final case class CountTrailingZeros(type_ : Type) extends Opcode
    final case class CountNumberOneBits(type_ : Type) extends Opcode

    // integer binary
    final case class Add(type_ : Type) extends Opcode
    final case class Substract(type_ : Type) extends Opcode
    final case class Multiply(type_ : Type) extends Opcode
    // TODO Option[Sign] is for float where sign is meaningless
    final case class Divide(type_ : Type, sign: Option[Sign]) extends Opcode
    final case class Remainder(type_ : Type, sign: Sign) extends Opcode
    final case class And(type_ : Type) extends Opcode
    final case class Or(type_ : Type) extends Opcode
    final case class Xor(type_ : Type) extends Opcode
    final case class ShiftLeft(type_ : Type) extends Opcode
    final case class ShiftRight(type_ : Type, sign: Sign) extends Opcode
    final case class RotateLeft(type_ : Type) extends Opcode
    final case class RotateRight(type_ : Type) extends Opcode

    // integer test unary
    final case class EqualZero(type_ : Type) extends Opcode

    // integer test binary
    final case class Equal(type_ : Type) extends Opcode
    final case class NotEqual(type_ : Type) extends Opcode
    final case class LessThan(type_ : Type, sign: Option[Sign]) extends Opcode
    final case class GreaterThan(type_ : Type, sign: Option[Sign]) extends Opcode
    final case class LessOrEqual(type_ : Type, sign: Option[Sign]) extends Opcode
    final case class GreaterOrEqual(type_ : Type, sign: Option[Sign]) extends Opcode

    // float unary
    final case class Absolute(type_ : Type) extends Opcode
    final case class Negative(type_ : Type) extends Opcode
    final case class Ceiling(type_ : Type) extends Opcode
    final case class Floor(type_ : Type) extends Opcode
    final case class Nearest(type_ : Type) extends Opcode
    final case class Sqrt(type_ : Type) extends Opcode

    // float binary
    final case class Min(type_ : Type) extends Opcode
    final case class Max(type_ : Type) extends Opcode
    final case class CopySign(type_ : Type) extends Opcode

    // conv
    final case class Wrap(from: Type, to: Type) extends Opcode
    final case class Extend(from: Type, to: Type, sign: Sign) extends Opcode
    final case class Truncate(from: Type, to: Type, sign: Sign) extends Opcode
    final case class Demote(from: Type, to: Type) extends Opcode
    final case class Promote(from: Type, to: Type) extends Opcode
    final case class Convert(from: Type, to: Type, sign: Sign) extends Opcode
    final case class Reinterpret(from: Type, to: Type) extends Opcode
  }

  sealed trait Section
  object Section {
    case class Type(types: Seq[Signature.Function]) extends Section
    case class Import(imports: Seq[Signature.Import]) extends Section
    case class Function(indexes: Seq[Int]) extends Section
    // TODO add more
    case class Memory(memories: Seq[Signature.Memory]) extends Section
    case class Global(globals: Seq[(Signature.Global, Seq[Opcode])]) extends Section
    case class Export(exports: Seq[(String, Kind, Int)]) extends Section
    // TODO add more
    case class Code(codes: Seq[(Seq[(Int, AT.Value)], Seq[Opcode])]) extends Section
  }

  type TypeIndex = Int

  object Signature {
    sealed trait Import {
      val module: String
      val field: String
    }
    object Import {
      case class Function(module: String, field: String, index: TypeIndex) extends Import
      case class Table(module: String, field: String, type_ : Signature.Table) extends Import
      case class Memory(module: String, field: String, type_ : Signature.Memory) extends Import
      case class Global(module: String, field: String, type_ : Signature.Global) extends Import
    }

    case class Function(params: Seq[AT.Value], returns: Seq[AT.Value])
    case class Memory(resizable_limits: ResizableLimits)
    case class Table(elem_type: AT.Element, resizable_limits: ResizableLimits)
    case class Global(content_type: AT.Value, mutability: Boolean)
  }
}
