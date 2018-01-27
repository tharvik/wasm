package wasm.ast

import scala.util.parsing.input.Positional

sealed trait Token extends Positional

object Token {
  case object LBRACE extends Token
  case object RBRACE extends Token
  case object LPAREN extends Token
  case object RPAREN extends Token
  case object DOT extends Token
  case object SLASH extends Token
  case object EQUAL extends Token

  case class INTLIT(value: Long) extends Token
  case class FLOATLIT(value: Double) extends Token
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
  case object DIV extends Token
  case object EQ extends Token
  case object EQZ extends Token
  case object EXTEND extends Token
  case object DEMOTE extends Token
  case object GE extends Token
  case object GT extends Token
  case object LE extends Token
  case object LT extends Token
  case object MUL extends Token
  case object NE extends Token
  case object NEG extends Token
  case object OR extends Token
  case object POPCNT extends Token
  case object PROMOTE extends Token
  case object REINTERPRET extends Token
  case object REM extends Token
  case object ROTL extends Token
  case object ROTR extends Token
  case object SHL extends Token
  case object SHR extends Token
  case object SUB extends Token
  case object TRUNC extends Token
  case object WRAP extends Token
  case object XOR extends Token

  case object ABS extends Token
  case object CEIL extends Token
  case object FLOOR extends Token
  case object NEAREST extends Token
  case object SQRT extends Token
  case object MIN extends Token
  case object MAX extends Token
  case object COPYSIGN extends Token

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
  case object BR_IF extends Token
  case object BR_TABLE extends Token
  case object RETURN extends Token
  case object CALL extends Token
  case object CALL_INDIRECT extends Token
  case object DROP extends Token
  case object SELECT extends Token
  case object GET_LOCAL extends Token
  case object SET_LOCAL extends Token
  case object TEE_LOCAL extends Token
  case object GET_GLOBAL extends Token
  case object SET_GLOBAL extends Token
  case object CURRENT_MEMORY extends Token
  case object GROW_MEMORY extends Token
  case object LOAD extends Token
  case object LOAD8 extends Token
  case object LOAD16 extends Token
  case object LOAD32 extends Token
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
