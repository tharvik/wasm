package scalawasm.text

import java.io.File
import java.nio.file.{Files, Paths}

import org.scalatest._

import scala.io.Source.fromFile
import scala.language.postfixOps
import scala.sys.process._
import scalawasm.Main.pipe
import scalawasm.binary.Printer
import scalawasm.right

class FromWatToWasm extends FlatSpec with Matchers {
  private def getReferenceBinary(text: String): Stream[Byte] = {
    val f = java.io.File.createTempFile("test", ".wasm")
    val cmd = Seq("wasm", "-e", text, "-o", f.getAbsolutePath)

    cmd.!!

    Files.readAllBytes(Paths get f.getAbsolutePath) toStream
  }

  private def checkSameBinary(text: String) = {
    val own = pipe(text).map(_.toList)
    own should be (right)

    val ref = getReferenceBinary(text)
    val (o, r) = (own.asInstanceOf[Right[Any, List[Byte]]].value, ref)
    Printer.check(o)
    Printer.check(r)
    o should be (r)
  }

  private def readTestData(filename: String): String = fromFile(s"test-data/$filename").mkString

  def coreTestFiles: Stream[File] = {
    val d = new File("test-data")
    d.listFiles filter {_.getName endsWith ".wat"} toStream
  }

  coreTestFiles foreach { f =>
    registerTest(f.getName) {
      checkSameBinary(readTestData(f.getName))
    }
  }

  "The empty module" should "be equals to the reference" in {
    checkSameBinary("(module)")
  }

  "The Arithmetic module" should "be equals to the reference" in {
    checkSameBinary(readTestData("Arithmetic.wat"))
  }

  "The type section" should "be equals to the reference" in {
    checkSameBinary("(module (type (func)))")
    checkSameBinary("(module (type (func (param))))")
    checkSameBinary("(module (type (func (param $x i32))))")
    checkSameBinary(
      """
        |(module
        |  (type $t (func))
        |  (type (func (param $x i32) (result i32)))
        |)
      """.stripMargin)
  }

  "The import section" should "be equals to the reference" in {
    checkSameBinary("""(module (import "spectest" "print" (func)))""")
  }

  "The code section" should "be equals to the reference" in {
    checkSameBinary("""(module (func (i32.const 0) (f32.const 0.0) (f32.store)) (memory 0))""")
    checkSameBinary("""(module (func (i32.const 64) (drop)))""")
  }
}