package scalawasm.text

import scala.util.parsing.combinator.RegexParsers
import scalawasm.ast.{Type => AT}
import scalawasm.{ast => A}

class TextParser extends RegexParsers {
  //lexical.delimiters ++= List("(", ")")
  //lexical.reserved ++= List("")

  // TODO check for truncation
  def valueInt: Parser[Int] = """[0-9]+""".r ^^ { _.toInt }
  def valueLong: Parser[Long] = """[0-9]+""".r ^^ { _.toLong }
  def valueFloat: Parser[Float] = """[0-9]+\.[0-9]*""".r ^^ { _.toFloat }
  def valueDouble: Parser[Double] = """[0-9]+\.[0-9]*""".r ^^ { _.toDouble }
  def variable: Parser[TextTree.Variable] =
    ( """[0-9]+""".r ^^ { s => Left(s.toInt) }
    | name ^^ { s => Right(s) }
    )
  def name: Parser[String] = """\$[a-zA-Z0-9_\.+-*/\\^~=<>!?@#$%&|:'`]+""".r
  def string: Parser[String] = """"([a-zA-Z]|\\([nt\\'"]|[0-9a-fA-F]{2}|u{[0-9a-fA-F]+}))*"""".r

  // --
  def elemType: Parser[A.Type.Trait.Element] = "anyfunc" ^^^ A.Type.anyfunc

  def offset: Parser[Int] = "offset=" ~> valueInt
  def align: Parser[Int] = "align=" ~> valueInt  // TODO only power of two
  def memory_immediate: Parser[TextOpcode.memory_immediate] =
    opt(offset) ~ opt(align) ^^ { case offset ~ align =>
      val o = offset getOrElse 0
      val a = align.map { i: Int => math.log(i) / math.log(2) }.map { _.toInt } getOrElse 0
      TextOpcode.memory_immediate(a, o)
  }

  /*def op: Parser[TextOpcode] =
    ( "unreachable" ^^^ TextOpcode.Unreachable
    | "nop" ^^^ TextOpcode.Nop
    | "br" ~> variable ^^ TextOpcode.Br
    | "br_if" ~> variable ^^ TextOpcode.BrIf
    | "br_table" ~> rep1(variable) ^^ { l => TextOpcode.BrTable(l.init, l.last) }
    | "return" ^^^ TextOpcode.Return
    | "call" ~> variable ^^ TextOpcode.Call
    | "call_indirect" ~> variable ^^ TextOpcode.CallIndirect
    | "drop" ^^^ TextOpcode.Drop
    | "select" ^^^ TextOpcode.Select
    | "get_local" ~> variable ^^ TextOpcode.GetLocal
    | "set_local" ~> variable ^^ TextOpcode.SetLocal
    | "tee_local" ~> variable ^^ TextOpcode.TeeLocal
    | "get_global" ~> variable ^^ TextOpcode.GetGlobal
    | "set_global" ~> variable ^^ TextOpcode.SetGlobal
    | "current_memory" ^^^ TextOpcode.CurrentMemory
    | "grow_memory" ^^^ TextOpcode.GrowMemory
    | i32 | i64 | f32 | f64
    )

  def i32: Parser[A.TextOpcode] =
    ( "i32.load" ~> memory_immediate ^^ TextOpcode.i32.Load
    | "i32.load8_s" ~> memory_immediate ^^ TextOpcode.i32.Load8Signed
    | "i32.load8_u" ~> memory_immediate ^^ TextOpcode.i32.Load8Unsigned
    | "i32.load16_s" ~> memory_immediate ^^ TextOpcode.i32.Load16Signed
    | "i32.load16_u" ~> memory_immediate ^^ TextOpcode.i32.Load16Unsigned
    | "i32.store" ~> memory_immediate ^^ TextOpcode.i32.Store
    | "i32.store8" ~> memory_immediate ^^ TextOpcode.i32.Store8
    | "i32.store16" ~> memory_immediate ^^ TextOpcode.i32.Store16
    | "i32.const" ~> valueInt ^^ TextOpcode.i32.Const
    | "i32.eq" ^^^ TextOpcode.i32.Equal
    | "i32.eqz" ^^^ TextOpcode.i32.EqualZero
    | "i32.ne" ^^^ TextOpcode.i32.NotEqual
    | "i32.lt_s" ^^^ TextOpcode.i32.LessThanSigned
    | "i32.lt_u" ^^^ TextOpcode.i32.LessThanUnsigned
    | "i32.gt_s" ^^^ TextOpcode.i32.GreaterThanSigned
    | "i32.gt_u" ^^^ TextOpcode.i32.GreaterThanUnsigned
    | "i32.le_s" ^^^ TextOpcode.i32.LessOrEqualSigned
    | "i32.le_u" ^^^ TextOpcode.i32.LessOrEqualUnsigned
    | "i32.ge_s" ^^^ TextOpcode.i32.GreaterOrEqualSigned
    | "i32.ge_u" ^^^ TextOpcode.i32.GreaterOrEqualUnsigned
    | "i32.clz" ^^^ TextOpcode.i32.CountLeadingZeros
    | "i32.ctz" ^^^ TextOpcode.i32.CountTrailingZeros
    | "i32.popcnt" ^^^ TextOpcode.i32.CountNumberOneBits
    | "i32.add" ^^^ TextOpcode.i32.Add
    | "i32.sub" ^^^ TextOpcode.i32.Subtracte
    | "i32.mul" ^^^ TextOpcode.i32.Multiply
    | "i32.div_s" ^^^ TextOpcode.i32.DivideSigned
    | "i32.div_u" ^^^ TextOpcode.i32.DivideUnsigned
    | "i32.rem_s" ^^^ TextOpcode.i32.RemainderSigned
    | "i32.rem_u" ^^^ TextOpcode.i32.RemainderUnsigned
    | "i32.and" ^^^ TextOpcode.i32.And
    | "i32.or" ^^^ TextOpcode.i32.Or
    | "i32.xor" ^^^ TextOpcode.i32.Xor
    | "i32.shl" ^^^ TextOpcode.i32.ShiftLeft
    | "i32.shr_u" ^^^ TextOpcode.i32.ShiftRightUnsigned
    | "i32.shr_s" ^^^ TextOpcode.i32.ShiftRightSigned
    | "i32.rotl" ^^^ TextOpcode.i32.RotateLeft
    | "i32.rotr" ^^^ TextOpcode.i32.RotateRight
    | "i32.wrap/i64" ^^^ TextOpcode.i32.WrapFromInt64
    | "i32.trunc_s/f32" ^^^ TextOpcode.i32.TruncateSignedFromFloat32
    | "i32.trunc_u/f32" ^^^ TextOpcode.i32.TruncateUnsignedFromFloat32
    | "i32.trunc_s/f64" ^^^ TextOpcode.i32.TruncateSignedFromFloat64
    | "i32.trunc_u/f64" ^^^ TextOpcode.i32.TruncateUnsignedFromFloat64
    | "i32.reinterpret/f32" ^^^ TextOpcode.i32.ReinterpretFromFloat32
    )

  def i64: Parser[A.TextOpcode] =
    ( "i64.load" ~> memory_immediate ^^ TextOpcode.i64.Load
    | "i64.load8_s" ~> memory_immediate ^^ TextOpcode.i64.Load8Signed
    | "i64.load8_u" ~> memory_immediate ^^ TextOpcode.i64.Load8Unsigned
    | "i64.load16_s" ~> memory_immediate ^^ TextOpcode.i64.Load16Signed
    | "i64.load16_u" ~> memory_immediate ^^ TextOpcode.i64.Load16Unsigned
    | "i64.load32_s" ~> memory_immediate ^^ TextOpcode.i64.Load32Signed
    | "i64.load32_u" ~> memory_immediate ^^ TextOpcode.i64.Load32Unsigned
    | "i64.store" ~> memory_immediate ^^ TextOpcode.i64.Store
    | "i64.store8" ~> memory_immediate ^^ TextOpcode.i64.Store8
    | "i64.store16" ~> memory_immediate ^^ TextOpcode.i64.Store16
    | "i64.store32" ~> memory_immediate ^^ TextOpcode.i64.Store32
    | "i64.const" ~> valueLong ^^ TextOpcode.i64.Const
    | "i64.eqz" ^^^ TextOpcode.i64.EqualZero
    | "i64.eq" ^^^ TextOpcode.i64.Equal
    | "i64.ne" ^^^ TextOpcode.i64.NotEqual
    | "i64.lt_s" ^^^ TextOpcode.i64.LessThanSigned
    | "i64.lt_u" ^^^ TextOpcode.i64.LessThanUnsigned
    | "i64.gt_s" ^^^ TextOpcode.i64.GreaterThanSigned
    | "i64.gt_u" ^^^ TextOpcode.i64.GreaterThanUnsigned
    | "i64.le_s" ^^^ TextOpcode.i64.LessOrEqualSigned
    | "i64.le_u" ^^^ TextOpcode.i64.LessOrEqualUnsigned
    | "i64.ge_s" ^^^ TextOpcode.i64.GreaterOrEqualSigned
    | "i64.ge_u" ^^^ TextOpcode.i64.GreaterOrEqualUnsigned
    | "i64.clz" ^^^ TextOpcode.i64.CountLeadingZeros
    | "i64.ctz" ^^^ TextOpcode.i64.CountTrailingZeros
    | "i64.popcnt" ^^^ TextOpcode.i64.CountNumberOneBits
    | "i64.add" ^^^ TextOpcode.i64.Add
    | "i64.sub" ^^^ TextOpcode.i64.Subtracte
    | "i64.mul" ^^^ TextOpcode.i64.Multiply
    | "i64.div_s" ^^^ TextOpcode.i64.DivideSigned
    | "i64.div_u" ^^^ TextOpcode.i64.DivideUnsigned
    | "i64.rem_s" ^^^ TextOpcode.i64.RemainderSigned
    | "i64.rem_u" ^^^ TextOpcode.i64.RemainderUnsigned
    | "i64.and" ^^^ TextOpcode.i64.And
    | "i64.or" ^^^ TextOpcode.i64.Or
    | "i64.xor" ^^^ TextOpcode.i64.Xor
    | "i64.shl" ^^^ TextOpcode.i64.ShiftLeft
    | "i64.shr_s" ^^^ TextOpcode.i64.ShiftRightSigned
    | "i64.shr_u" ^^^ TextOpcode.i64.ShiftRightUnsigned
    | "i64.rotl" ^^^ TextOpcode.i64.RotateLeft
    | "i64.rotr" ^^^ TextOpcode.i64.RotateRight
    | "i64.extend_s/i32" ^^^ TextOpcode.i64.ExtendSignedFromInt32
    | "i64.extend_u/i32" ^^^ TextOpcode.i64.ExtendUnsignedFromInt32
    | "i64.trunc_s/f32" ^^^ TextOpcode.i64.TruncateSignedFromFloat32
    | "i64.trunc_u/f32" ^^^ TextOpcode.i64.TruncateUnsignedFromFloat32
    | "i64.trunc_s/f64" ^^^ TextOpcode.i64.TruncateSignedFromFloat64
    | "i64.trunc_u/f64" ^^^ TextOpcode.i64.TruncateUnsignedFromFloat64
    | "i64.reinterpret/f64" ^^^ TextOpcode.i64.ReinterpretFromFloat64
    )

  def f32: Parser[A.TextOpcode] =
    ( "f32.load" ~> memory_immediate ^^ TextOpcode.f32.Load
    | "f32.store" ~> memory_immediate ^^ TextOpcode.f32.Store
    | "f32.const" ~> valueFloat ^^ TextOpcode.f32.Const
    | "f32.eq" ^^^ TextOpcode.f32.Equal
    | "f32.ne" ^^^ TextOpcode.f32.NotEqual
    | "f32.lt" ^^^ TextOpcode.f32.LessThan
    | "f32.gt" ^^^ TextOpcode.f32.GreaterThan
    | "f32.le" ^^^ TextOpcode.f32.LessOrEqual
    | "f32.ge" ^^^ TextOpcode.f32.GreaterOrEqual
    | "f32.abs" ^^^ TextOpcode.f32.Absolute
    | "f32.neg" ^^^ TextOpcode.f32.Negative
    | "f32.ceil" ^^^ TextOpcode.f32.Ceiling
    | "f32.floor" ^^^ TextOpcode.f32.Floor
    | "f32.trunc" ^^^ TextOpcode.f32.Truncate
    | "f32.nearest" ^^^ TextOpcode.f32.Nearest
    | "f32.sqrt" ^^^ TextOpcode.f32.Sqrt
    | "f32.add" ^^^ TextOpcode.f32.Add
    | "f32.sub" ^^^ TextOpcode.f32.Substract
    | "f32.mul" ^^^ TextOpcode.f32.Multiply
    | "f32.div" ^^^ TextOpcode.f32.Divide
    | "f32.min" ^^^ TextOpcode.f32.Min
    | "f32.max" ^^^ TextOpcode.f32.Max
    | "f32.copysign" ^^^ TextOpcode.f32.CopySign
    | "f32.convert_s/i32" ^^^ TextOpcode.f32.ConvertSignedFromInt32
    | "f32.convert_u/i32" ^^^ TextOpcode.f32.ConvertUnsignedFromInt32
    | "f32.convert_s/i64" ^^^ TextOpcode.f32.ConvertSignedFromInt64
    | "f32.convert_u/i64" ^^^ TextOpcode.f32.ConvertUnsignedFromInt64
    | "f32.demote/f64" ^^^ TextOpcode.f32.DemoteFromFloat64
    | "f32.reinterpret/i32" ^^^ TextOpcode.f32.ReinterpretFromInt32
    )

  def f64: Parser[A.TextOpcode] =
    ( "f64.load" ~> memory_immediate ^^ TextOpcode.f64.Load
      | "f64.store" ~> memory_immediate ^^ TextOpcode.f64.Store
      | "f64.const" ~> valueDouble ^^ TextOpcode.f64.Const
      | "f64.eq" ^^^ TextOpcode.f64.Equal
      | "f64.ne" ^^^ TextOpcode.f64.NotEqual
      | "f64.lt" ^^^ TextOpcode.f64.LessThan
      | "f64.gt" ^^^ TextOpcode.f64.GreaterThan
      | "f64.le" ^^^ TextOpcode.f64.LessOrEqual
      | "f64.ge" ^^^ TextOpcode.f64.GreaterOrEqual
      | "f64.abs" ^^^ TextOpcode.f64.Absolute
      | "f64.neg" ^^^ TextOpcode.f64.Negative
      | "f64.ceil" ^^^ TextOpcode.f64.Ceiling
      | "f64.floor" ^^^ TextOpcode.f64.Floor
      | "f64.trunc" ^^^ TextOpcode.f64.Truncate
      | "f64.nearest" ^^^ TextOpcode.f64.Nearest
      | "f64.sqrt" ^^^ TextOpcode.f64.Sqrt
      | "f64.add" ^^^ TextOpcode.f64.Add
      | "f64.sub" ^^^ TextOpcode.f64.Substract
      | "f64.mul" ^^^ TextOpcode.f64.Multiply
      | "f64.div" ^^^ TextOpcode.f64.Divide
      | "f64.min" ^^^ TextOpcode.f64.Min
      | "f64.max" ^^^ TextOpcode.f64.Max
      | "f64.copysign" ^^^ TextOpcode.f64.CopySign
      | "f64.convert_s/i32" ^^^ TextOpcode.f64.ConvertSignedFromInt32
      | "f64.convert_u/i32" ^^^ TextOpcode.f64.ConvertUnsignedFromInt32
      | "f64.convert_s/i64" ^^^ TextOpcode.f64.ConvertSignedFromInt64
      | "f64.convert_u/i64" ^^^ TextOpcode.f64.ConvertUnsignedFromInt64
      | "f64.promote/f64" ^^^ TextOpcode.f64.PromoteFromFloat32
      | "f64.reinterpret/i64" ^^^ TextOpcode.f64.ReinterpretFromInt64
      )

  def valueType: Parser[AT.Trait.Value] =
    ( "i32" ^^^ AT.i32
    | "i64" ^^^ AT.i64
    | "f32" ^^^ AT.f32
    | "f64" ^^^ AT.f64
    )

  def blockSig: Parser[Seq[List[AT.Trait.Value]]] = rep("(" ~ "result" ~> rep(valueType) <~ ")")
  def funcSig: Parser[(Option[TextTree.Variable], AT.Function)] =
    opt("(" ~ "type" ~> variable <~ ")") ~ rep(param) ~ rep(result) ^? {
      case v ~ params ~ results if results.count(_.isDefined) > 1 =>
        val ret = results.filter{_.isDefined}.head
        val parms: Seq[AT.Trait.Value] = params
        (v, AT.Function(parms, ret))
    }
  def globalSig: Parser[AT.Global] =
    ( valueType ^^ { t => AT.Global(t, false) }
    | "(" ~ "mut" ~> valueType <~ ")" ^^ { t => AT.Global(t, true) }
    )
  def tableSig: Parser[AT.Table] =
    valueInt ~ opt(valueInt) ~ elemType ^^ { case min ~ max ~ tpe => AT.Table(tpe, AT.ResizableLimits(min, max)) }
  def memorySig: Parser[AT.Memory] =
    valueInt ~ opt(valueInt) ^^ { case min ~ max => AT.Memory(AT.ResizableLimits(min, max)) }

  def expr: Parser[Expr] = "(" ~>
    ( op ~ rep(expr)
    | "block" ~> opt(name) ~ blockSig ~ rep(instr)
    | "loop" ~> opt(name) ~ blockSig ~ rep(instr)
    | "if" ~> opt(name) ~ blockSig ~ rep(expr) <~ "(" ~ "then" ~> rep(instr) <~ ")" ~> opt("(" ~ "else" ~> rep(instr) <~ ")")
    ) <~ ")"

  def instr: Parser[Instruction] = {
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
  }*/

  /*def func: Parser[Function] = "(" ~ "func" ~>
    ( opt(name) ~ funcSig ~ rep(local) ~ rep(instr)
    | opt(name) <~ rep1("(" ~ "export" ~> string <~ ")") ~ funcSig ~ rep(local) ~ rep(instr)
    | opt(name) ~ ("(" ~ "import" ~> string ~ string <~ ")") ~ funcSig
    ) <~ ")"

  def param: Parser[AT.Trait.Value] = "(" ~ "param" ~>
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

  def run(text: String): TextTerm = TextParser.parse(TextParser.word(text))*/
}
