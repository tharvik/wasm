package scalawasm.binary

import scala.math.ceil

object LEB128 {
  private def numberOfBytes(size: Int): Int = ceil(size / 7) toInt

  private def splitAndExtend(size: Int, value: Long): Seq[Byte] = {
    0 until numberOfBytes(size) map { i =>
      (value >> (i * 7)) toByte
    } reverse
  }

  def packUnsigned(size: Int, value: Long): Seq[Byte] = {
    val bytes = splitAndExtend(size, value)

    (bytes.head +:
      (bytes.tail map { _ | 0x80 toByte })) reverse
  }

  def packSigned(size: Int, value: Long): Seq[Byte] = {
    val bytes = splitAndExtend(size, value)

    ((bytes.head & 0x7F toByte) +:
      (bytes.tail map { _ | 0x80 toByte })) reverse
  }
}
