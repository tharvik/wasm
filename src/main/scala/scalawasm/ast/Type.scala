package scalawasm.ast

import scalawasm.ast.Type.Trait._

object Type {
  object Trait {
    sealed trait TypeConstructor
    sealed trait Element
    sealed trait Block
    sealed trait Value extends Block
  }

  final case object i32 extends TypeConstructor with Value
  final case object i64 extends TypeConstructor with Value
  final case object f32 extends TypeConstructor with Value
  final case object f64 extends TypeConstructor with Value
  final case object anyfunc extends TypeConstructor with Element
  final case object func extends TypeConstructor
  final case object empty_block_type extends TypeConstructor with Block

  final case class Function(params: Seq[Value], returnType: Option[Value] = None)
  final case class Global(content_type: Value, mutability: Boolean)
  final case class Table(element_type: Element, limits: ResizableLimits)
  final case class Memory(limits: ResizableLimits)

  sealed trait external_kind
  object external_kind {
    final case object Function extends external_kind
    final case object Table extends external_kind
    final case object Memory extends external_kind
    final case object Global extends external_kind
  }

  final case class ResizableLimits(initial: Int, maximum: Option[Int] = None) {
    require(initial >= 0)
    require(maximum.exists(initial <= _))
  }
}
