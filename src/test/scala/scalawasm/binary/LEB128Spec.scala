package scalawasm.binary

import org.scalatest._
import scalawasm.binary.LEB128._

class LEB128Spec extends FlatSpec with Matchers {

  private def byteStream(values: Int*): Stream[Byte] = values map {_.toByte} toStream

  "wikipedia examples" should "correctly translate to binary" in {
    Unsigned.pack(20, 624485) should be (byteStream(0xE5, 0x8E, 0x26))
    Signed.pack(20, -624485) should be (byteStream(0x9B, 0xF1, 0x59))
  }

  "dwarf examples" should "correctly translate to binary" in {
    def packU(value: Int) = Unsigned.pack(7, value)
    def packS(value: Int) = Signed.pack(7, value)

    packU(2) should be (byteStream(2))
    packU(127) should be (byteStream(127))
    packU(128) should be (byteStream(0 + 0x80, 1))
    packU(129) should be (byteStream(1 + 0x80, 1))
    packU(130) should be (byteStream(2 + 0x80, 1))
    packU(12857) should be (byteStream(57 + 0x80, 100))

    packS(2) should be (byteStream(2))
    packS(-2) should be (byteStream(0x7e))
    packS(127) should be (byteStream(127 + 0x80, 0))
    packS(-127) should be (byteStream(1 + 0x80, 0x7f))
    packS(128) should be (byteStream(0 + 0x80, 1))
    packS(-128) should be (byteStream(0 + 0x80, 0x7f))
    packS(129) should be (byteStream(1 + 0x80, 1))
    packS(-129) should be (byteStream(0x7f + 0x80, 0x7e))
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

  "signed(1)" should "be one byte" in {
    Signed.pack(32, 1) should be (byteStream(0x01))
  }
}

