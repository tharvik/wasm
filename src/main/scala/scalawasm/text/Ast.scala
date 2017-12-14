package scalawasm.text

import scala.util.parsing.input.Positional

object Ast {
  sealed trait BaseTrait extends Positional
  final case class Variable(id: Either[Long, String]) extends BaseTrait

  sealed trait Value extends BaseTrait
  object Value {
    final case class Integral(v: Long) extends Value
    final case class Floating(v: Double) extends Value
  }

  sealed trait Sign extends BaseTrait
  object Sign {
    final case object Signed extends Sign
    final case object Unsigned extends Sign
  }

  sealed trait Type extends BaseTrait
  object Type {
    sealed trait Element extends BaseTrait
    object Element {
      final case object AnyFunc extends Element
    }
    final case object i32 extends Type
    final case object i64 extends Type
    final case object f32 extends Type
    final case object f64 extends Type
  }

  object Signature {
    final case class Block(results: Seq[Type]) extends BaseTrait
    final case class Function(params: Seq[Parameter], results: Seq[Type]) extends BaseTrait
    final case class Global(mutable: Boolean, type_ : Type) extends BaseTrait
    final case class Table(initial: Long, maximum: Option[Long], type_ : Type.Element) extends BaseTrait
    final case class Memory(initial: Long, maximum: Option[Long]) extends BaseTrait
  }

  sealed trait Expr extends BaseTrait
  object Expr {
    final case class Block(name: Option[String], sig: Signature.Block, exprs: Seq[Expr]) extends Expr
    final case class Loop(name: Option[String], result: Seq[Type], exprs: Seq[Expr]) extends Expr
    final case class If(name: Option[String], result: Seq[Type], thn: Seq[Expr], els: Seq[Expr]) extends Expr
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
    final case class CallIndirect(label: Variable) extends Opcode
    final case object Drop extends Opcode
    final case object Select extends Opcode
    final case class GetLocal(label: Variable) extends Opcode
    final case class SetLocal(label: Variable) extends Opcode
    final case class TeeLocal(label: Variable) extends Opcode
    final case class GetGlobal(label: Variable) extends Opcode
    final case class SetGlobal(label: Variable) extends Opcode
    final case class LoadSizeAndSign(size: Long, sign: Sign) extends Opcode
    final case class Load(tpe: Type, sizeAndSize: Option[LoadSizeAndSign], offset: Long, align: Long) extends Opcode
    final case class Store(tpe: Type, size: Option[Long], offset: Long, align: Long) extends Opcode
    final case object CurrentMemory extends Opcode
    final case object GrowMemory extends Opcode
    final case class Const(tpe: Type, value: Value) extends Opcode

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
    // TODO Option[Sign] is for float where sign is meaningless
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

  final case class Parameter(name: Option[String], type_ : Type) extends BaseTrait
  final case class Local(name: Option[String], type_ : Type) extends BaseTrait
  final case class Function(name: Option[String], typeref: Option[Variable], sig: Signature.Function, locals: Seq[Local], instrs: Seq[Expr]) extends BaseTrait

  final case class Global(name: Option[String], sig: Signature.Global, instrs: Seq[Expr]) extends BaseTrait
  final case class Table(name: Option[String], sig: Signature.Table) extends BaseTrait
  final case class Element(var_ : Option[Variable], offset: Seq[Expr], vars: Seq[Variable]) extends BaseTrait
  final case class Memory(name: Option[String], sig: Signature.Memory) extends BaseTrait
  final case class Data(var_ : Option[Variable], offset: Seq[Expr], vars: Seq[String]) extends BaseTrait

  final case class Start(var_ : Variable) extends BaseTrait

  final case class TypeDef(name: Option[String], sig: Signature.Function) extends BaseTrait

  final case class Import(module: String, field: String, kind: Import.Kind) extends BaseTrait
  object Import {
    sealed trait Kind extends BaseTrait
    object Kind {
      final case class Function(name: Option[String], typeref: Option[Variable], sig: Signature.Function) extends Kind
      final case class Table(name: Option[String], sig: Signature.Table) extends Kind
      final case class Memory(name: Option[String], sig: Signature.Memory) extends Kind
      final case class Global(name: Option[String], sig: Signature.Global) extends Kind
    }
  }
  final case class Export(field: String, kind: Export.Kind) extends BaseTrait
  object Export {
    sealed trait Kind extends BaseTrait
    object Kind {
      final case class Function(var_ : Variable) extends Kind
      final case class Table(var_ : Variable) extends Kind
      final case class Memory(var_ : Variable) extends Kind
      final case class Global(var_ : Variable) extends Kind
    }
  }

  final case class Module(name: Option[String], typedefs: Seq[TypeDef], funcs: Seq[Function], imports: Seq[Import],
                          exports: Seq[Export], table: Option[Table], memory: Option[Memory], globals: Seq[Global],
                          elements: Seq[Element], datas: Seq[Data], start: Option[Start]) extends BaseTrait
}
