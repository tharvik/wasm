package scalawasm.text

import scala.util.parsing.combinator.Parsers
import scalawasm.ast.Token
import scalawasm.ast.Tree.Opcode._
import scalawasm.ast.Tree._
import scalawasm.ast.Token._

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

  private def value: Parser[Value] = positioned {
    ( int ^^ Value.Integral
    | float ^^ Value.Floating ) }

  private def var_ : Parser[Variable] = positioned {
    ( int ^^ { v => Variable(Left(v)) }
    | name ^^ { s => Variable(Right(s)) } ) }
  private def name: Parser[String] = accept("name", { case NAME(n) => n })
  private def string: Parser[String] = accept("string", { case STRINGLIT(s) => s })

  private def type_ : Parser[Type] = positioned {
    ( I32 ^^^ Type.i32
    | I64 ^^^ Type.i64
    | F32 ^^^ Type.f32
    | F64 ^^^ Type.f64 ) }
  private def elem_type = positioned { ANYFUNC ^^^ Type.Element.AnyFunc }

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
  private def sign: Parser[Sign] = positioned {
    ( UNSIGNED ^^^ Sign.Unsigned
    | SIGNED ^^^ Sign.Unsigned ) }
  private def offset: Parser[Long] = OFFSET ~ EQUAL ~> int
  // TODO ensure power of two
  private def align: Parser[Long] = ALIGN ~ EQUAL ~> int
  private def cvtop: Parser[(Type, Type) => Opcode] =
  ( WRAP ^^^ Wrap
  | EXTEND ~> sign ^^ { s => { (from: Type, to: Type) => Extend(from, to, s) } }
  | TRUNC ~> sign ^^ { s => { (from: Type, to: Type) => Truncate(from, to, s) } }
  | DEMOTE ^^^ Demote
  | PROMOTE ^^^ Promote
  | CONVERT ~> sign ^^ { s => { (from: Type, to: Type) => Convert(from, to, s) } }
  | REINTERPRET ^^^ Reinterpret )

  private def block_sig: Parser[Signature.Block] = positioned { rep(LPAREN ~ RESULT ~> rep(type_) <~ RPAREN) ^^ {
    types => Signature.Block(types.flatten) } }
  // type_ : Option[Variable]
  private def typeref: Parser[Variable] = LPAREN ~ TYPE ~> var_ <~ RPAREN
  private def func_sig: Parser[Signature.Function] = positioned { rep(param) ~ rep(result) ^^ {
    case params ~ results => Signature.Function(params.flatten, results.flatten) } }
  private def global_sig: Parser[Signature.Global] = positioned {
    ( type_ ^^ { t => Signature.Global(mutable = false, t) }
    | LPAREN ~ MUT ~> type_ <~ RPAREN ^^ { t => Signature.Global(mutable = true, t) } ) }
  private def table_sig: Parser[Signature.Table] = positioned { nat ~ opt(nat) ~ elem_type ^^ {
    case i ~ m ~ t => Signature.Table(i, m, t) } }
  private def memory_sig: Parser[Signature.Memory] = positioned { nat ~ opt(nat) ^^ {
    case i ~ m => Signature.Memory(i, m) } }

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

  private def loadSizeAndSign: Parser[Option[Opcode.LoadSizeAndSign]] =
    ( LOAD ^^^ None
    | LOAD8 ~> sign ^^ { s => Some(Opcode.LoadSizeAndSign(8, s)) }
    | LOAD16 ~> sign ^^ { s => Some(Opcode.LoadSizeAndSign(16, s)) }
    | LOAD32 ~> sign ^^ { s => Some(Opcode.LoadSizeAndSign(32, s)) }
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
    | BR ~> var_ ^^ { v => Opcode.Br(v) }
    | BR_IF ~> var_ ^^ { v => Opcode.BrIf(v) }
    | BR_TABLE ~> rep1(var_) ^^ { vars => Opcode.BrTable(vars.init, vars.last) }
    | RETURN ^^^ Opcode.Return
    | CALL ~> var_ ^^ { v => Opcode.Call(v) }
    | CALL_INDIRECT ~> var_ ^^ { v => Opcode.CallIndirect(v) }
    | DROP ^^^ Opcode.Drop
    | SELECT ^^^ Opcode.Select
    | GET_LOCAL ~> var_ ^^ { v => Opcode.GetLocal(v) }
    | SET_LOCAL ~> var_ ^^ { v => Opcode.SetLocal(v) }
    | TEE_LOCAL ~> var_ ^^ { v => Opcode.TeeLocal(v) }
    | GET_GLOBAL ~> var_ ^^ { v => Opcode.GetLocal(v) }
    | SET_GLOBAL ~> var_ ^^ { v => Opcode.SetLocal(v) }
    | type_ ~ DOT ~ loadSizeAndSign ~ opt(offset) ~ opt(align) ^^ {
      case t ~ _ ~ sizeAndSign ~ off ~ ali => Opcode.Load(t, sizeAndSign, off.getOrElse(0), ali.getOrElse(0)) }
    | type_ ~ DOT ~ storeSize ~ opt(offset) ~ opt(align) ^^ {
      case t ~ _ ~ size ~ off ~ ali => Opcode.Store(t, size, off.getOrElse(0), ali.getOrElse(0)) }
    | CURRENT_MEMORY ^^^ Opcode.CurrentMemory
    | GROW_MEMORY ^^^ Opcode.GrowMemory
    | type_ >> { t => DOT ~>
      ( CONST ~> value ^^ { v => Opcode.Const(t, v) }
      | unop ^^ { op => op(t) }
      | binop ^^ { op => op(t) }
      | relop ^^ { op => op(t) }
      | cvtop ~ (SLASH ~> type_) ^^ { case op ~ to => op(t, to) } )
    } ) }

  private def func: Parser[Function] = positioned { LPAREN ~ FUNC ~> opt(name) ~ opt(typeref) ~ func_sig ~ rep(local) ~ rep(instr) <~ RPAREN ^^ {
    case n ~ t ~ sig ~ locals ~ instrs => Function(n, t, sig, locals.flatten, instrs) } }
  private def param: Parser[Seq[Parameter]] = LPAREN ~ PARAM ~>
    ( rep(type_) ^^ { types => types.map(t => Parameter(None, t)) }
    | name ~ type_ ^^ { case n ~ t => Seq(Parameter(Some(n), t)) }
    ) <~ RPAREN
  private def result: Parser[Seq[Type]] = LPAREN ~ RESULT ~> rep(type_) <~ RPAREN
  private def local: Parser[Seq[Local]] = LPAREN ~ LOCAL ~>
    ( rep(type_) ^^ { types => types.map(t => Local(None, t)) }
    | name ~ type_ ^^ { case n ~ t => Seq(Local(Some(n), t)) }
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

  private def typedef: Parser[TypeDef] = positioned { LPAREN ~ TYPE ~> opt(name) ~ (LPAREN ~ FUNC ~> func_sig <~ RPAREN ~ RPAREN) ^^ {
    case n ~ sig => TypeDef(n, sig) } }

  private def import_ : Parser[Import] = positioned { LPAREN ~ IMPORT ~> string ~ string ~ imkind <~ RPAREN ^^ {
    case m ~ f ~ k => Import(m, f, k) } }
  private def imkind: Parser[Import.Kind] = positioned { LPAREN ~>
    ( FUNC ~> opt(name) ~ opt(typeref) ~ func_sig ^^ { case n ~ t ~ sig => Import.Kind.Function(n, t, sig)}
    | GLOBAL ~> opt(name) ~ global_sig ^^ { case n ~ sig => Import.Kind.Global(n, sig)}
    | TABLE ~> opt(name) ~ table_sig ^^ { case n ~ sig => Import.Kind.Table(n, sig)}
    | MEMORY ~> opt(name) ~ memory_sig ^^ { case n ~ sig => Import.Kind.Memory(n, sig)}
    ) <~ RPAREN }
  private def export: Parser[Export] = positioned { LPAREN ~ EXPORT ~> string ~ exkind <~ RPAREN ^^ {
    case s ~ k => Export(s, k) } }
  private def exkind: Parser[Export.Kind] = positioned { LPAREN ~>
    ( FUNC ~> var_ ^^ Export.Kind.Function
    | GLOBAL ~> var_ ^^ Export.Kind.Global
    | TABLE ~> var_ ^^ Export.Kind.Table
    | MEMORY ~> var_ ^^ Export.Kind.Memory
    ) <~ RPAREN }

  private def module: Parser[Module] = positioned { LPAREN ~ MODULE ~> opt(name) ~ rep(typedef) ~ rep(func) ~ rep(import_) ~
    rep(export) ~ opt(table) ~ opt(memory) ~ rep(global) ~ rep(elem) ~ rep(data) ~ opt(start) <~ RPAREN ^^ {
    case n ~ td ~ f ~ im ~ ex ~ t ~ m ~ g ~ e ~ d ~ s => Module(n, td, f, im, ex, t, m, g, e, d, s) } }

  def apply(tokens: Seq[Token]): Either[ParsingError, Module] = {
    val reader = new TokenReader(tokens)
    module(reader) match {
      case NoSuccess(msg, next) => Left(ParsingError(msg, next.pos))
      case Success(result, _) => Right(result)
    }
  }
}
