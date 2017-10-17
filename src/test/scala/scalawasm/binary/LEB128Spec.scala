package scalawasm.binary

import org.scalatest._
import scalawasm.binary.LEB128._

class LEB128Spec extends FlatSpec with Matchers {

  private def byteStream(values: Int*): Stream[Byte] = values map {_.toByte} toStream

  "wikipedia examples" should "correctly translate to binary" in {
    Unsigned.pack(20, 624485) should be (byteStream(0xE5, 0x8E, 0x26))
    Signed.pack(20, -624485) should be (byteStream(0x9B, 0xF1, 0x59))
  }

  "wasm types" should "correctly translate to binary" in {
    def pack(value: Int) = Signed.pack(7, value)

    pack(-0x01) should be (byteStream(0x7f))
    pack(-0x02) should be (byteStream(0x7e))
    pack(-0x03) should be (byteStream(0x7d))
    pack(-0x04) should be (byteStream(0x7c))
    pack(-0x10) should be (byteStream(0x70))
    pack(-0x20) should be (byteStream(0x60))
    pack(-0x40) should be (byteStream(0x40))
  }

  "pipelining" should "work" in {
    def pipeU(value: Long) = Unsigned.unpack(Unsigned.pack(20, value))
    def pipeS(value: Long) = Signed.unpack(Signed.pack(20, value))

    pipeU(624485) should be (624485)

    pipeS(624485) should be (624485)
    pipeS(-624485) should be (-624485)
  }
}

