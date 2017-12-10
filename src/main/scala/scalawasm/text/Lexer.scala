package scalawasm.text

import scala.util.parsing.combinator.RegexParsers
import scalawasm.text.Token._

// https://enear.github.io/2016/03/31/parser-combinators/

object Lexer extends RegexParsers {
  override def skipWhitespace = true
  // TODO handle multi lines comments
  override val whiteSpace = "([ \n\t\r\f]|;;.*\n)+".r

  // TODO check "1e9" is valid, never definied exactly but seems valid
  private def valuelit = """-?[0-9]+(\.[0-9]|e[0-9]+)?""".r ^^ { s => VALUELIT(s) }
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
  private def div_s = "div_s" ^^^ DIVS
  private def div_u = "div_u" ^^^ DIVU
  private def eq = "eq" ^^^ EQ
  private def eqz = "eqz" ^^^ EQZ
  private def extendsi32 = "extendsi32" ^^^ EXTENDSI32
  private def extendui32 = "extendui32" ^^^ EXTENDUI32
  private def ge_s = "ge_s" ^^^ GES
  private def ge_u = "ge_u" ^^^ GEU
  private def gt_s = "gt_s" ^^^ GTS
  private def gt_u = "gt_u" ^^^ GTU
  private def le_s = "le_s" ^^^ LES
  private def le_u = "le_u" ^^^ LEU
  private def lt_s = "lt_s" ^^^ LTS
  private def lt_u = "lt_u" ^^^ LTU
  private def mul = "mul" ^^^ MUL
  private def ne = "ne" ^^^ NE
  private def neg = "neg" ^^^ NEG
  private def or = "or" ^^^ OR
  private def popcnt = "popcnt" ^^^ POPCNT
  private def reinterpretfloat = "reinterpretfloat" ^^^ REINTERPRETFLOAT
  private def rem_s = "rem_s" ^^^ REMS
  private def rem_u = "rem_u" ^^^ REMU
  private def rotl = "rotl" ^^^ ROTL
  private def rotr = "rotr" ^^^ ROTR
  private def shl = "shl" ^^^ SHL
  private def shr_s = "shr_s" ^^^ SHRS
  private def shr_u = "shr_u" ^^^ SHRU
  private def sub = "sub" ^^^ SUB
  private def truncsf32 = "truncsf32" ^^^ TRUNCSF32
  private def truncsf64 = "truncsf64" ^^^ TRUNCSF64
  private def truncuf32 = "truncuf32" ^^^ TRUNCUF32
  private def truncuf64 = "truncuf64" ^^^ TRUNCUF64
  private def wrapi64 = "wrapi64" ^^^ WRAPI64
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
  private def br_if = "br_if" ^^^ BRIF
  private def br_table = "br_table" ^^^ BRTABLE
  private def return_ = "return" ^^^ RETURN
  private def call = "call" ^^^ CALL
  private def call_indirect = "call_indirect" ^^^ CALLINDIRECT
  private def drop = "drop" ^^^ DROP
  private def select = "select" ^^^ SELECT
  private def get_local = "get_local" ^^^ GETLOCAL
  private def set_local = "set_local" ^^^ SETLOCAL
  private def tee_local = "tee_local" ^^^ TEELOCAL
  private def get_global = "get_global" ^^^ GETGLOBAL
  private def set_global = "set_global" ^^^ SETGLOBAL
  private def current_memory = "current_memory" ^^^ CURRENTMEMORY
  private def grow_memory = "grow_memory" ^^^ GROWMEMORY
  private def load = "load" ^^^ LOAD
  private def load8 = "load8" ^^^ LOAD8S
  private def load16 = "load16" ^^^ LOAD16S
  private def load32 = "load32" ^^^ LOAD32S
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
  private def part1 = name | stringlit | lbrace | rbrace | lparen | rparen | dot | slash | i32 | i64 | f32 | f64 |
      anyfunc | add | and | clz | ctz | div_s | div_u | extendsi32 | extendui32 | ge_s | ge_u | gt_s | gt_u | le_s |
      le_u | lt_s | lt_u | mul | or | popcnt | reinterpretfloat | rem_s | rem_u | rotl | rotr | shl | shr_s | shr_u | sub |
      truncsf32 | truncsf64 | truncuf32 | truncuf64 | wrapi64 | xor | offset | align | type_ | mut | block | loop | if_ |
      convert
  private def part2 = module |
      then_ | else_ | end | unreachable | nop | br | br_if | br_table | return_ | drop | select |
      get_local | set_local | tee_local | get_global | set_global | current_memory | grow_memory | load | store | const | func |
      param | result | local | global | table | elem | memory | data | start | typedef | import_ | export |
      module | underscore | equal | store8 | store16 | store32 | unsigned | signed | tokenLoad | tokenEq | tokenCall |
      tokenNe | valuelit
  private def token = part1 | part2

  private def tokens: Parser[List[Token]] = phrase(rep1(token))

  def apply(code: String): List[Token] =
    parse(tokens, code) match {
      case Success(result, _) => result
    }
}
