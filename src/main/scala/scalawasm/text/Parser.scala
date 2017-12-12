package scalawasm.text

import scala.util.parsing.combinator.Parsers
import scalawasm.text.Token._
import scalawasm.text.Ast._


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

  private def var_ : Parser[Variable] =
    ( int ^^ { v => Variable(Left(v)) }
    | name ^^ { s => Variable(Right(s))} )
  private def name: Parser[String] = accept("name", { case NAME(n) => n })
  private def string: Parser[String] = accept("string", { case STRINGLIT(s) => s })

  private def type_ : Parser[Type] =
    ( I32 ^^^ Type.i32
    | I64 ^^^ Type.i64
    | F32 ^^^ Type.f32
    | F64 ^^^ Type.f64 )
  private def elem_type = ANYFUNC ^^^ Type.Element.AnyFunc

  private def unop = CTZ | CLZ | POPCNT // TODO | ...
  private def binop = ADD | SUB | MUL // TODO | ...
  private def relop = EQ | NE | LT // TODO | ...
  private def sign: Parser[Sign] =
    ( UNSIGNED ^^^ Sign.Unsigned
    | SIGNED ^^^ Sign.Unsigned )
  private def offset: Parser[Long] = OFFSET ~ EQUAL ~> int
  // TODO ensure power of two
  private def align: Parser[Long] = ALIGN ~ EQUAL ~> int
  private def cvtop = ( TRUNC | EXTEND ) ~ sign // TODO | ...

  private def block_sig: Parser[Signature.Block] = LPAREN ~ RESULT ~> rep(type_) <~ RPAREN ^^ Signature.Block
  private def func_sig: Parser[Signature.Function] = opt(LPAREN ~ TYPE ~> var_ <~ RPAREN) ~ rep(param) ~ rep(result) ^^ {
    case t ~ params ~ results => Signature.Function(t, params.flatten, results.flatten) }
  private def global_sig: Parser[Signature.Global] =
    ( type_ ^^ { t => Signature.Global(mutable = false, t) }
    | LPAREN ~ MUT ~> type_ <~ RPAREN ^^ { t => Signature.Global(mutable = true, t) } )
  private def table_sig: Parser[Signature.Table] = nat ~ opt(nat) ~ elem_type ^^ { case i ~ m ~ t => Signature.Table(i, m, t) }
  private def memory_sig: Parser[Signature.Memory] = nat ~ opt(nat) ^^ { case i ~ m => Signature.Memory(i, m) }

  private def ifThen: Parser[List[Expr]] = LPAREN ~ THEN ~> rep(instr) <~ RPAREN
  private def ifElse: Parser[List[Expr]] = LPAREN ~ ELSE ~> rep(instr) <~ RPAREN
  private def expr: Parser[Expr] = LPAREN ~>
    ( op ^^ { op => op }
    | BLOCK ~> opt(name) ~ block_sig ~ rep(instr) ^^ { case n ~ sig ~ instrs => Expr.Block(n, sig, instrs) }
    | LOOP ~> opt(name) ~ block_sig ~ rep(instr) ^^ { case n ~ sig ~ instrs => Expr.Loop(n, sig.results, instrs) }
    | IF ~> opt(name) ~ block_sig ~ ifThen ~ opt(ifElse) ^^ {
        case n ~ sig ~ then_ ~ else_ => Expr.If(n, sig.results, then_, else_.getOrElse(Seq())) }
    ) <~ RPAREN

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

  private def op: Parser[Opcode] =
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
    | type_ >> { t => DOT ~ CONST ~> value ^^ { v => Opcode.Const(t, v) } }
    // TODO add other
    )

  private def func: Parser[Function] = LPAREN ~ FUNC ~> opt(name) ~ func_sig ~ rep(local) ~ rep(instr) <~ RPAREN ^^ {
    case n ~ sig ~ locals ~ instrs => Function(n, sig, locals.flatten, instrs) }
  private def param: Parser[Seq[Parameter]] = LPAREN ~ PARAM ~>
    ( rep(type_) ^^ { types => types.map(t => Parameter(None, t)) }
    | name ~ type_ ^^ { case n ~ t => Seq(Parameter(Some(n), t)) }
    ) <~ RPAREN
  private def result: Parser[Seq[Type]] = LPAREN ~ RESULT ~> rep(type_) <~ RPAREN
  private def local: Parser[Seq[Local]] = LPAREN ~ LOCAL ~>
    ( rep(type_) ^^ { types => types.map(t => Local(None, t)) }
    | name ~ type_ ^^ { case n ~ t => Seq(Local(Some(n), t)) }
    ) <~ RPAREN

  private def global: Parser[Global] = LPAREN ~ GLOBAL ~> opt(name) ~ global_sig ~ rep(instr) <~ RPAREN ^^ {
    case n ~ sig ~ instrs => Global(n, sig, instrs) }

  private def offsetInstrs: Parser[Seq[Expr]] = LPAREN ~ OFFSET ~> rep(instr) <~ RPAREN
  private def table: Parser[Table] = LPAREN ~ TABLE ~> opt(name) ~ table_sig <~ RPAREN ^^ {
    case n ~ sig => Table(n, sig) }
  private def elem: Parser[Element] = LPAREN ~ ELEM ~> opt(var_) ~ offsetInstrs ~ rep(var_) <~ RPAREN ^^ {
    case v ~ off ~ vars => Element(v, off, vars) }
  private def memory: Parser[Memory] = LPAREN ~ MEMORY ~> opt(name) ~ memory_sig <~ RPAREN ^^ {
    case v ~ sig => Memory(v, sig) }
  private def data: Parser[Data] = LPAREN ~ DATA ~> opt(var_) ~ offsetInstrs ~ rep(string) <~ RPAREN ^^ {
    case v ~ off ~ strs => Data(v, off, strs) }

  private def start: Parser[Start] = LPAREN ~ START ~> var_ <~ RPAREN ^^ Start

  private def typedef: Parser[TypeDef] = LPAREN ~ TYPE ~> opt(name) ~ (LPAREN ~ FUNC ~> func_sig <~ RPAREN ~ RPAREN) ^^ {
    case n ~ sig => TypeDef(n, sig) }

  private def import_ : Parser[Import] = LPAREN ~ IMPORT ~> string ~ string ~ imkind <~ RPAREN ^^ {
    case m ~ f ~ k => Import(m, f, k) }
  private def imkind: Parser[Import.Kind] = LPAREN ~>
    ( FUNC ~> opt(name) ~ func_sig ^^ { case n ~ sig => Import.Kind.Function(n, sig)}
    | GLOBAL ~> opt(name) ~ global_sig ^^ { case n ~ sig => Import.Kind.Global(n, sig)}
    | TABLE ~> opt(name) ~ table_sig ^^ { case n ~ sig => Import.Kind.Table(n, sig)}
    | MEMORY ~> opt(name) ~ memory_sig ^^ { case n ~ sig => Import.Kind.Memory(n, sig)}
    ) <~ RPAREN
  private def export: Parser[Export] = LPAREN ~ EXPORT ~> string ~ exkind <~ RPAREN ^^ { case s ~ k => Export(s, k) }
  private def exkind: Parser[Export.Kind] = LPAREN ~>
    ( FUNC ~> var_ ^^ Export.Kind.Function
    | GLOBAL ~> var_ ^^ Export.Kind.Global
    | TABLE ~> var_ ^^ Export.Kind.Table
    | MEMORY ~> var_ ^^ Export.Kind.Memory
    ) <~ RPAREN

  private def module: Parser[Module] = LPAREN ~ MODULE ~> opt(name) ~ rep(typedef) ~ rep(func) ~ rep(import_) ~
    rep(export) ~ opt(table) ~ opt(memory) ~ rep(global) ~ rep(elem) ~ rep(data) ~ opt(start) <~ RPAREN ^^ {
    case n ~ td ~ f ~ im ~ ex ~ t ~ m ~ g ~ e ~ d ~ s => Module(n, td, f, im, ex, t, m, g, e, d, s) }

  def apply(tokens: Seq[Token]): Either[ParsingError, Module] = {
    val reader = new TokenReader(tokens)
    module(reader) match {
      //case NoSuccess(msg, _) => Left(msg)
      case Success(result, _) => Right(result)
    }
  }
}
