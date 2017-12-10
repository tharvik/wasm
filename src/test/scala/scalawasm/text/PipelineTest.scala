package scalawasm.text

import org.scalatest._

import java.io.File
import scala.io.Source.fromFile

class PipelineTest extends FlatSpec with Matchers {

  def coreTestFiles: Stream[File] = {
    val d = new File("test-data")
    d.listFiles filter {_.getName endsWith ".wast"} toStream
  }

  coreTestFiles foreach { f =>
    registerTest(f.getName) {
      Lexer(fromFile(f).mkString)
    }
  }
}
