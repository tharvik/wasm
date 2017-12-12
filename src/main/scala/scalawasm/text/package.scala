package scalawasm

import scalawasm.ast.{Opcode, Tree}
import scala.util.parsing.input.Position

package object text {
  final case class ParsingError(msg: String, pos: Position)

  object TextTree extends Tree {
    type Variable = Either[Int, String]
  }

  object TextOpcode extends Opcode {
    type Variable = Either[Int, String]
  }
}
