package scalawasm.text

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scalawasm.ast.Token
import scalawasm.ast.Token._

// https://enear.github.io/2016/03/31/parser-combinators/

object Lexer extends RegexParsers {
  override def skipWhitespace = true
  // TODO handle multi lines comments
  override val whiteSpace: Regex = "([ \n\t\r\f]|;;.*\n)+".r

  // TODO int/float lexer is to redo
  private def intlit = positioned {
    ( """[0-9]+""".r ^^ { s => INTLIT(java.lang.Long.parseUnsignedLong(s)) }
    | """-[0-9]+""".r ^^ { s => INTLIT(s.toLong) })
  }

  private def floatlit = positioned { """-?[0-9]+[\.e][0-9]+""".r ^^ { s => FLOATLIT(s.toDouble) } }
  private def name = positioned { """\$[a-zA-Z0-9_\.+-\\*/\\^~=<>!?@#$%&|:'`]+""".r ^^ { s => NAME(s) } }
  // TODO "\x" string to actual char
  private def stringlit = positioned { "\"" ~> ("""([ -!#-\[\]-~])*""".r ^^ { s: String => STRINGLIT(s) }) <~ "\"" }

  private def lbrace = positioned { "{" ^^^ LBRACE }
  private def rbrace = positioned { "}" ^^^ RBRACE }
  private def lparen = positioned { "(" ^^^ LPAREN }
  private def rparen = positioned { ")" ^^^ RPAREN }
  private def dot = positioned { "." ^^^ DOT }
  private def slash = positioned { "/" ^^^ SLASH }
  private def equal = positioned { "=" ^^^ EQUAL }

  private def i32 = positioned { "i32" ^^^ I32 }
  private def i64 = positioned { "i64" ^^^ I64 }
  private def f32 = positioned { "f32" ^^^ F32 }
  private def f64 = positioned { "f64" ^^^ F64 }
  private def anyfunc = positioned { "anyfunc" ^^^ ANYFUNC }

  private def add = positioned { "add" ^^^ ADD }
  private def and = positioned { "and" ^^^ AND }
  private def clz = positioned { "clz" ^^^ CLZ }
  private def ctz = positioned { "ctz" ^^^ CTZ }
  private def convert = positioned { "convert" ^^^ CONVERT }
  private def div = positioned { "div" ^^^ DIV }
  private def eq = positioned { "eq" ^^^ EQ }
  private def eqz = positioned { "eqz" ^^^ EQZ }
  private def extend = positioned { "extend" ^^^ EXTEND }
  private def ge = positioned { "ge" ^^^ GE }
  private def gt = positioned { "gt" ^^^ GT }
  private def le = positioned { "le" ^^^ LE }
  private def lt = positioned { "lt" ^^^ LT }
  private def mul = positioned { "mul" ^^^ MUL }
  private def ne = positioned { "ne" ^^^ NE }
  private def neg = positioned { "neg" ^^^ NEG }
  private def or = positioned { "or" ^^^ OR }
  private def popcnt = positioned { "popcnt" ^^^ POPCNT }
  private def reinterpretfloat = positioned { "reinterpret" ^^^ REINTERPRET }
  private def rem = positioned { "rem" ^^^ REM }
  private def rotl = positioned { "rotl" ^^^ ROTL }
  private def rotr = positioned { "rotr" ^^^ ROTR }
  private def shl = positioned { "shl" ^^^ SHL }
  private def shr = positioned { "shr" ^^^ SHR }
  private def sub = positioned { "sub" ^^^ SUB }
  private def trunc = positioned { "trunc" ^^^ TRUNC }
  private def wrap = positioned { "wrap" ^^^ WRAP }
  private def xor = positioned { "xor" ^^^ XOR }

  private def abs = positioned { "abs" ^^^ ABS }
  private def ceil = positioned { "ceil" ^^^ CEIL }
  private def floor = positioned { "floor" ^^^ FLOOR }
  private def nearest = positioned { "nearest" ^^^ NEAREST }
  private def sqrt = positioned { "sqrt" ^^^ SQRT }
  private def min = positioned { "min" ^^^ MIN }
  private def max = positioned { "max" ^^^ MAX }
  private def copysign = positioned { "copysign" ^^^ COPYSIGN }

  private def offset = positioned { "offset" ^^^ OFFSET }
  private def align = positioned { "align" ^^^ ALIGN }

  private def type_ = positioned { "type" ^^^ TYPE }
  private def mut = positioned { "mut" ^^^ MUT }

  private def block = positioned { "block" ^^^ BLOCK }
  private def loop = positioned { "loop" ^^^ LOOP }
  private def if_ = positioned { "if" ^^^ IF }
  private def then_ = positioned { "then" ^^^ THEN }
  private def else_ = positioned { "else" ^^^ ELSE }
  private def end = positioned { "end" ^^^ END }

  private def unreachable = positioned { "unreachable" ^^^ UNREACHABLE }
  private def nop = positioned { "nop" ^^^ NOP }
  private def br = positioned { "br" ^^^ BR }
  private def br_if = positioned { "br_if" ^^^ BR_IF }
  private def br_table = positioned { "br_table" ^^^ BR_TABLE }
  private def return_ = positioned { "return" ^^^ RETURN }
  private def call = positioned { "call" ^^^ CALL }
  private def call_indirect = positioned { "call_indirect" ^^^ CALL_INDIRECT }
  private def drop = positioned { "drop" ^^^ DROP }
  private def select = positioned { "select" ^^^ SELECT }
  private def get_local = positioned { "get_local" ^^^ GET_LOCAL }
  private def set_local = positioned { "set_local" ^^^ SET_LOCAL }
  private def tee_local = positioned { "tee_local" ^^^ TEE_LOCAL }
  private def get_global = positioned { "get_global" ^^^ GET_GLOBAL }
  private def set_global = positioned { "set_global" ^^^ SET_GLOBAL }
  private def current_memory = positioned { "current_memory" ^^^ CURRENT_MEMORY }
  private def grow_memory = positioned { "grow_memory" ^^^ GROW_MEMORY }
  private def load = positioned { "load" ^^^ LOAD }
  private def load8 = positioned { "load8" ^^^ LOAD8 }
  private def load16 = positioned { "load16" ^^^ LOAD16 }
  private def load32 = positioned { "load32" ^^^ LOAD32 }
  private def store = positioned { "store" ^^^ STORE }
  private def store8 = positioned { "store8" ^^^ STORE8 }
  private def store16 = positioned { "store16" ^^^ STORE16 }
  private def store32 = positioned { "store32" ^^^ STORE32 }
  private def const = positioned { "const" ^^^ CONST }

  private def signed = positioned { "_s" ^^^ SIGNED }
  private def unsigned = positioned { "_u" ^^^ UNSIGNED }

  private def func = positioned { "func" ^^^ FUNC }
  private def param = positioned { "param" ^^^ PARAM }
  private def result = positioned { "result" ^^^ RESULT }
  private def local = positioned { "local" ^^^ LOCAL }
  private def global = positioned { "global" ^^^ GLOBAL }
  private def table = positioned { "table" ^^^ TABLE }
  private def elem = positioned { "elem" ^^^ ELEM }
  private def memory = positioned { "memory" ^^^ MEMORY }
  private def data = positioned { "data" ^^^ DATA }
  private def start = positioned { "start" ^^^ START }
  private def typedef = positioned { "typedef" ^^^ TYPEDEF }
  private def import_ = positioned { "import" ^^^ IMPORT }
  private def export = positioned { "export" ^^^ EXPORT }

  private def module = positioned { "module" ^^^ MODULE }

  private def token = abs | add | align | and | anyfunc | block | ceil | clz | const | convert | copysign | ctz |
    current_memory | data | div | dot | drop | elem | else_ | end | equal | export | extend | f32 | f64 | floor | func |
    global | grow_memory | gt | i32 | i64 | if_ | import_ | lbrace | le | local | loop | lparen | lt | max | memory |
    min | module | mul | mut | name | nearest | nop | offset | or | param | popcnt | rbrace | reinterpretfloat | rem |
    result | return_ | rotl | rotr | rparen | select | shl | shr | signed | slash | sqrt | start | stringlit | sub |
    table | tee_local | then_ | trunc | unreachable | unsigned | wrap | xor |
    ( intlit ||| floatlit ) |
    ( br ||| br_if ||| br_table ) |
    ( call ||| call_indirect ) |
    ( eq ||| eqz ) |
    ( ge ||| get_local ||| get_global ) |
    ( load ||| load8 ||| load16 ||| load32 ) |
    ( ne ||| neg ) |
    ( set_local ||| set_global ) |
    ( store ||| store8 ||| store16 ||| store32 ) |
    ( type_ ||| typedef )

  private def tokens: Parser[List[Token]] = phrase(rep1(token))

  def apply(code: String): Either[ParsingError, List[Token]] =
    parse(tokens, code) match {
      case NoSuccess(msg, next) => Left(ParsingError(msg, next.pos))
      case Success(result, _) => Right(result)
    }
}
