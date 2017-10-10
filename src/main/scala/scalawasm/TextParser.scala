package scalawasm

import scala.util.parsing.combinator.RegexParsers
import scalawasm.binary._

class TextParser extends RegexParsers {
  //lexical.delimiters ++= List("(", ")")
  //lexical.reserved ++= List("")

  /*def value[T: Numeric]: Parser[T] =
    ( """[0-9]+""".r ^^ { _.toInt }
    | """[0-9]+\.[0-9]*""".r ^^ { implicitly[Numeric[T]].toFloat _ }
    )*/

  /*def variable: Parser[Term] =
    ( """[0-9]+""".r ^^ { _.toInt }
    | name
    )*/

  def name: Parser[String] =
    """\$[a-zA-Z0-9_\.+-*/\\^~=<>!?@#$%&|:'`]+""".r

  def string: Parser[String] =
    """"([a-zA-Z]|\\([nt\\'"]|[0-9a-fA-F]{2}|u{[0-9a-fA-F]+}))*"""".r

  def value_type: Parser[value_type] =
    ( "i32" ^^ { x => i32() }
    | "i64" ^^ { x => i64() }
    | "f32" ^^ { x => f32() }
    | "f64" ^^ { x => f64() }
    )

  def elem_type: Parser[String] =
    """anyfunc"""

  def unop: Parser[String] =
    """ctz|clz|popcnt""" // TODO | ...

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
