package scalawasm

package object ast {
  object Trait {
    abstract class Term
  }

  final case class Preamble(sections: Seq[Section]) extends Trait.Term

  sealed trait Variable
  final case class VariableWithIndex(i: Int) extends Variable
  final case class VariableWithName(n: String) extends Variable
}

// TODO final case class init_expr()
