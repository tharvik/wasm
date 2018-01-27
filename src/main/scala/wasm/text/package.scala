package wasm

import scala.util.parsing.input.Position

package object text {
  final case class ParsingError(msg: String, pos: Position)
}
