package scalawasm.binary

import scalawasm.ast.Type._
import scalawasm.ast.{Type => AT}
import scalawasm.ast.Type.Trait._
import scalawasm.binary.LEB128.Type._

object Type {
  object Dispatcher {
    def toBinary(v: Value): Stream[Byte] = v match {
      case t: TypeConstructor => Type.toBinary(t.asInstanceOf[TypeConstructor])
    }

    def toBinary(b: Block): Stream[Byte] = b match {
      case t: TypeConstructor => Type.toBinary(t.asInstanceOf[TypeConstructor])
    }

    def toBinary(e: Element): Stream[Byte] = Type.toBinary(e.asInstanceOf[TypeConstructor])
  }

  def toBinary(o: TypeConstructor): Stream[Byte] = varint7(o match {
    case AT.i32 => -0x01
    case AT.i64 => -0x02
    case AT.f32 => -0x03
    case AT.f64 => -0x04
    case AT.anyfunc => -0x10
    case AT.func => -0x20
    case AT.empty_block_type => -0x40
  }) pack

  def toBinary(f: Function): Stream[Byte] =
    toBinary(func) #:::
      varuint32(f.params size).pack #:::
      f.params.flatMap { Dispatcher.toBinary }.toStream #:::
      varuint1(if (f.returnType.isDefined) 1 else 0).pack #:::
      f.returnType.map { Dispatcher.toBinary }.getOrElse(Stream.empty)

  def toBinary(g: Global): Stream[Byte] =
    Dispatcher.toBinary(g.content_type) #::: varuint1(if (g.mutability) 1 else 0).pack

  def toBinary(t: Table): Stream[Byte] =
    Dispatcher.toBinary(t.element_type) #::: toBinary(t.limits)

  def toBinary(m: Memory): Stream[Byte] =
    toBinary(m.limits)

  def toBinary(e: external_kind): Stream[Byte] =
    uint8(e.kind toByte).pack

  def toBinary(r: ResizableLimits): Stream[Byte] = {
    val flags: Short = if (r.maximum.isDefined) 1 else 0
    val maximumStream: Stream[Byte] = r.maximum map { _ => varuint32(1).pack } getOrElse Stream.empty
    varuint1(flags).pack #:::
      varuint32(r.initial).pack #:::
      maximumStream
  }
}
