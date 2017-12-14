package scalawasm.ast

object Binary {
  final case class Preamble(sections: Seq[Section])

  sealed trait Section
  object Section {
    case class Type(types: Seq[Binary.Signature.Function]) extends Section
  }

  object Signature {
    case class Function(params: Seq[Type.Value], returns: Seq[Type.Value])
  }

  trait Type
  object Type {
    case object Function extends Type
    case object AnyFunction extends Type
    case object Empty extends Type
    sealed trait Value extends Type
    object Value {
      case object i32 extends Value
      case object i64 extends Value
      case object f32 extends Value
      case object f64 extends Value
    }
  }
}
