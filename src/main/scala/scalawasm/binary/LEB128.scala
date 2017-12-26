package scalawasm.binary

import scala.math.{ceil,pow}

object LEB128 {
  private def numberOfBytes(size: Int): Int = ceil(size / 7.0) toInt

  private def splitAndExtend(size: Int, value: Long): Seq[Byte] = {
    0 until numberOfBytes(size) map { i =>
      (value >> (i * 7) & 0x7F) toByte
    } takeWhile { _ != 0 } reverse
  }

  object Signed {
    def pack(size: Int, value: Long): Seq[Byte] = {
      val bytes = splitAndExtend(size, value)
      val seq = if (bytes.nonEmpty) bytes else Seq(0x00 toByte)

      ((seq.head & 0x7F toByte) +:
        (seq.tail map { _ | 0x80 toByte })) reverse
    }

    def unpack(stream: Seq[Byte]): Long = {
      val size = java.lang.Long.BYTES * 8
      val result = (Stream.from(0, 7) zip stream) map { case (s, e) =>
        (e & 0x7f).toLong << s
      } reduce { _ | _ }

      val shift = stream.size * 7

      if ((shift < size) && (stream.last & 0x40) != 0)
        result | (~0L << shift)
      else
        result

    }
  }

  object Unsigned {
    def pack(size: Int, value: Long): Seq[Byte] = {
      val bytes = splitAndExtend(size, value)
      val seq = if (bytes.nonEmpty) bytes else Seq(0x00 toByte)

      (seq.head +:
        (seq.tail map {
          _ | 0x80 toByte
        })) reverse
    }

    def unpack(stream: Seq[Byte]): Long =
      (Stream.from(0, 7) zip stream) map { case (shift, e) =>
        (e & 0x7f).toLong << shift
      } reduce { _ | _ }
  }

  object Type {
    type uint32 = (Byte, Byte, Byte, Byte)
    type bytes = Seq[Byte]

    // TODO nice generic way?
    def varuint1(value: Short): varuint = varuint(1, value)
    def varuint7(value: Short): varuint = varuint(7, value)
    def varuint32(value: Int): varuint = varuint(32, value)
    def varint7(value: Short): varint = varint(7, value)
    def varint32(value: Int): varint = varint(32, value)
    def varint64(value: Long): varint = varint(64, value)
    def uint8(value: Byte): uint = uint(8, value)
    def uint16(value: Short): uint = uint(16, value)
    def uint32(value: Int): uint = uint(32, value)
    def uint64(value: Long): uint = uint(64, value)

    // TODO not in LEB128
    case class uint(numberOfBits: Int, value: Long) {
      val pack: Stream[Byte] =
        0 until (numberOfBits / 8) map {
          i => (value >> (i * 8)) toByte
        } toStream
    }

    case class varuint(size: Int, value: Long) {
      require(0 <= value && value <= pow(2, size) - 1)

      val pack: Stream[Byte] = LEB128.Unsigned.pack(size, value) toStream
    }

    case class varint(size: Int, value: Long) {
      require(-pow(2, size - 1) <= value && value <= pow(2, size - 1) - 1)

      val pack: Stream[Byte] = LEB128.Signed.pack(size, value) toStream
    }

  }
}
