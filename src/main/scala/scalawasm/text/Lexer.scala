package scalawasm.text

import scala.util.parsing.combinator.RegexParsers
import scalawasm.text.Token._

// https://enear.github.io/2016/03/31/parser-combinators/

object Lexer extends RegexParsers {
  override def skipWhitespace = true
  // TODO handle multi lines comments
  override val whiteSpace = "([ \n\t\r\f]|;;.*\n)+".r

  // TODO check "1e9" is valid, never definied exactly but seems valid
  private def intlit =
    ( """[0-9]+""".r ^^ { s => INTLIT(java.lang.Long.parseUnsignedLong(s)) }
    | """-[0-9]+""".r ^^ { s => INTLIT(s.toLong) } )

  private def floatlit = """-?[0-9]+(\.[0-9]|e[0-9]+)?""".r ^^ { s => FLOATLIT(s.toDouble) }
  private def name = """\$[a-zA-Z0-9_\.+-\\*/\\^~=<>!?@#$%&|:'`]+""".r ^^ { s => NAME(s) }
  // TODO "\x" string to actual char
  private def stringlit = """"([ -!#-\[\]-~])*"""".r ^^ { s => STRINGLIT(s)}

  private def lbrace = "{" ^^^ LBRACE
  private def rbrace = "}" ^^^ RBRACE
  private def lparen = "(" ^^^ LPAREN
  private def rparen = ")" ^^^ RPAREN
  private def dot = "." ^^^ DOT
  private def slash = "/" ^^^ SLASH
  private def underscore = "_" ^^^ UNDERSCORE
  private def equal = "=" ^^^ EQUAL

  private def i32 = "i32" ^^^ I32
  private def i64 = "i64" ^^^ I64
  private def f32 = "f32" ^^^ F32
  private def f64 = "f64" ^^^ F64
  private def anyfunc = "anyfunc" ^^^ ANYFUNC

  private def add = "add" ^^^ ADD
  private def and = "and" ^^^ AND
  private def clz = "clz" ^^^ CLZ
  private def ctz = "ctz" ^^^ CTZ
  private def convert = "convert" ^^^ CONVERT
  private def div = "div" ^^^ DIV
  private def eq = "eq" ^^^ EQ
  private def eqz = "eqz" ^^^ EQZ
  private def extend = "extend" ^^^ EXTEND
  private def ge = "ge" ^^^ GE
  private def gt = "gt" ^^^ GT
  private def le = "le" ^^^ LE
  private def lt = "lt" ^^^ LT
  private def mul = "mul" ^^^ MUL
  private def ne = "ne" ^^^ NE
  private def neg = "neg" ^^^ NEG
  private def or = "or" ^^^ OR
  private def popcnt = "popcnt" ^^^ POPCNT
  private def reinterpretfloat = "reinterpretfloat" ^^^ REINTERPRETFLOAT
  private def rem = "rem" ^^^ REM
  private def rotl = "rotl" ^^^ ROTL
  private def rotr = "rotr" ^^^ ROTR
  private def shl = "shl" ^^^ SHL
  private def shr = "shr" ^^^ SHR
  private def sub = "sub" ^^^ SUB
  private def trunc = "trunc" ^^^ TRUNC
  private def wrap = "wrap" ^^^ WRAP
  private def xor = "xor" ^^^ XOR

  private def offset = "offset" ^^^ OFFSET
  private def align = "align" ^^^ ALIGN

  private def type_ = "type" ^^^ TYPE
  private def mut = "MUT" ^^^ MUT

  private def block = "block" ^^^ BLOCK
  private def loop = "loop" ^^^ LOOP
  private def if_ = "if" ^^^ IF
  private def then_ = "then" ^^^ THEN
  private def else_ = "else" ^^^ ELSE
  private def end = "end" ^^^ END

  private def unreachable = "unreachable" ^^^ UNREACHABLE
  private def nop = "nop" ^^^ NOP
  private def br = "br" ^^^ BR
  private def br_if = "br_if" ^^^ BR_IF
  private def br_table = "br_table" ^^^ BR_TABLE
  private def return_ = "return" ^^^ RETURN
  private def call = "call" ^^^ CALL
  private def call_indirect = "call_indirect" ^^^ CALL_INDIRECT
  private def drop = "drop" ^^^ DROP
  private def select = "select" ^^^ SELECT
  private def get_local = "get_local" ^^^ GET_LOCAL
  private def set_local = "set_local" ^^^ SET_LOCAL
  private def tee_local = "tee_local" ^^^ TEE_LOCAL
  private def get_global = "get_global" ^^^ GET_GLOBAL
  private def set_global = "set_global" ^^^ SET_GLOBAL
  private def current_memory = "current_memory" ^^^ CURRENT_MEMORY
  private def grow_memory = "grow_memory" ^^^ GROW_MEMORY
  private def load = "load" ^^^ LOAD
  private def load8 = "load8" ^^^ LOAD8
  private def load16 = "load16" ^^^ LOAD16
  private def load32 = "load32" ^^^ LOAD32
  private def store = "store" ^^^ STORE
  private def store8 = "store8" ^^^ STORE8
  private def store16 = "store16" ^^^ STORE16
  private def store32 = "store32" ^^^ STORE32
  private def const = "const" ^^^ CONST

  private def signed = "s" ^^^ SIGNED
  private def unsigned = "u" ^^^ UNSIGNED

  private def func = "func" ^^^ FUNC
  private def param = "param" ^^^ PARAM
  private def result = "result" ^^^ RESULT
  private def local = "local" ^^^ LOCAL
  private def global = "global" ^^^ GLOBAL
  private def table = "table" ^^^ TABLE
  private def elem = "elem" ^^^ ELEM
  private def memory = "memory" ^^^ MEMORY
  private def data = "data" ^^^ DATA
  private def start = "start" ^^^ START
  private def typedef = "typedef" ^^^ TYPEDEF
  private def import_ = "import" ^^^ IMPORT
  private def export = "export" ^^^ EXPORT

  private def module = "module" ^^^ MODULE

  private def tokenLoad = load ||| (load8 | load16 | load32)
  private def tokenEq = eq ||| eqz
  private def tokenCall = call ||| call_indirect
  private def tokenNe = ne ||| neg
  private def tokenGe = ge ||| get_local ||| get_global
  private def tokenValue = intlit ||| floatlit
  private def part1 = name | stringlit | lbrace | rbrace | lparen | rparen | dot | slash | i32 | i64 | f32 | f64 |
      anyfunc | add | and | clz | ctz | div | extend | gt |
      le | lt | mul | or | popcnt | reinterpretfloat | rem | rotl | rotr | shl | shr | sub |
      trunc | wrap | xor | offset | align | type_ | mut | block | loop | if_ |
      convert
  private def part2 = module |
      then_ | else_ | end | unreachable | nop | br | br_if | br_table | return_ | drop | select |
      get_local | set_local | tee_local | get_global | set_global | current_memory | grow_memory | load | store | const | func |
      param | result | local | global | table | elem | memory | data | start | typedef | import_ | export |
      module | underscore | equal | store8 | store16 | store32 | unsigned | signed | tokenLoad | tokenEq | tokenCall |
      tokenNe | tokenGe | tokenValue
  private def token = part1 | part2

  private def tokens: Parser[List[Token]] = phrase(rep1(token))

  def apply(code: String): Either[ParsingError, List[Token]] =
    parse(tokens, code) match {
      //case NoSuccess(msg, _) => Left(msg)
      case Success(result, _) => Right(result)
    }
}
