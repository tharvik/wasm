import org.scalatest.matchers

import wasm.text.ParsingError

package object wasm {
  class IsRightBePropertyMatcher extends matchers.BePropertyMatcher[Either[ParsingError, Any]] {
    override def apply(ret: Either[ParsingError, Any]) = matchers.BePropertyMatchResult(ret.isRight, "right")
  }
  val right = new IsRightBePropertyMatcher
}
