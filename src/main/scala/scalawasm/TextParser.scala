package scalawasm

import scala.math
import scala.util.parsing.combinator.RegexParsers
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
  def elem_type: Parser[ast.Type.Trait.Element] = "anyfunc" ^^^ ast.Type.anyfunc

  def offset: Parser[Int] = "offset=" ~> valueInt
  def align: Parser[Int] = "align=" ~> valueInt  // TODO only power of two
  def memory_immediate: Parser[Opcode.memory_immediate] =
    opt(offset) ~ opt(align) ^^ { case offset ~ align =>
      val o = offset getOrElse 0
      val a = align.map { i: Int => math.log(i) / math.log(2) }.map { _.toInt } getOrElse 0
      Opcode.memory_immediate(a, o)
  }

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
    | "i32.eq" ^^^ Opcode.i32.Equal
    | "i32.ne" ^^^ Opcode.i32.NotEqual
    | "i32.lt_s" ^^^ Opcode.i32.LessThanSigned
    | "i32.lt_u" ^^^ Opcode.i32.LessThanUnsigned
    | "i32.le_s" ^^^ Opcode.i32.LessOrEqualSigned
    | "i32.le_u" ^^^ Opcode.i32.LessOrEqualUnsigned
    | "i32.gt_s" ^^^ Opcode.i32.GreaterThanSigned
    | "i32.gt_u" ^^^ Opcode.i32.GreaterThanUnsigned
    | "i32.ge_s" ^^^ Opcode.i32.GreaterOrEqualSigned
    | "i32.ge_u" ^^^ Opcode.i32.GreaterOrEqualUnsigned
    | "i32.clz" ^^^ Opcode.i32.CountLeadingZeros
    | "i32.ctz" ^^^ Opcode.i32.CountTrailingZeros
    | "i32.popcnt" ^^^ Opcode.i32.CountNumberOneBits
    | "i32.eqz" ^^^ Opcode.i32.EqualZero
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
      | "i64.store" ~> memory_immediate ^^ Opcode.i64.Store
      | "i64.store8" ~> memory_immediate ^^ Opcode.i64.Store8
      | "i64.store16" ~> memory_immediate ^^ Opcode.i64.Store16
      | "i64.const" ~> valueLong ^^ Opcode.i64.Const
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
      | "i64.shr_u" ^^^ Opcode.i64.ShiftRightUnsigned
      | "i64.shr_s" ^^^ Opcode.i64.ShiftRightSigned
      | "i64.rotl" ^^^ Opcode.i64.RotateLeft
      | "i64.rotr" ^^^ Opcode.i64.RotateRight
      | "i64.eq" ^^^ Opcode.i64.Equal
      | "i64.ne" ^^^ Opcode.i64.NotEqual
      | "i64.lt_s" ^^^ Opcode.i64.LessThanSigned
      | "i64.lt_u" ^^^ Opcode.i64.LessThanUnsigned
      | "i64.le_s" ^^^ Opcode.i64.LessOrEqualSigned
      | "i64.le_u" ^^^ Opcode.i64.LessOrEqualUnsigned
      | "i64.gt_s" ^^^ Opcode.i64.GreaterThanSigned
      | "i64.gt_u" ^^^ Opcode.i64.GreaterThanUnsigned
      | "i64.ge_s" ^^^ Opcode.i64.GreaterOrEqualSigned
      | "i64.ge_u" ^^^ Opcode.i64.GreaterOrEqualUnsigned
      | "i64.clz" ^^^ Opcode.i64.CountLeadingZeros
      | "i64.ctz" ^^^ Opcode.i64.CountTrailingZeros
      | "i64.popcnt" ^^^ Opcode.i64.CountNumberOneBits
      | "i64.eqz" ^^^ Opcode.i64.EqualZero
      | "i64.trunc_s/f32" ^^^ Opcode.i64.TruncateSignedFromFloat32
      | "i64.trunc_u/f32" ^^^ Opcode.i64.TruncateUnsignedFromFloat32
      | "i64.trunc_s/f64" ^^^ Opcode.i64.TruncateSignedFromFloat64
      | "i64.trunc_u/f64" ^^^ Opcode.i64.TruncateUnsignedFromFloat64
      | "i64.reinterpret/f32" ^^^ Opcode.i64.ReinterpretFromFloat64
      )



  /*
  binop: add | sub | mul | ...
  relop: eq | ne | lt | ...
  sign: s|u
  offset: offset=<nat>
  align: align=(1|2|4|8|...)
  cvtop: trunc_s | trunc_u | extend_s | extend_u | ...

  block_sig : ( result <value_type>* )*
  func_sig:   ( type <var> )? <param>* <result>*
  global_sig: <value_type> | ( mut <value_type> )
  table_sig:  <nat> <nat>? <elem_type>
  memory_sig: <nat> <nat>?

  expr:
    ( <op> )
    ( <op> <expr>+ )                                                  ;; = <expr>+ (<op>)
    ( block <name>? <block_sig> <instr>* )
    ( loop <name>? <block_sig> <instr>* )
    ( if <name>? <block_sig> ( then <instr>* ) ( else <instr>* )? )
    ( if <name>? <block_sig> <expr>+ ( then <instr>* ) ( else <instr>* )? ) ;; = <expr>+ (if <name>? <block_sig> (then <instr>*) (else <instr>*)?)

  instr:
    <expr>
    <op>                                                              ;; = (<op>)
    block <name>? <block_sig> <instr>* end <name>?                    ;; = (block <name>? <block_sig> <instr>*)
    loop <name>? <block_sig> <instr>* end <name>?                     ;; = (loop <name>? <block_sig> <instr>*)
    if <name>? <block_sig> <instr>* end <name>?                       ;; = (if <name>? <block_sig> (then <instr>*))
    if <name>? <block_sig> <instr>* else <name>? <instr>* end <name>? ;; = (if <name>? <block_sig> (then <instr>*) (else <instr>*))

  op:
    unreachable
    nop
    br <var>
    br_if <var>
    br_table <var>+
    return
    call <var>
    call_indirect <var>
    drop
    select
    get_local <var>
    set_local <var>
    tee_local <var>
    get_global <var>
    set_global <var>
    <value_type>.load((8|16|32)_<sign>)? <offset>? <align>?
    <value_type>.store(8|16|32)? <offset>? <align>?
    current_memory
    grow_memory
    <value_type>.const <value>
    <value_type>.<unop>
    <value_type>.<binop>
    <value_type>.<testop>
    <value_type>.<relop>
    <value_type>.<cvtop>/<value_type>

  func:    ( func <name>? <func_sig> <local>* <instr>* )
           ( func <name>? ( export <string> )+ <func_sig> <local>* <instr>* ) ;; = (export <string> (func <N>))+ (func <name>? <func_sig> <local>* <instr>*)
           ( func <name>? ( import <string> <string> ) <func_sig>)            ;; = (import <name>? <string> <string> (func <func_sig>))
  param:   ( param <value_type>* ) | ( param <name> <value_type> )
  result:  ( result <value_type> )
  local:   ( local <value_type>* ) | ( local <name> <value_type> )

  global:  ( global <name>? <global_sig> <instr>* )
           ( global <name>? ( export <string> )+ <global_sig> <instr>* )      ;; = (export <string> (global <N>))+ (global <name>? <global_sig> <instr>*)
           ( global <name>? ( import <string> <string> ) <global_sig> )       ;; = (import <name>? <string> <string> (global <global_sig>))
  table:   ( table <name>? <table_sig> )
           ( table <name>? ( export <string> )+ <table_sig> )                 ;; = (export <string> (table <N>))+ (table <name>? <table_sig>)
           ( table <name>? ( import <string> <string> ) <table_sig> )         ;; = (import <name>? <string> <string> (table <table_sig>))
           ( table <name>? ( export <string> )* <elem_type> ( elem <var>* ) ) ;; = (table <name>? ( export <string> )* <size> <size> <elem_type>) (elem (i32.const 0) <var>*)
  elem:    ( elem <var>? (offset <instr>* ) <var>* )
           ( elem <var>? <expr> <var>* )                                      ;; = (elem <var>? (offset <expr>) <var>*)
  memory:  ( memory <name>? <memory_sig> )
           ( memory <name>? ( export <string> )+ <memory_sig> )               ;; = (export <string> (memory <N>))+ (memory <name>? <memory_sig>)
           ( memory <name>? ( import <string> <string> ) <memory_sig> )       ;; = (import <name>? <string> <string> (memory <memory_sig>))
           ( memory <name>? ( export <string> )* ( data <string>* )           ;; = (memory <name>? ( export <string> )* <size> <size>) (data (i32.const 0) <string>*)
  data:    ( data <var>? ( offset <instr>* ) <string>* )
           ( data <var>? <expr> <string>* )                                   ;; = (data <var>? (offset <expr>) <string>*)

  start:   ( start <var> )

  typedef: ( type <name>? ( func <func_sig> ) )

  import:  ( import <string> <string> <imkind> )
  imkind:  ( func <name>? <func_sig> )
           ( global <name>? <global_sig> )
           ( table <name>? <table_sig> )
           ( memory <name>? <memory_sig> )
  export:  ( export <string> <exkind> )
  exkind:  ( func <var> )
           ( global <var> )
           ( table <var> )
           ( memory <var> )

  module:  ( module <name>? <typedef>* <func>* <import>* <export>* <table>? <memory>? <global>* <elem>* <data>* <start>? )
           ( module <name>? <string>+ )
           <typedef>* <func>* <import>* <export>* <table>? <memory>? <global>* <elem>* <data>* <start>?  ;; =
           ( module <typedef>* <func>* <import>* <export>* <table>? <memory>? <global>* <elem>* <data>* <start>? )
     */
}
