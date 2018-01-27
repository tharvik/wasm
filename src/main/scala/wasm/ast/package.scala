package wasm

/** Provides classes representing AST.
  *
  * The most relevant class is [[ast.Tree]].
  *
  * The normal order of phases is [[ast.Token]] -> [[ast.Tree]] -> [[ast.Binary]].
  */
package object ast {
  case class ResizableLimits(initial: Int, maximum: Option[Int])

  sealed trait Sign
  object Sign {
    final case object Signed extends Sign
    final case object Unsigned extends Sign
  }

  sealed trait Value
  object Value {
    final case class Integral(v: Long) extends Value
    final case class Floating(v: Double) extends Value
  }

  sealed trait Type
  object Type {
    sealed trait Element extends Type
    final case object AnyFunction extends Element

    sealed trait Block extends Type
    case object Empty extends Block

    sealed trait Value extends Block
    final case object i32 extends Value
    final case object i64 extends Value
    final case object f32 extends Value
    final case object f64 extends Value
  }

  trait Kind
  object Kind {
    case object Function extends Kind
    case object Table extends Kind
    case object Memory extends Kind
    case object Global extends Kind
  }
}
