import org.scalatest.matchers

import scalawasm.text.ParsingError

package object scalawasm {
  class IsRightBePropertyMatcher extends matchers.BePropertyMatcher[Either[ParsingError, Any]] {
    override def apply(ret: Either[ParsingError, Any]) = matchers.BePropertyMatchResult(ret.isRight, "right")
  }
  val right = new IsRightBePropertyMatcher
}
