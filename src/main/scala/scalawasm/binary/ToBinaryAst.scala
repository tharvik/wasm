package scalawasm.binary

import scala.language.implicitConversions
import scalawasm.ast.Binary.{Section => BSec, Signature => BSig, Type => BT}
import scalawasm.ast.{Binary => B, Tree => T}
import scalawasm.text.ParsingError

object ToBinaryAst {

  type Context = Set[String]

  private def toValueAndName(p: T.Parameter): (BT.Value, Option[String]) = (toValue(p.type_), p.name)

  private def toValue(t: T.Type): BT.Value = t match {
    case T.Type.i32 => BT.Value.i32
    case T.Type.i64 => BT.Value.i64
    case T.Type.f32 => BT.Value.f32
    case T.Type.f64 => BT.Value.f64
  }

  private def toFunction(t: T.TypeDef): (BSig.Function, Option[String]) =
    (BSig.Function(
      t.sig.params.map(p => toValue(p.type_)),
      t.sig.results map toValue
    ), t.name)

  private def getSectionType(types: Seq[T.TypeDef]): (BSec.Type, Context) = {
    val funcsAndNames = types.map { t => toFunction(t) }
    val names = funcsAndNames.filter { case (_, o) => o.isDefined } map { case (_, o) => o.get } toSet

    (BSec.Type(funcsAndNames.map { case (f, _) => f }), names)
  }

  private def getSections(m: T.Module): Seq[B.Section] = {
    val sectionsContainingNames: Seq[(B.Section, Context)] = Seq(
      getSectionType(m.typedefs),
    )
    val names = sectionsContainingNames.map { n => n._2 }
    // TODO getOthers(names) and apply names to resolve
    sectionsContainingNames.map { case (s, _) => s }
  }

  def apply(m: T.Module): Either[ParsingError, B.Preamble] = Right(B.Preamble(getSections(m)))
}
