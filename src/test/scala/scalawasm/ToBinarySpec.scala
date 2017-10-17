package scalawasm

import org.scalatest._

import scala.io.Source.fromFile
import scala.sys.process._
import scalawasm.binary.Types._

class ToBinarySpec extends FlatSpec with Matchers {
  "The empty module" should "be equals to the reference" in {
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
    val own = code.toBinary
    val ref = getReferenceBinary(text)

    own should be (ref)
  }

  "types" should "correctly translate to binary" in {
    i32().toBinary should be (Stream(0x7f toByte))
    i64().toBinary should be (Stream(0x7e toByte))
    f32().toBinary should be (Stream(0x7d toByte))
    f64().toBinary should be (Stream(0x7c toByte))
    anyfunc().toBinary should be (Stream(0x70 toByte))
    func().toBinary should be (Stream(0x60 toByte))
  }
}

