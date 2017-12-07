package scalawasm

package object ast {
  final case class Preamble(sections: Seq[Section])

  trait Tree {
    type Variable
  }
}

// TODO final case class init_expr()
