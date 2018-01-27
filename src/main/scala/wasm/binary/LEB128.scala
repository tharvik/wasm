package wasm.binary

import scala.language.postfixOps
import scala.math.{ceil, pow}

/** Implement Little Endian Base 128.
  *
  * Used internally to encode int in wasm.
  *
  * https://en.wikipedia.org/wiki/LEB128
  *
  * There is two version of LEB128, signed and unsigned, both implemented here, each contains a `pack` and an `unpack`
  * method.
  * There is some wasm type helpers in [[LEB128.Type]].
  */
object LEB128 {

  /** Signed version of LEB128. */
  object Signed {
    def pack(value: Long): Seq[Byte] = {
      def signBitIsSet(v: Byte): Boolean = (v & 0x40) != 0

      def rec(v: Long, more: Boolean): Seq[Byte] = if (!more) Seq.empty else {
        val byte = (v & 0x7f).toByte
        val nextV = v >> 7

        val nextMore = !((nextV == 0 && !signBitIsSet(byte)) || (nextV == -1 && signBitIsSet(byte)))

        val toEmit = if (!nextMore) byte else (byte | 0x80).toByte

        toEmit +: rec(nextV, nextMore)
      }

      val bytes: Seq[Byte] = rec(value, more = true)
      if (bytes.nonEmpty) bytes else Seq(0x00)
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

  /** Unsigned version of LEB128. */
  object Unsigned {
    def pack(value: Long): Seq[Byte] = {
      assert(value >= 0)

      def rec(v: Long): Seq[Byte] = {
        val byte = (v & 0x7f).toByte
        val nextV = v >> 7

        val toEmit = if (nextV != 0) (byte | 0x80).toByte else byte

        toEmit +: (if (nextV != 0) rec(nextV) else Seq.empty)
      }

      rec(value)
    }

    def unpack(stream: Seq[Byte]): Long =
      (Stream.from(0, 7) zip stream) map { case (shift, e) =>
        (e & 0x7f).toLong << shift
      } reduce { _ | _ }
  }

  /** wasm int helpers.
    *
    * To be closer to the spec documentation, we use the same names.
    */
  object Type {
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

    /** Fixed size int. */
    case class uint(numberOfBits: Int, value: Long) {
      val pack: Stream[Byte] =
        0 until (numberOfBits / 8) map {
          i => (value >> (i * 8)) toByte
        } toStream
    }

    // TODO quirk
    /** Encode as the spec does, which is invalid LEB128 but still unpack'able. */
    def varuint32CompatPack(value: Long): Stream[Byte] =
      ((0 to 3).map { i: Int => (((value >> (i * 7)) & 0x7F) | 0x80).toByte } :+ 0x00.toByte).toStream

    /** Variable size unsigned int, of max `size` bits. */
    case class varuint(size: Int, value: Long) {
      require(0 <= value && value <= pow(2, size) - 1)

      val pack: Stream[Byte] = LEB128.Unsigned.pack(value) toStream
    }

    /** Variable size signed int, of max `size` bits. */
    case class varint(size: Int, value: Long) {
      require(-pow(2, size - 1) <= value && value <= pow(2, size - 1) - 1)

      val pack: Stream[Byte] = LEB128.Signed.pack(value) toStream
    }
  }
}
