package scalawasm

package object ast {
  final case class Preamble(sections: Seq[Section])

  sealed trait Variable
  final case class VariableWithIndex(i: Int) extends Variable
  final case class VariableWithName(n: String) extends Variable
}

// TODO final case class init_expr()
