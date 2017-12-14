package scalawasm.binary

import java.nio.file.{Files, Paths}

import org.scalatest._

import scala.io.Source.fromFile
import scala.sys.process._

import scalawasm.Main.pipe

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

  "The trivial type module" should "be equals to the reference" in {
    checkSameBinary("(module)")
  }

  "The Type module" should "be equals to the reference" in {
    checkSameBinary(readTestData("Types.wat"))
  }
}