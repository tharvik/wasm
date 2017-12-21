package scalawasm.ast

object Binary {
  final case class Preamble(sections: Seq[Section])

  sealed trait Section
  object Section {
    case class Type(types: Seq[Binary.Type.Function]) extends Section
    case class Import(imports: Seq[Signature.Import]) extends Section
  }

  type TypeIndex = Int

  case class ResizableLimits(initial: Int, maximum: Option[Int])

  object Signature {
    sealed trait Import {
      val module: String
      val field: String
      val kind: Type.Kind
    }
    object Import {
      case class Function(module: String, field: String, index: TypeIndex) extends Import {
        val kind = Type.Kind.Function
      }
      case class Table(module: String, field: String, type_ : Type.Table) extends Import {
        val kind = Type.Kind.Table
      }
      case class Memory(module: String, field: String, type_ : Type.Memory) extends Import {
        val kind = Type.Kind.Memory
      }
      case class Global(module: String, field: String, type_ : Type.Global) extends Import {
        val kind = Type.Kind.Global
      }
    }
  }

  sealed trait Type
  object Type {
    //case object Function extends Type

    sealed trait Element extends Type
    case object AnyFunction extends Element

    sealed trait Block extends Type
    case object Empty extends Block

    sealed trait Value extends Block
    case object i32 extends Value
    case object i64 extends Value
    case object f32 extends Value
    case object f64 extends Value

    case class Function(params: Seq[Type.Value], returns: Seq[Type.Value]) extends Type
    case class Global(content_type: Value, mutability: Boolean) extends Type
    case class Table(elem_type: Element, resizable_limits: ResizableLimits) extends Type
    case class Memory(resizable_limits: ResizableLimits) extends Type

    sealed trait Kind
    object Kind {
      case object Function extends Kind
      case object Table extends Kind
      case object Memory extends Kind
      case object Global extends Kind
    }
  }
}
