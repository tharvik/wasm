package scalawasm.ast

import scala.util.parsing.input.Positional

object Tree {
  sealed trait BaseTrait extends Positional
  final case class Variable(id: Either[Long, String]) extends BaseTrait

  object Signature {
    final case class Block(results: Seq[Type.Block]) extends BaseTrait
    final case class Function(params: Seq[Parameter], results: Seq[Type.Value]) extends BaseTrait
    final case class Global(mutable: Boolean, type_ : Type.Value) extends BaseTrait
    final case class Table(resizableLimits: ResizableLimits, type_ : Type.Element) extends BaseTrait
    final case class Memory(resizableLimits: ResizableLimits) extends BaseTrait
  }

  sealed trait Expr extends BaseTrait
  object Expr {
    final case class Block(name: Option[String], sig: Signature.Block, exprs: Seq[Expr]) extends Expr
    final case class Loop(name: Option[String], result: Seq[Type.Block], exprs: Seq[Expr]) extends Expr
    final case class If(name: Option[String], result: Seq[Type.Block], thn: Seq[Expr], els: Seq[Expr]) extends Expr
  }

  sealed trait Opcode extends Expr
  object Opcode {
    final case object Unreachable extends Opcode
    final case object Nop extends Opcode
    final case class Br(label: Variable) extends Opcode
    final case class BrIf(label: Variable) extends Opcode
    final case class BrTable(labels: Seq[Variable], default: Variable) extends Opcode
    final case object Return extends Opcode
    final case class Call(label: Variable) extends Opcode
    final case class CallIndirect(typeref: Option[Variable], sig: Signature.Function) extends Opcode
    final case object Drop extends Opcode
    final case object Select extends Opcode
    final case class GetLocal(label: Variable) extends Opcode
    final case class SetLocal(label: Variable) extends Opcode
    final case class TeeLocal(label: Variable) extends Opcode
    final case class GetGlobal(label: Variable) extends Opcode
    final case class SetGlobal(label: Variable) extends Opcode
    final case class Load(tpe: Type.Value, sizeAndSize: Option[Load.SizeAndSign], offset: Long, align: Long) extends Opcode
    object Load {
      final case class SizeAndSign(size: Long, sign: Sign)
    }
    final case class Store(tpe: Type.Value, size: Option[Long], offset: Long, align: Long) extends Opcode
    final case object CurrentMemory extends Opcode
    final case object GrowMemory extends Opcode
    final case class Const(tpe: Type.Value, value: Value) extends Opcode

    // integer unary
    final case class CountLeadingZeros(type_ : Type.Value) extends Opcode
    final case class CountTrailingZeros(type_ : Type.Value) extends Opcode
    final case class CountNumberOneBits(type_ : Type.Value) extends Opcode

    // integer binary
    final case class Add(type_ : Type.Value) extends Opcode
    final case class Substract(type_ : Type.Value) extends Opcode
    final case class Multiply(type_ : Type.Value) extends Opcode
    // TODO Option[Sign] is for float where sign is meaningless
    final case class Divide(type_ : Type.Value, sign: Option[Sign]) extends Opcode
    final case class Remainder(type_ : Type.Value, sign: Sign) extends Opcode
    final case class And(type_ : Type.Value) extends Opcode
    final case class Or(type_ : Type.Value) extends Opcode
    final case class Xor(type_ : Type.Value) extends Opcode
    final case class ShiftLeft(type_ : Type.Value) extends Opcode
    final case class ShiftRight(type_ : Type.Value, sign: Sign) extends Opcode
    final case class RotateLeft(type_ : Type.Value) extends Opcode
    final case class RotateRight(type_ : Type.Value) extends Opcode

    // integer test unary
    final case class EqualZero(type_ : Type.Value) extends Opcode

    // integer test binary
    final case class Equal(type_ : Type.Value) extends Opcode
    final case class NotEqual(type_ : Type.Value) extends Opcode
    // TODO Option[Sign] is for float where sign is meaningless
    final case class LessThan(type_ : Type.Value, sign: Option[Sign]) extends Opcode
    final case class GreaterThan(type_ : Type.Value, sign: Option[Sign]) extends Opcode
    final case class LessOrEqual(type_ : Type.Value, sign: Option[Sign]) extends Opcode
    final case class GreaterOrEqual(type_ : Type.Value, sign: Option[Sign]) extends Opcode

    // float unary
    final case class Absolute(type_ : Type.Value) extends Opcode
    final case class Negative(type_ : Type.Value) extends Opcode
    final case class Ceiling(type_ : Type.Value) extends Opcode
    final case class Floor(type_ : Type.Value) extends Opcode
    final case class Nearest(type_ : Type.Value) extends Opcode
    final case class Sqrt(type_ : Type.Value) extends Opcode

    // float binary
    final case class Min(type_ : Type.Value) extends Opcode
    final case class Max(type_ : Type.Value) extends Opcode
    final case class CopySign(type_ : Type.Value) extends Opcode

    // conv
    final case class Wrap(from: Type.Value, to: Type.Value) extends Opcode
    final case class Extend(from: Type.Value, to: Type.Value, sign: Sign) extends Opcode
    final case class Truncate(from: Type.Value, to: Type.Value, sign: Sign) extends Opcode
    final case class Demote(from: Type.Value, to: Type.Value) extends Opcode
    final case class Promote(from: Type.Value, to: Type.Value) extends Opcode
    final case class Convert(from: Type.Value, to: Type.Value, sign: Sign) extends Opcode
    final case class Reinterpret(from: Type.Value, to: Type.Value) extends Opcode
  }

  final case class Parameter(name: Option[String], type_ : Type.Value) extends BaseTrait
  final case class Local(name: Option[String], type_ : Type.Value) extends BaseTrait
  final case class Function(name: Option[String], typeref: Option[Variable], sig: Signature.Function, locals: Seq[Local], instrs: Seq[Expr]) extends BaseTrait

  final case class Global(name: Option[String], sig: Signature.Global, instrs: Seq[Expr]) extends BaseTrait
  final case class Table(name: Option[String], sig: Signature.Table) extends BaseTrait
  final case class Element(var_ : Option[Variable], offset: Seq[Expr], vars: Seq[Variable]) extends BaseTrait
  final case class Memory(name: Option[String], sig: Signature.Memory) extends BaseTrait
  final case class Data(var_ : Option[Variable], offset: Seq[Expr], vars: Seq[String]) extends BaseTrait

  final case class Start(var_ : Variable) extends BaseTrait

  final case class TypeDef(name: Option[String], sig: Signature.Function) extends BaseTrait

  trait Import extends BaseTrait {
    val module: String
    val field: String
  }
  object Import {
    final case class Function(module: String, field: String, name: Option[String], typeref: Option[Variable], sig: Signature.Function) extends Import
    final case class Table(module: String, field: String, name: Option[String], sig: Signature.Table) extends Import
    final case class Memory(module: String, field: String, name: Option[String], sig: Signature.Memory) extends Import
    final case class Global(module: String, field: String, name: Option[String], sig: Signature.Global) extends Import
  }

  final case class Export(field: String, kind: Kind, var_ : Variable) extends BaseTrait

  final case class Module(name: Option[String], typedefs: Seq[TypeDef], funcs: Seq[Function], imports: Seq[Import],
                          exports: Seq[Export], table: Option[Table], memory: Option[Memory], globals: Seq[Global],
                          elements: Seq[Element], datas: Seq[Data], start: Option[Start]) extends BaseTrait
}
