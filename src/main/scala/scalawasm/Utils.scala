package scalawasm

import scalawasm.binary.Alias._

object Utils {
  def packToUint32(i: Int): uint32 = (
    (i >> (0 * 8)).toByte,
    (i >> (1 * 8)).toByte,
    (i >> (2 * 8)).toByte,
    (i >> (3 * 8)).toByte
  )

  def uint32ToStream(i: uint32): Stream[Byte] =
    i._1 #:: i._2 #:: i._3 #:: i._4 #:: Stream.empty

  def neededBytesFor(i: Int): Int = {
    if (i == 0) 0
    else 1 + neededBytesFor(i >> 8)
  }
}
