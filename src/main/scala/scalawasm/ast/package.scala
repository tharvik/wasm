package scalawasm

package object ast {
  final case class Preamble(sections: Seq[Section])

  trait Tree {
    type Variable
  }

  //case class Function
}

// TODO final case class init_expr()
