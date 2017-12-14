package scalawasm.binary

import java.nio.file.{Files, Paths}

import org.scalatest._

import scala.io.Source.fromFile
import scala.sys.process._

import scalawasm.ast.{Type => AT}
import scalawasm.binary.{Type => BT}
import scalawasm.text.Main.pipe

class ToBinarySpec extends FlatSpec with Matchers {
  private def getReferenceBinary(text: String): Stream[Byte] = {
    val f = java.io.File.createTempFile("test", ".wasm")
    val cmd = Seq("wasm", "-e", text, "-o", f.getAbsolutePath)

    cmd.!!

    Files.readAllBytes(Paths get f.getAbsolutePath) toStream
  }

  private def checkSameBinary(text: String) = {
    val own = pipe(text).asInstanceOf[Right[String, Stream[Byte]]].value
    val ref = getReferenceBinary(text)

    own should be (ref)
  }

  private def readTestData(filename: String): String = fromFile(s"test-data/$filename").mkString

  "The empty module" should "be equals to the reference" in {
    checkSameBinary("(module)")
  }

  "The Type module" should "be equals to the reference" in {
    checkSameBinary(readTestData("Types.wat"))
  }

  "types" should "correctly translate to binary" in {
    BT.toBinary(AT.i32) should be (Stream(0x7f toByte))
    BT.toBinary(AT.i64) should be (Stream(0x7e toByte))
    BT.toBinary(AT.f32) should be (Stream(0x7d toByte))
    BT.toBinary(AT.f64) should be (Stream(0x7c toByte))
    BT.toBinary(AT.anyfunc) should be (Stream(0x70 toByte))
    BT.toBinary(AT.func) should be (Stream(0x60 toByte))
  }
}

