package scalawasm.binary

import scala.math.ceil

object LEB128 {
  private def numberOfBytes(size: Int): Int = ceil(size / 7.0) toInt

  private def splitAndExtend(size: Int, value: Long): Seq[Byte] = {
    0 until numberOfBytes(size) map { i =>
      (value >> (i * 7)) toByte
    } reverse
  }

  object Signed {
    def pack(size: Int, value: Long): Seq[Byte] = {
      val bytes = splitAndExtend(size, value)

      ((bytes.head & 0x7F toByte) +:
        (bytes.tail map { _ | 0x80 toByte })) reverse
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

      (bytes.head +:
        (bytes.tail map {
          _ | 0x80 toByte
        })) reverse
    }

    def unpack(stream: Seq[Byte]): Long =
      (Stream.from(0, 7) zip stream) map { case (shift, e) =>
        (e & 0x7f).toLong << shift
      } reduce { _ | _ }
  }
}
