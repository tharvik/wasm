package scalawasm.text

import scala.language.postfixOps
import scala.util.parsing.combinator.Parsers
import scalawasm.ast._
import scalawasm.ast.Tree.Opcode._
import scalawasm.ast.Tree._
import scalawasm.ast.Token._
import scalawasm.Config.enableSpecCompat

object Parser extends Parsers {
  override type Elem = Token

  // TODO undefinied in spec
  private def nat = int
  private def int: Parser[Long] = accept("integral value", {
    case INTLIT(v) => v
  })
  private def float: Parser[Double] = accept("floating value", {
    case FLOATLIT(v) => v
  })

  private def value: Parser[Value] =
    ( int ^^ Value.Integral
    | float ^^ Value.Floating )

  private def var_ : Parser[Variable] = positioned {
    ( int ^^ { v => Variable(Left(v)) }
    | name ^^ { s => Variable(Right(s)) } ) }
  private def name: Parser[String] = accept("name", { case NAME(n) => n })
  private def string: Parser[String] = accept("string", { case STRINGLIT(s) => s })

  private def value_type : Parser[Type.Value] =
    ( I32 ^^^ Type.i32
    | I64 ^^^ Type.i64
    | F32 ^^^ Type.f32
    | F64 ^^^ Type.f64 )
  private def elem_type = ANYFUNC ^^^ Type.AnyFunction

  private def unop: Parser[Type => Opcode] =
  ( CTZ ^^^ CountTrailingZeros
  | CLZ ^^^ CountLeadingZeros
  | POPCNT ^^^ CountNumberOneBits
  | ABS ^^^ Absolute
  | NEG ^^^ Negative
  | SQRT ^^^ Sqrt
  | CEIL ^^^ Ceiling
  | FLOOR ^^^ Floor
  | NEAREST ^^^ Nearest )
  private def binop: Parser[Type => Opcode] =
    ( ADD ^^^ Add
    | SUB ^^^ Substract
    | MUL ^^^ Multiply
    | DIV ~> sign ^^ { s => { t: Type => Divide(t, Some(s)) } }
    | REM ~> sign ^^ { s => { t: Type => Remainder(t, s) } }
    | AND ^^^ And
    | OR ^^^ Or
    | XOR ^^^ Xor
    | SHL ^^^ ShiftLeft
    | SHR ~> sign ^^ { s => { t: Type => ShiftRight(t, s) } }
    | ROTL ^^^ RotateLeft
    | ROTR ^^^ RotateRight )
  private def relop: Parser[Type => Opcode] =
    ( EQ ^^^ Equal
    | NE ^^^ NotEqual
    | EQZ ^^^ EqualZero
    | LT ~> opt(sign) ^^ { s => { t: Type => LessThan(t, s) } }
    | GT ~> opt(sign) ^^ { s => { t: Type => GreaterThan(t, s) } }
    | LE ~> opt(sign) ^^ { s => { t: Type => LessOrEqual(t, s) } }
    | GE ~> opt(sign) ^^ { s => { t: Type => GreaterOrEqual(t, s) } } )
  private def sign: Parser[Sign] =
    ( UNSIGNED ^^^ Sign.Unsigned
    | SIGNED ^^^ Sign.Signed )
  private def offset: Parser[Long] = OFFSET ~ EQUAL ~> int
  // TODO ensure power of two
  private def align: Parser[Long] = ALIGN ~ EQUAL ~> int
  private def cvtop: Parser[(Type.Value, Type.Value) => Opcode] =
  ( WRAP ^^^ Wrap
  | EXTEND ~> sign ^^ { s => { (from: Type.Value, to: Type.Value) => Extend(from, to, s) } }
  | TRUNC ~> sign ^^ { s => { (from: Type.Value, to: Type.Value) => Truncate(from, to, s) } }
  | DEMOTE ^^^ Demote
  | PROMOTE ^^^ Promote
  | CONVERT ~> sign ^^ { s => { (from: Type.Value, to: Type.Value) => Convert(from, to, s) } }
  | REINTERPRET ^^^ Reinterpret )

  private def block_sig: Parser[Signature.Block] = positioned { rep(LPAREN ~ RESULT ~> rep(value_type) <~ RPAREN) ^^ {
    types => Signature.Block(types.flatten) } }
  // TODO add Config.enableSpecCompat
  private def func_sig_without_type = positioned { rep(param) ~ rep(result) ^^ {
    case params ~ results => Signature.Function(params.flatten, results.flatten) } }
  private def typeref = LPAREN ~ TYPE ~> var_ <~ RPAREN
  private def func_sig = opt(typeref) ~ func_sig_without_type
  private def global_sig: Parser[Signature.Global] = positioned {
    ( value_type ^^ { t => Signature.Global(mutable = false, t) }
    | LPAREN ~ MUT ~> value_type <~ RPAREN ^^ { t => Signature.Global(mutable = true, t) } ) }
  private def table_sig: Parser[Signature.Table] = positioned { nat ~ opt(nat) ~ elem_type ^^ {
    case i ~ m ~ t => Signature.Table(ResizableLimits(i toInt, m.map(_.toInt)), t) } } // TODO remove toInt
  private def memory_sig: Parser[Signature.Memory] = positioned { nat ~ opt(nat) ^^ {
    case i ~ m => Signature.Memory(ResizableLimits(i toInt, m.map(_.toInt))) } } // TODO remove toInt

  private def ifThen: Parser[List[Expr]] = LPAREN ~ THEN ~> rep(instr) <~ RPAREN
  private def ifElse: Parser[List[Expr]] = LPAREN ~ ELSE ~> rep(instr) <~ RPAREN
  private def expr: Parser[Expr] = positioned { LPAREN ~>
    ( op ^^ { op => op }
    | BLOCK ~> opt(name) ~ block_sig ~ rep(instr) ^^ { case n ~ sig ~ instrs => Expr.Block(n, sig, instrs) }
    | LOOP ~> opt(name) ~ block_sig ~ rep(instr) ^^ { case n ~ sig ~ instrs => Expr.Loop(n, sig.results, instrs) }
    | IF ~> opt(name) ~ block_sig ~ ifThen ~ opt(ifElse) ^^ {
        case n ~ sig ~ then_ ~ else_ => Expr.If(n, sig.results, then_, else_.getOrElse(Seq())) }
    ) <~ RPAREN }

  private def instr = expr

  private def loadSizeAndSign: Parser[Option[Opcode.Load.SizeAndSign]] =
    ( LOAD ^^^ None
    | LOAD8 ~> sign ^^ { s => Some(Opcode.Load.SizeAndSign(8, s)) }
    | LOAD16 ~> sign ^^ { s => Some(Opcode.Load.SizeAndSign(16, s)) }
    | LOAD32 ~> sign ^^ { s => Some(Opcode.Load.SizeAndSign(32, s)) }
    )

  private def storeSize: Parser[Option[Long]] =
    ( STORE ^^^ None
    | STORE8 ^^^ Some(8.toLong)
    | STORE16 ^^^ Some(16.toLong)
    | STORE32 ^^^ Some(32.toLong)
    )

  private def op: Parser[Opcode] = positioned {
    ( UNREACHABLE ^^^ Opcode.Unreachable
    | NOP ^^^ Opcode.Nop
    | BR ~> var_ ^^ Opcode.Br
    | BR_IF ~> var_ ^^ Opcode.BrIf
    | BR_TABLE ~> rep1(var_) ^^ { vars => Opcode.BrTable(vars.init, vars.last) }
    | RETURN ^^^ Opcode.Return
    | CALL ~> var_ ^^ Opcode.Call
    | CALL_INDIRECT ~> func_sig_without_type ^^ Opcode.CallIndirect // TODO without_type?
    | DROP ^^^ Opcode.Drop
    | SELECT ^^^ Opcode.Select
    | GET_LOCAL ~> var_ ^^ Opcode.GetLocal
    | SET_LOCAL ~> var_ ^^ Opcode.SetLocal
    | TEE_LOCAL ~> var_ ^^ Opcode.TeeLocal
    | GET_GLOBAL ~> var_ ^^ Opcode.GetGlobal
    | SET_GLOBAL ~> var_ ^^ Opcode.SetGlobal
    | value_type ~ DOT ~ loadSizeAndSign ~ opt(offset) ~ opt(align) ^^ {
      case t ~ _ ~ sizeAndSign ~ off ~ ali => Opcode.Load(t, sizeAndSign, off.getOrElse(0), ali.getOrElse(0)) }
    | value_type ~ DOT ~ storeSize ~ opt(offset) ~ opt(align) ^^ {
      case t ~ _ ~ size ~ off ~ ali => Opcode.Store(t, size, off.getOrElse(0), ali.getOrElse(0)) }
    | CURRENT_MEMORY ^^^ Opcode.CurrentMemory
    | GROW_MEMORY ^^^ Opcode.GrowMemory
    | value_type >> { t => DOT ~>
      ( CONST ~> value ^^ { v => Opcode.Const(t, v) }
      | unop ^^ { op => op(t) }
      | binop ^^ { op => op(t) }
      | relop ^^ { op => op(t) }
      | cvtop ~ (SLASH ~> value_type) ^^ { case op ~ to => op(t, to) } )
    } ) }

  private def func: Parser[Function] = positioned { LPAREN ~ FUNC ~> opt(name) ~ func_sig ~ rep(local) ~ rep(instr) <~ RPAREN ^^ {
    case n ~ (t ~ sig) ~ locals ~ instrs => Function(n, t, sig, locals.flatten, instrs) } }
  private def param: Parser[Seq[Parameter]] = LPAREN ~ PARAM ~>
    ( name ~ value_type ^^ { case n ~ t => Seq(Parameter(Some(n), t)) }
    | rep(value_type) ^^ { types => types.map(t => Parameter(None, t)) }
    ) <~ RPAREN
  private def result: Parser[Seq[Type.Value]] = LPAREN ~ RESULT ~> rep(value_type) <~ RPAREN
  private def local: Parser[Seq[Local]] = LPAREN ~ LOCAL ~>
    ( name ~ value_type ^^ { case n ~ t => Seq(Local(Some(n), t)) }
    | rep(value_type) ^^ { types => types.map(t => Local(None, t)) }
    ) <~ RPAREN

  private def global: Parser[Global] = positioned { LPAREN ~ GLOBAL ~> opt(name) ~ global_sig ~ rep(instr) <~ RPAREN ^^ {
    case n ~ sig ~ instrs => Global(n, sig, instrs) } }

  private def offsetInstrs: Parser[Seq[Expr]] = LPAREN ~ OFFSET ~> rep(instr) <~ RPAREN
  private def table: Parser[Table] = positioned { LPAREN ~ TABLE ~> opt(name) ~ table_sig <~ RPAREN ^^ {
    case n ~ sig => Table(n, sig) } }
  private def elem: Parser[Element] = positioned { LPAREN ~ ELEM ~> opt(var_) ~ offsetInstrs ~ rep(var_) <~ RPAREN ^^ {
    case v ~ off ~ vars => Element(v, off, vars) } }
  private def memory: Parser[Memory] = positioned { LPAREN ~ MEMORY ~> opt(name) ~ memory_sig <~ RPAREN ^^ {
    case v ~ sig => Memory(v, sig) } }
  private def data: Parser[Data] = positioned { LPAREN ~ DATA ~> opt(var_) ~ offsetInstrs ~ rep(string) <~ RPAREN ^^ {
    case v ~ off ~ strs => Data(v, off, strs) } }

  private def start: Parser[Start] = positioned { LPAREN ~ START ~> var_ <~ RPAREN ^^ Start }

  private def typedef: Parser[TypeDef] = positioned { LPAREN ~ TYPE ~> opt(name) ~ (LPAREN ~ FUNC ~> func_sig_without_type <~ RPAREN ~ RPAREN) ^^ {
    case n ~ sig => TypeDef(n, sig) } }

  private def import_ : Parser[Import] = positioned { LPAREN ~ IMPORT ~> (string ~ string) >> { case m ~ f =>
    LPAREN ~>
      ( FUNC ~> opt(name) ~ func_sig ^^ { case n ~ (t ~ sig) => Import.Function(m, f, n, t, sig)}
      | GLOBAL ~> opt(name) ~ global_sig ^^ { case n ~ sig => Import.Global(m, f, n, sig)}
      | TABLE ~> opt(name) ~ table_sig ^^ { case n ~ sig => Import.Table(m, f, n, sig)}
      | MEMORY ~> opt(name) ~ memory_sig ^^ { case n ~ sig => Import.Memory(m, f, n, sig)}
      ) <~ RPAREN
    } <~ RPAREN }
  private def export: Parser[Export] = positioned { (LPAREN ~ EXPORT) ~> string ~ (LPAREN ~> (kind ~ var_) <~ (RPAREN ~ RPAREN)) ^^ {
    case s ~ (k ~ v) => Export(s, k, v) } }
  private def kind: Parser[Kind] =
    ( FUNC ^^^ Kind.Function
    | GLOBAL ^^^ Kind.Global
    | TABLE ^^^ Kind.Table
    | MEMORY ^^^ Kind.Memory
    )

  private def moduleSpecCompat: Parser[Module] = positioned { LPAREN ~ MODULE ~> (
    opt(name) ~ rep(typedef) ~ (rep(import_) ~ rep(func)) ~ rep(export) ~ opt(table) ~ opt(memory) ~ rep(global) ~
      rep(elem) ~ rep(data) ~ opt(start) ^^ {
      case n ~ td ~ (im ~ f) ~ ex ~ t ~ m ~ g ~ e ~ d ~ s => Module(n, td, f, im, ex, t, m, g, e, d, s) }
    ) <~ RPAREN }
  private def module: Parser[Module] = positioned { LPAREN ~ MODULE ~> (
    opt(name) ~ rep(typedef) ~ (rep(func) ~ rep(import_)) ~ rep(export) ~ opt(table) ~ opt(memory) ~ rep(global) ~
      rep(elem) ~ rep(data) ~ opt(start) ^^ {
      case n ~ td ~ (f ~ im) ~ ex ~ t ~ m ~ g ~ e ~ d ~ s => Module(n, td, f, im, ex, t, m, g, e, d, s) }
    ) <~ RPAREN }

  def apply(tokens: Seq[Token]): Either[ParsingError, Module] = {
    val reader = new TokenReader(tokens)
    val parser: Parser[Module] = if (enableSpecCompat) moduleSpecCompat else module
    parser(reader) match {
      case NoSuccess(msg, next) => Left(ParsingError(msg, next.pos))
      case Success(result, _) => Right(result)
    }
  }
}
