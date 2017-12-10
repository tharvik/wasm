package scalawasm.binary

import org.scalatest._

import scala.io.Source.fromFile
import scala.sys.process._
import scalawasm.ast.{Preamble, Type => AT}
import scalawasm.binary.{Type => BT}
import scalawasm.{binary => B}

class ToBinarySpec extends FlatSpec with Matchers {
  /*"The empty module" should "be equals to the reference" in {
    checkSameBinary(Preamble(Seq()), "(module)")
  }

  def getReferenceBinary(text: String): Stream[Byte] = {
    val f = java.io.File.createTempFile("test", ".wasm")
    val cmd = Seq("wasm", "-e", text, "-o", f.getAbsolutePath)

    cmd.!!

    fromFile(f).buffered map(_.toByte) toStream
  }

  // TODO
  //    code -> wat -> ./wasm -> wasm
  //    code -> wasm
  //def checkSameBinary(code: Preamble)

  def checkSameBinary(code: Preamble, text: String) = {
    val own = B.toBinary(code)
    val ref = getReferenceBinary(text)

    own should be (ref)
  }*/

  "types" should "correctly translate to binary" in {
    BT.toBinary(AT.i32) should be (Stream(0x7f toByte))
    BT.toBinary(AT.i64) should be (Stream(0x7e toByte))
    BT.toBinary(AT.f32) should be (Stream(0x7d toByte))
    BT.toBinary(AT.f64) should be (Stream(0x7c toByte))
    BT.toBinary(AT.anyfunc) should be (Stream(0x70 toByte))
    BT.toBinary(AT.func) should be (Stream(0x60 toByte))
  }

  "inline-module.wast" should "correctly translate to binary" in {
  }
}

