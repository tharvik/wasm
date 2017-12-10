package scalawasm.text

sealed trait Token

object Token {
  case object LBRACE extends Token
  case object RBRACE extends Token
  case object LPAREN extends Token
  case object RPAREN extends Token
  case object DOT extends Token
  case object SLASH extends Token
  case object UNDERSCORE extends Token
  case object EQUAL extends Token

  case class VALUELIT(value: String) extends Token
  case class NAME(value: String) extends Token
  case class STRINGLIT(value: String) extends Token

  case object I32 extends Token
  case object I64 extends Token
  case object F32 extends Token
  case object F64 extends Token
  case object ANYFUNC extends Token

  case object ADD extends Token
  case object AND extends Token
  case object CLZ extends Token
  case object CTZ extends Token
  case object CONVERT extends Token
  case object DIVS extends Token
  case object DIVU extends Token
  case object EQ extends Token
  case object EQZ extends Token
  case object EXTENDSI32 extends Token
  case object EXTENDUI32 extends Token
  case object GES extends Token
  case object GEU extends Token
  case object GTS extends Token
  case object GTU extends Token
  case object LES extends Token
  case object LEU extends Token
  case object LTS extends Token
  case object LTU extends Token
  case object MUL extends Token
  case object NE extends Token
  case object NEG extends Token
  case object OR extends Token
  case object POPCNT extends Token
  case object REINTERPRETFLOAT extends Token
  case object REMS extends Token
  case object REMU extends Token
  case object ROTL extends Token
  case object ROTR extends Token
  case object SHL extends Token
  case object SHRS extends Token
  case object SHRU extends Token
  case object SUB extends Token
  case object TRUNCSF32 extends Token
  case object TRUNCSF64 extends Token
  case object TRUNCUF32 extends Token
  case object TRUNCUF64 extends Token
  case object WRAPI64 extends Token
  case object XOR extends Token

  case object SIGNED extends Token
  case object UNSIGNED extends Token

  case object OFFSET extends Token
  case object ALIGN extends Token

  case object TYPE extends Token
  case object MUT extends Token

  case object BLOCK extends Token
  case object LOOP extends Token
  case object IF extends Token
  case object THEN extends Token
  case object ELSE extends Token
  case object END extends Token

  case object UNREACHABLE extends Token
  case object NOP extends Token
  case object BR extends Token
  case object BRIF extends Token
  case object BRTABLE extends Token
  case object RETURN extends Token
  case object CALL extends Token
  case object CALLINDIRECT extends Token
  case object DROP extends Token
  case object SELECT extends Token
  case object GETLOCAL extends Token
  case object SETLOCAL extends Token
  case object TEELOCAL extends Token
  case object GETGLOBAL extends Token
  case object SETGLOBAL extends Token
  case object CURRENTMEMORY extends Token
  case object GROWMEMORY extends Token
  case object LOAD extends Token
  case object LOAD8S extends Token
  case object LOAD8U extends Token
  case object LOAD16S extends Token
  case object LOAD16U extends Token
  case object LOAD32S extends Token
  case object LOAD32U extends Token
  case object STORE extends Token
  case object STORE8 extends Token
  case object STORE16 extends Token
  case object STORE32 extends Token
  case object CONST extends Token

  case object FUNC extends Token
  case object PARAM extends Token
  case object RESULT extends Token
  case object LOCAL extends Token
  case object GLOBAL extends Token
  case object TABLE extends Token
  case object ELEM extends Token
  case object MEMORY extends Token
  case object DATA extends Token
  case object START extends Token
  case object TYPEDEF extends Token
  case object IMPORT extends Token
  case object EXPORT extends Token

  case object MODULE extends Token
}
