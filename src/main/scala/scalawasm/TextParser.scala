package scalawasm

import scala.math
import scala.util.parsing.combinator.RegexParsers
import scalawasm.{ast => A}
import scalawasm.ast.{Type => AT}
import scalawasm.ast.Opcode

class TextParser extends RegexParsers {
  //lexical.delimiters ++= List("(", ")")
  //lexical.reserved ++= List("")

  // TODO check for truncation
  def valueInt: Parser[Int] = """[0-9]+""".r ^^ { _.toInt }
  def valueLong: Parser[Long] = """[0-9]+""".r ^^ { _.toLong }
  def valueFloat: Parser[Float] = """[0-9]+\.[0-9]*""".r ^^ { _.toFloat }
  def valueDouble: Parser[Double] = """[0-9]+\.[0-9]*""".r ^^ { _.toDouble }
  def variable: Parser[ast.Variable] =
    ( """[0-9]+""".r ^^ { s => ast.VariableWithIndex(s.toInt) }
    | name ^^ { ast.VariableWithName }
    )
  def name: Parser[String] = """\$[a-zA-Z0-9_\.+-*/\\^~=<>!?@#$%&|:'`]+""".r
  def string: Parser[String] = """"([a-zA-Z]|\\([nt\\'"]|[0-9a-fA-F]{2}|u{[0-9a-fA-F]+}))*"""".r

  // --
  def elemType: Parser[ast.Type.Trait.Element] = "anyfunc" ^^^ ast.Type.anyfunc

  def offset: Parser[Int] = "offset=" ~> valueInt
  def align: Parser[Int] = "align=" ~> valueInt  // TODO only power of two
  def memory_immediate: Parser[Opcode.memory_immediate] =
    opt(offset) ~ opt(align) ^^ { case offset ~ align =>
      val o = offset getOrElse 0
      val a = align.map { i: Int => math.log(i) / math.log(2) }.map { _.toInt } getOrElse 0
      Opcode.memory_immediate(a, o)
  }

  def op: Parser[Opcode] =
    ( "unreachable" ^^^ Opcode.Unreachable
    | "nop" ^^^ Opcode.Nop
    | "br" ~> variable ^^ Opcode.Br
    | "br_if" ~> variable ^^ Opcode.BrIf
    | "br_table" ~> rep(variable) ^^ Opcode.BrTable
    | "return" ^^^ Opcode.Return
    | "call" ~> variable ^^ Opcode.Call
    | "call_indirect" ~> variable ^^ Opcode.CallIndirect
    | "drop" ^^^ Opcode.Drop
    | "select" ^^^ Opcode.Select
    | "get_local" ~> variable ^^ Opcode.GetLocal
    | "set_local" ~> variable ^^ Opcode.SetLocal
    | "tee_local" ~> variable ^^ Opcode.TeeLocal
    | "get_global" ~> variable ^^ Opcode.GetGlobal
    | "set_global" ~> variable ^^ Opcode.SetGlobal
    | "current_memory" ^^^ Opcode.CurrentMemory
    | "grow_memory" ^^^ Opcode.GrowMemory
    | i32 | i64 | f32 | f64
    )

  def i32: Parser[ast.Opcode] =
    ( "i32.load" ~> memory_immediate ^^ Opcode.i32.Load
    | "i32.load8_s" ~> memory_immediate ^^ Opcode.i32.Load8Signed
    | "i32.load8_u" ~> memory_immediate ^^ Opcode.i32.Load8Unsigned
    | "i32.load16_s" ~> memory_immediate ^^ Opcode.i32.Load16Signed
    | "i32.load16_u" ~> memory_immediate ^^ Opcode.i32.Load16Unsigned
    | "i32.store" ~> memory_immediate ^^ Opcode.i32.Store
    | "i32.store8" ~> memory_immediate ^^ Opcode.i32.Store8
    | "i32.store16" ~> memory_immediate ^^ Opcode.i32.Store16
    | "i32.const" ~> valueInt ^^ Opcode.i32.Const
    | "i32.eq" ^^^ Opcode.i32.Equal
    | "i32.eqz" ^^^ Opcode.i32.EqualZero
    | "i32.ne" ^^^ Opcode.i32.NotEqual
    | "i32.lt_s" ^^^ Opcode.i32.LessThanSigned
    | "i32.lt_u" ^^^ Opcode.i32.LessThanUnsigned
    | "i32.gt_s" ^^^ Opcode.i32.GreaterThanSigned
    | "i32.gt_u" ^^^ Opcode.i32.GreaterThanUnsigned
    | "i32.le_s" ^^^ Opcode.i32.LessOrEqualSigned
    | "i32.le_u" ^^^ Opcode.i32.LessOrEqualUnsigned
    | "i32.ge_s" ^^^ Opcode.i32.GreaterOrEqualSigned
    | "i32.ge_u" ^^^ Opcode.i32.GreaterOrEqualUnsigned
    | "i32.clz" ^^^ Opcode.i32.CountLeadingZeros
    | "i32.ctz" ^^^ Opcode.i32.CountTrailingZeros
    | "i32.popcnt" ^^^ Opcode.i32.CountNumberOneBits
    | "i32.add" ^^^ Opcode.i32.Add
    | "i32.sub" ^^^ Opcode.i32.Subtracte
    | "i32.mul" ^^^ Opcode.i32.Multiply
    | "i32.div_s" ^^^ Opcode.i32.DivideSigned
    | "i32.div_u" ^^^ Opcode.i32.DivideUnsigned
    | "i32.rem_s" ^^^ Opcode.i32.RemainderSigned
    | "i32.rem_u" ^^^ Opcode.i32.RemainderUnsigned
    | "i32.and" ^^^ Opcode.i32.And
    | "i32.or" ^^^ Opcode.i32.Or
    | "i32.xor" ^^^ Opcode.i32.Xor
    | "i32.shl" ^^^ Opcode.i32.ShiftLeft
    | "i32.shr_u" ^^^ Opcode.i32.ShiftRightUnsigned
    | "i32.shr_s" ^^^ Opcode.i32.ShiftRightSigned
    | "i32.rotl" ^^^ Opcode.i32.RotateLeft
    | "i32.rotr" ^^^ Opcode.i32.RotateRight
    | "i32.wrap/i64" ^^^ Opcode.i32.WrapFromInt64
    | "i32.trunc_s/f32" ^^^ Opcode.i32.TruncateSignedFromFloat32
    | "i32.trunc_u/f32" ^^^ Opcode.i32.TruncateUnsignedFromFloat32
    | "i32.trunc_s/f64" ^^^ Opcode.i32.TruncateSignedFromFloat64
    | "i32.trunc_u/f64" ^^^ Opcode.i32.TruncateUnsignedFromFloat64
    | "i32.reinterpret/f32" ^^^ Opcode.i32.ReinterpretFromFloat32
    )

  def i64: Parser[ast.Opcode] =
    ( "i64.load" ~> memory_immediate ^^ Opcode.i64.Load
    | "i64.load8_s" ~> memory_immediate ^^ Opcode.i64.Load8Signed
    | "i64.load8_u" ~> memory_immediate ^^ Opcode.i64.Load8Unsigned
    | "i64.load16_s" ~> memory_immediate ^^ Opcode.i64.Load16Signed
    | "i64.load16_u" ~> memory_immediate ^^ Opcode.i64.Load16Unsigned
    | "i64.load32_s" ~> memory_immediate ^^ Opcode.i64.Load32Signed
    | "i64.load32_u" ~> memory_immediate ^^ Opcode.i64.Load32Unsigned
    | "i64.store" ~> memory_immediate ^^ Opcode.i64.Store
    | "i64.store8" ~> memory_immediate ^^ Opcode.i64.Store8
    | "i64.store16" ~> memory_immediate ^^ Opcode.i64.Store16
    | "i64.store32" ~> memory_immediate ^^ Opcode.i64.Store32
    | "i64.const" ~> valueLong ^^ Opcode.i64.Const
    | "i64.eqz" ^^^ Opcode.i64.EqualZero
    | "i64.eq" ^^^ Opcode.i64.Equal
    | "i64.ne" ^^^ Opcode.i64.NotEqual
    | "i64.lt_s" ^^^ Opcode.i64.LessThanSigned
    | "i64.lt_u" ^^^ Opcode.i64.LessThanUnsigned
    | "i64.gt_s" ^^^ Opcode.i64.GreaterThanSigned
    | "i64.gt_u" ^^^ Opcode.i64.GreaterThanUnsigned
    | "i64.le_s" ^^^ Opcode.i64.LessOrEqualSigned
    | "i64.le_u" ^^^ Opcode.i64.LessOrEqualUnsigned
    | "i64.ge_s" ^^^ Opcode.i64.GreaterOrEqualSigned
    | "i64.ge_u" ^^^ Opcode.i64.GreaterOrEqualUnsigned
    | "i64.clz" ^^^ Opcode.i64.CountLeadingZeros
    | "i64.ctz" ^^^ Opcode.i64.CountTrailingZeros
    | "i64.popcnt" ^^^ Opcode.i64.CountNumberOneBits
    | "i64.add" ^^^ Opcode.i64.Add
    | "i64.sub" ^^^ Opcode.i64.Subtracte
    | "i64.mul" ^^^ Opcode.i64.Multiply
    | "i64.div_s" ^^^ Opcode.i64.DivideSigned
    | "i64.div_u" ^^^ Opcode.i64.DivideUnsigned
    | "i64.rem_s" ^^^ Opcode.i64.RemainderSigned
    | "i64.rem_u" ^^^ Opcode.i64.RemainderUnsigned
    | "i64.and" ^^^ Opcode.i64.And
    | "i64.or" ^^^ Opcode.i64.Or
    | "i64.xor" ^^^ Opcode.i64.Xor
    | "i64.shl" ^^^ Opcode.i64.ShiftLeft
    | "i64.shr_s" ^^^ Opcode.i64.ShiftRightSigned
    | "i64.shr_u" ^^^ Opcode.i64.ShiftRightUnsigned
    | "i64.rotl" ^^^ Opcode.i64.RotateLeft
    | "i64.rotr" ^^^ Opcode.i64.RotateRight
    | "i64.extend_s/i32" ^^^ Opcode.i64.ExtendSignedFromInt32
    | "i64.extend_u/i32" ^^^ Opcode.i64.ExtendUnsignedFromInt32
    | "i64.trunc_s/f32" ^^^ Opcode.i64.TruncateSignedFromFloat32
    | "i64.trunc_u/f32" ^^^ Opcode.i64.TruncateUnsignedFromFloat32
    | "i64.trunc_s/f64" ^^^ Opcode.i64.TruncateSignedFromFloat64
    | "i64.trunc_u/f64" ^^^ Opcode.i64.TruncateUnsignedFromFloat64
    | "i64.reinterpret/f64" ^^^ Opcode.i64.ReinterpretFromFloat64
    )

  def f32: Parser[ast.Opcode] =
    ( "f32.load" ~> memory_immediate ^^ Opcode.f32.Load
    | "f32.store" ~> memory_immediate ^^ Opcode.f32.Store
    | "f32.const" ~> valueFloat ^^ Opcode.f32.Const
    | "f32.eq" ^^^ Opcode.f32.Equal
    | "f32.ne" ^^^ Opcode.f32.NotEqual
    | "f32.lt" ^^^ Opcode.f32.LessThan
    | "f32.gt" ^^^ Opcode.f32.GreaterThan
    | "f32.le" ^^^ Opcode.f32.LessOrEqual
    | "f32.ge" ^^^ Opcode.f32.GreaterOrEqual
    | "f32.abs" ^^^ Opcode.f32.Absolute
    | "f32.neg" ^^^ Opcode.f32.Negative
    | "f32.ceil" ^^^ Opcode.f32.Ceiling
    | "f32.floor" ^^^ Opcode.f32.Floor
    | "f32.trunc" ^^^ Opcode.f32.Truncate
    | "f32.nearest" ^^^ Opcode.f32.Nearest
    | "f32.sqrt" ^^^ Opcode.f32.Sqrt
    | "f32.add" ^^^ Opcode.f32.Add
    | "f32.sub" ^^^ Opcode.f32.Substract
    | "f32.mul" ^^^ Opcode.f32.Multiply
    | "f32.div" ^^^ Opcode.f32.Divide
    | "f32.min" ^^^ Opcode.f32.Min
    | "f32.max" ^^^ Opcode.f32.Max
    | "f32.copysign" ^^^ Opcode.f32.CopySign
    | "f32.convert_s/i32" ^^^ Opcode.f32.ConvertSignedFromInt32
    | "f32.convert_u/i32" ^^^ Opcode.f32.ConvertUnsignedFromInt32
    | "f32.convert_s/i64" ^^^ Opcode.f32.ConvertSignedFromInt64
    | "f32.convert_u/i64" ^^^ Opcode.f32.ConvertUnsignedFromInt64
    | "f32.demote/f64" ^^^ Opcode.f32.DemoteFromFloat64
    | "f32.reinterpret/i32" ^^^ Opcode.f32.ReinterpretFromInt32
    )

  def f64: Parser[ast.Opcode] =
    ( "f64.load" ~> memory_immediate ^^ Opcode.f64.Load
      | "f64.store" ~> memory_immediate ^^ Opcode.f64.Store
      | "f64.const" ~> valueDouble ^^ Opcode.f64.Const
      | "f64.eq" ^^^ Opcode.f64.Equal
      | "f64.ne" ^^^ Opcode.f64.NotEqual
      | "f64.lt" ^^^ Opcode.f64.LessThan
      | "f64.gt" ^^^ Opcode.f64.GreaterThan
      | "f64.le" ^^^ Opcode.f64.LessOrEqual
      | "f64.ge" ^^^ Opcode.f64.GreaterOrEqual
      | "f64.abs" ^^^ Opcode.f64.Absolute
      | "f64.neg" ^^^ Opcode.f64.Negative
      | "f64.ceil" ^^^ Opcode.f64.Ceiling
      | "f64.floor" ^^^ Opcode.f64.Floor
      | "f64.trunc" ^^^ Opcode.f64.Truncate
      | "f64.nearest" ^^^ Opcode.f64.Nearest
      | "f64.sqrt" ^^^ Opcode.f64.Sqrt
      | "f64.add" ^^^ Opcode.f64.Add
      | "f64.sub" ^^^ Opcode.f64.Substract
      | "f64.mul" ^^^ Opcode.f64.Multiply
      | "f64.div" ^^^ Opcode.f64.Divide
      | "f64.min" ^^^ Opcode.f64.Min
      | "f64.max" ^^^ Opcode.f64.Max
      | "f64.copysign" ^^^ Opcode.f64.CopySign
      | "f64.convert_s/i32" ^^^ Opcode.f64.ConvertSignedFromInt32
      | "f64.convert_u/i32" ^^^ Opcode.f64.ConvertUnsignedFromInt32
      | "f64.convert_s/i64" ^^^ Opcode.f64.ConvertSignedFromInt64
      | "f64.convert_u/i64" ^^^ Opcode.f64.ConvertUnsignedFromInt64
      | "f64.promote/f64" ^^^ Opcode.f64.PromoteFromFloat32
      | "f64.reinterpret/i64" ^^^ Opcode.f64.ReinterpretFromInt64
      )

  def valueType: Parser[AT.Trait.Value] =
    ( "i32" ^^^ AT.i32
    | "i64" ^^^ AT.i64
    | "f32" ^^^ AT.f32
    | "f64" ^^^ AT.f64
    )

  def blockSig: Parser[Seq[List[AT.Trait.Value]]] = rep("(" ~ "result" ~> rep(valueType) <~ ")")
  def funcSig: Parser[(Option[ast.Variable], AT.Function)] =
    opt("(" ~ "type" ~> variable <~ ")") ~ rep(param) ~ rep(result) ^? {
      case v ~ params ~ results if results.count(_.isDefined) > 1 =>
        val ret = results.filter{_.isDefined}.head
        (v, AT.Function(params, ret))
    }
  def globalSig: Parser[AT.Global] =
    ( valueType ^^ { t => AT.Global(t, false) }
    | "(" ~ "mut" ~> valueType <~ ")" ^^ { t => AT.Global(t, true) }
    )
  def tableSig: Parser[AT.Table] =
    valueInt ~ opt(valueInt) ~ elemType ^^ { case min ~ max ~ tpe => AT.Table(tpe, AT.ResizableLimits(min, max)) }
  def memorySig: Parser[AT.Memory] =
    valueInt ~ opt(valueInt) ^^ { case min ~ max => AT.Memory(AT.ResizableLimits(min, max)) }

  def expr: Parser[???] = "(" ~>
    ( op ~ rep(expr)
    | "block" ~> opt(name) ~ blockSig ~ rep(instr)
    | "loop" ~> opt(name) ~ blockSig ~ rep(instr)
    | "if" ~> opt(name) ~ blockSig ~ rep(expr) <~ "(" ~ "then" ~> rep(instr) <~ ")" ~> opt("(" ~ "else" ~> rep(instr) <~ ")")
    ) <~ ")"

  def instr: Parser[???] = {
    def blockBased(id: String) = id ~>
      ( blockSig ~ rep(instr) ~ "end"
      | (name into { n => blockSig ~ rep(instr) <~ "end" ~ n })
      )

    ( expr
    | op
    | blockBased("block")
    | blockBased("loop")
    | blockBased("if")
    )
  }

  def func: Parser[???] = "(" ~ "func" ~>
    ( opt(name) ~ funcSig ~ rep(local) ~ rep(instr)
    | opt(name) <~ rep1("(" ~ "export" ~> string <~ ")") ~ funcSig ~ rep(local) ~ rep(instr)
    | opt(name) ~ ("(" ~ "import" ~> string ~ string <~ ")") ~ funcSig
    ) <~ ")"

  def param: Parser[???] = "(" ~ "param" ~>
    ( rep(valueType)
    | name ~ valueType
  ) <~ ")"

  def result: Parser[Option[AT.Trait.Value]] =
    "(" ~ "result" ~> rep(valueType) <~ ")" ^? { case l: Seq[AT.Trait.Value] if l.size <= 1 => l.headOption }

  def local: Parser[???] = "(" ~ "local" ~>
    ( rep(valueType)
    | name ~ valueType
    ) <~ ")"

  def global: Parser[???] = "(" ~ "global" ~>
    ( opt(name) ~ globalSig ~ rep(instr)
    | opt(name) ~ rep("(" ~ "export" ~> string <~ ")") ~ globalSig ~ rep(instr)
    | opt(name) ~ rep("(" ~ "import" ~> string ~ string <~ ")") ~ globalSig
    ) <~ ")"

  def table: Parser[???] = "(" ~ "table" ~> opt(name) ~
    ( tableSig
    | rep1("(" ~ "export" ~> string <~ ")") ~ tableSig
    | ("(" ~ "import" ~> string ~ string <~ ")") ~ tableSig
    | rep("(" ~ "export" ~> string <~ ")") ~ elemType <~ "(" ~ "elem" ~> rep(variable) <~ ")"
    ) <~ ")"

  def elem: Parser[???] = "(" ~ "elem" ~> opt(variable) ~
    ( "(" ~ "offset" ~> rep(instr) <~ ")"
    | expr
    ) ~ rep(variable) ~ ")"

  def memory: Parser[???] = "(" ~ "memory" ~> opt(name) ~
    ( memorySig
    | rep1("(" ~ "export" ~> string <~ ")") ~ memorySig
    | ("(" ~ "import" ~> string ~ string <~ ")") ~ memorySig
    | rep("(" ~ "import" ~> string ~ string <~ ")") ~ "(" ~ "data" ~> rep(string) <~ ")"
    ) <~ ")"

  def data: Parser[???] = "(" ~ "data" ~> opt(variable) ~
    ( "(" ~ "offset" ~> rep(instr) <~ ")" ~ rep(string)
    | expr ~ rep(string)
    ) <~ ")"

  def start: Parser[???] = "(" ~ "start" ~> variable <~ ")"
  def typedef: Parser[???] = "(" ~ "type" ~> opt(name) <~ "(" ~ "func" ~> funcSig <~ ")" <~ ")"

  def imprt: Parser[???] = "(" ~ "import" ~> string ~ string ~ imkind <~ ")"
  def imkind: Parser[???] = "(" ~
    ( "func" ~> opt(name) ~ funcSig
    | "global" ~> opt(name) ~ globalSig
    | "table" ~> opt(name) ~ tableSig
    | "memory" ~> opt(name) ~ memorySig
    ) ~ ")"

  def export: Parser[???] = "(" ~ "export" ~> string ~ exkind <~ ")"
  def exkind: Parser[???] = "(" ~
    ( "func" ~> variable
    | "global" ~> variable
    | "table" ~> variable
    | "memory" ~> variable
    ) ~ ")"

  def module: Parser[???] = {
    def moduleArgs = rep(func) ~ rep(imprt) ~ rep(export) ~ opt(table) ~ opt(memory) ~ rep(global) ~ rep(elem) ~ rep(data) ~ opt(start)
    ("(" ~ "module" ~> opt(name) ~ rep(typedef) ~ moduleArgs <~ ")"
    | rep(typedef) ~ moduleArgs
    )
  }
}
