package scalawasm.text

import java.io.File
import java.nio.file.{Files, Paths}

import org.scalatest._

import scala.io.Source.fromFile
import scala.language.postfixOps
import scala.sys.process._
import scalawasm.Main.pipe
import scalawasm.Config.enableSpecCompat
import scalawasm.binary.Printer
import scalawasm.right

class FromWatToWasm extends FlatSpec with Matchers {
  private def getReferenceBinary(text: String): Stream[Byte] = {
    val f = java.io.File.createTempFile("test", ".wasm")
    val cmd = Seq("wasm", "-e", text, "-o", f.getAbsolutePath)

    cmd.!!

    Files.readAllBytes(Paths get f.getAbsolutePath) toStream
  }

  private def checkBinary(text: String) = {
    val own = pipe(text).map(_.toList)
    own should be (right)

    val o = own.asInstanceOf[Right[Any, List[Byte]]].value
    Printer.check(o)

    if (enableSpecCompat) {
      val ref = getReferenceBinary(text)
      val (o, r) = (own.asInstanceOf[Right[Any, List[Byte]]].value, ref)
      o should be (r)
    }
  }

  private def readTestData(filename: String): String = fromFile(s"test-data/$filename").mkString

  def coreTestFiles: Stream[File] = {
    val d = new File("test-data")
    d.listFiles filter {_.getName endsWith ".wat"} toStream
  }

  coreTestFiles foreach { f =>
    registerTest(f.getName) {
      checkBinary(readTestData(f.getName))
    }
  }

  "The empty module" should "translate" in {
    checkBinary("(module)")
  }

  "The type section" should "translate" in {
    checkBinary("(module (type (func)))")
    checkBinary("(module (type (func (param))))")
    checkBinary("(module (type (func (param $x i32))))")
    checkBinary(
      """
        |(module
        |  (type $t (func))
        |  (type (func (param $x i32) (result i32)))
        |)
      """.stripMargin)
  }

  "The import section" should "translate" in {
    checkBinary("""(module (import "spectest" "print" (func)))""")
  }

  "The function section" should "translate" in {
    checkBinary("""(module (func (i32.const 0) (f32.const 0.0) (f32.store)) (memory 0))""")
    checkBinary("""(module (func (i32.const 64) (drop)))""")
    checkBinary("""(module (func (f32.const 3.0) (drop)))""")
    checkBinary(
      """
        |(module
        |  (type (func))
        |  (type (func))
        |  (func (type 1))
        |)
      """.stripMargin)
    checkBinary(
      """
        |(module
        |  (func (i32.const 0) (i32.const 25) (i32.store8 align=1))
        |  (memory 1)
        |)
      """.stripMargin)
  }
}