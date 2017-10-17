package scalawasm.binary

import scala.math.pow
import scalawasm.{BinaryEncoding, Term}
import scalawasm.binary.LEB128._

object Types {
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

  case class uint(size: Int, value: Long) {
    val pack: Stream[Byte] =
      0 until size map {
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


  sealed trait value_type extends BinaryEncoding

  sealed trait TypeConstructor extends Term {
    require(opcode.size == 7)

    def opcode: varint

    override def toBinary = opcode pack
  }

  final case class i32() extends TypeConstructor with value_type {
    override def opcode = varint7(-0x01)
  }

  final case class i64() extends TypeConstructor with value_type {
    override def opcode = varint7(-0x02)
  }

  final case class f32() extends TypeConstructor with value_type {
    override def opcode = varint7(-0x03)
  }

  final case class f64() extends TypeConstructor with value_type {
    override def opcode = varint7(-0x04)
  }

  final case class anyfunc() extends TypeConstructor {
    override def opcode = varint7(-0x10)
  }

  final case class func() extends TypeConstructor {
    override def opcode = varint7(-0x20)
  }

  final case class empty_block_type() extends TypeConstructor {
    override def opcode = varint7(-0x40)
  }

  final case class block_type(contained: Option[value_type] = None) extends BinaryEncoding {
    override def toBinary = contained map { _.toBinary } getOrElse varint7(-0x40).pack
  }

  final case class elem_type() extends BinaryEncoding {
    override def toBinary = anyfunc() toBinary
  }

  final case class func_type(params: Seq[value_type], returnType: Option[value_type] = None) extends BinaryEncoding {
    override def toBinary = {
      func().toBinary #:::
        varuint32(params size).pack #:::
        params.flatMap { _.toBinary }.toStream #:::
        varuint1(if (returnType.isDefined) 1 else 0).pack #:::
        returnType.map { _.toBinary }.getOrElse(Stream.empty)
    }
  }

  final case class global_type(content_type: value_type, mutability: Boolean) extends BinaryEncoding {
    override def toBinary = content_type.toBinary #::: varuint1(if (mutability) 1 else 0).pack
  }

  final case class table_type(element_type: elem_type, limits: resizable_limits) extends BinaryEncoding {
    override def toBinary = element_type.toBinary #::: limits.toBinary
  }

  final case class memory_type(limits: resizable_limits) extends BinaryEncoding {
    override def toBinary = limits.toBinary
  }

  final case class external_kind(kind: uint) extends BinaryEncoding {
    require(kind.size == 8)
    require(0 until 3 contains kind.value)

    override def toBinary = kind.pack
  }

  object external_kind {
    val Function = external_kind(uint8(0))
    val Table = external_kind(uint8(1))
    val Memory = external_kind(uint8(2))
    val Global = external_kind(uint8(3))
  }

  final case class resizable_limits(initial: Int, maximum: Option[Int] = None) extends BinaryEncoding {
    require(initial >= 0)
    require(maximum.isEmpty || maximum.get >= 0)

    override def toBinary = {
      val flags: Short = if (maximum.isDefined) 1 else 0
      val maximumStream: Stream[Byte] = maximum map { _ => varuint32(1).pack } getOrElse (Stream.empty)

      varuint1(flags).pack #:::
        varuint32(initial).pack #:::
        maximumStream
    }
  }

}
