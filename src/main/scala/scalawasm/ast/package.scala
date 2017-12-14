package scalawasm

package object ast {
  final case class Preamble(sections: Seq[Section])
}

// TODO final case class init_expr()
