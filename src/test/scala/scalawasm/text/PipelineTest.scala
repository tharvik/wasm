package scalawasm.text

import org.scalatest._
import java.io.File
import scala.io.Source.fromFile

class PipelineTest extends FlatSpec with Matchers {
  def coreTestFiles: Stream[File] = {
    val d = new File("test-data")
    d.listFiles filter {_.getName endsWith "Arithmetic.wat"} toStream
  }

  class IsRightBePropertyMatcher extends matchers.BePropertyMatcher[Either[ParsingError, Any]] {
    override def apply(ret: Either[ParsingError, Any]) = matchers.BePropertyMatchResult(ret.isRight, "right")
  }
  val right = new IsRightBePropertyMatcher

  coreTestFiles foreach { f =>
    registerTest(f.getName) {
      (for {
        tokens <- Lexer(fromFile(f).mkString).right
        ast <- Parser(tokens).right
      } yield ast) should be (right)
    }
  }
}
