package scalawasm.text

object Ast {
  final case class Variable(id: Either[Long, String])

  sealed trait Value
  object Value {
    final case class Integral(v: Long) extends Value
    final case class Floating(v: Double) extends Value
  }

  sealed trait Sign
  object Sign {
    final case object Signed extends Sign
    final case object Unsigned extends Sign
  }

  sealed trait Type
  object Type {
    sealed trait Element
    object Element {
      final case object AnyFunc extends Element
    }
    final case object i32 extends Type
    final case object i64 extends Type
    final case object f32 extends Type
    final case object f64 extends Type
  }

  object Signature {
    final case class Block(results: Seq[Type])
    final case class Function(type_ : Option[Variable], params: Seq[Parameter], results: Seq[Type])
    final case class Global(mutable: Boolean, type_ : Type)
    final case class Table(initial: Long, maximum: Option[Long], type_ : Type.Element)
    final case class Memory(initial: Long, maximum: Option[Long])
  }

  sealed trait Expr
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
    final case object CountLeadingZeros extends Opcode
    final case object CountTrailingZeros extends Opcode
    final case object CountNumberOneBits extends Opcode

    // integer binary
    final case object Add extends Opcode
    final case object Substract extends Opcode
    final case object Multiply extends Opcode
    // TODO Option[Sign] is for float where sign is meaningless
    final case class Divide(sign: Option[Sign]) extends Opcode
    final case class Remainder(sign: Sign) extends Opcode
    final case object And extends Opcode
    final case object Or extends Opcode
    final case object Xor extends Opcode
    final case object ShiftLeft extends Opcode
    final case class ShiftRight(sign: Sign) extends Opcode
    final case object RotateLeft extends Opcode
    final case object RotateRight extends Opcode

    // integer test unary
    final case object EqualZero extends Opcode

    // integer test binary
    final case object Equal extends Opcode
    final case object NotEqual extends Opcode
    // TODO Option[Sign] is for float where sign is meaningless
    final case class LessThan(sign: Option[Sign]) extends Opcode
    final case class GreaterThan(sign: Option[Sign]) extends Opcode
    final case class LessOrEqual(sign: Option[Sign]) extends Opcode
    final case class GreaterOrEqual(sign: Option[Sign]) extends Opcode

    // integer conv
    final case object WrapFromInt64 extends Opcode
    final case class ExtendFromInt32(sign: Sign) extends Opcode
    final case class TruncateFromFloat32(sign: Sign) extends Opcode
    final case class TruncateFromFloat64(sign: Sign) extends Opcode
    final case object ReinterpretFromFloat32 extends Opcode

    // float unary
    final case object Absolute extends Opcode
    final case object Negative extends Opcode
    final case object Ceiling extends Opcode
    final case object Floor extends Opcode
    final case object Truncate extends Opcode
    final case object Nearest extends Opcode
    final case object Sqrt extends Opcode

    // float binary
    final case object Min extends Opcode
    final case object Max extends Opcode
    final case object CopySign extends Opcode

    // float conv
    final case class ConvertFromInt32(sign: Sign) extends Opcode
    final case class ConvertFromInt64(sign: Sign) extends Opcode
    final case object PromoteFromFloat32 extends Opcode
    final case object ReinterpretFromInt64 extends Opcode
  }

  final case class Parameter(name: Option[String], type_ : Type)
  final case class Local(name: Option[String], type_ : Type)
  final case class Function(name: Option[String], sig: Signature.Function, locals: Seq[Local], instrs: Seq[Expr])

  final case class Global(name: Option[String], sig: Signature.Global, instrs: Seq[Expr])
  final case class Table(name: Option[String], sig: Signature.Table)
  final case class Element(var_ : Option[Variable], offset: Seq[Expr], vars: Seq[Variable])
  final case class Memory(name: Option[String], sig: Signature.Memory)
  final case class Data(var_ : Option[Variable], offset: Seq[Expr], vars: Seq[String])

  final case class Start(var_ : Variable)

  final case class TypeDef(name: Option[String], sig: Signature.Function)


  final case class Import(module: String, field: String, kind: Import.Kind)
  object Import {
    sealed trait Kind
    object Kind {
      final case class Function(name: Option[String], sig: Signature.Function) extends Kind
      final case class Table(name: Option[String], sig: Signature.Table) extends Kind
      final case class Memory(name: Option[String], sig: Signature.Memory) extends Kind
      final case class Global(name: Option[String], sig: Signature.Global) extends Kind
    }
  }
  final case class Export(field: String, kind: Export.Kind)
  object Export {
    sealed trait Kind
    object Kind {
      final case class Function(var_ : Variable) extends Kind
      final case class Table(var_ : Variable) extends Kind
      final case class Memory(var_ : Variable) extends Kind
      final case class Global(var_ : Variable) extends Kind
    }
  }

  final case class Module(name: Option[String], typedefs: Seq[TypeDef], funcs: Seq[Function], imports: Seq[Import],
                          exports: Seq[Export], table: Option[Table], memory: Option[Memory], globals: Seq[Global],
                          elements: Seq[Element], datas: Seq[Data], start: Option[Start])
}
