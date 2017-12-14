package scalawasm.text

import scala.language.implicitConversions
import scalawasm.text.{Ast => A}
import scalawasm.text.{BinaryAst => B}
import scalawasm.text.BinaryAst.{Type => BT}
import scalawasm.text.BinaryAst.{Section => BSec}
import scalawasm.text.BinaryAst.{Signature => BSig}

object ToBinaryAst {

  /*def resolveName(name: String, pos: Int): Module = {

  }*/

  /*private implicit def foldContext(ctxs: Seq[Context]): Context =
    ctxs.reduce { (a, b) => Context(a.names ++ b.names) }

  //private implicit def contextasd(a: Any, c: Context): Context = ???

*/

  type Context = Set[String]

  private def toValueAndName(p: A.Parameter): (BT.Value, Option[String]) = (toValue(p.type_), p.name)

  private def toValue(t: A.Type): BT.Value = t match {
    case A.Type.i32 => BT.Value.i32
    case A.Type.i64 => BT.Value.i64
    case A.Type.f32 => BT.Value.f32
    case A.Type.f64 => BT.Value.f64
  }

  //final case class Parameter(name: Option[String], type_ : Type) extends BaseTrait

  // None if no name defined as it can't be referenced
  private def toFunction(t: A.TypeDef): (BSig.Function, Option[String]) =
    (BSig.Function(
      t.sig.params.map(p => toValue(p.type_)),
      t.sig.results map toValue
    ), t.name)

  private def getSectionType(types: Seq[A.TypeDef]): (BSec.Type, Context) = {
    val funcsAndNames = types.map { t => toFunction(t) }
    val names = funcsAndNames.filter { case (_, o) => o.isDefined } map { case (_, o) => o.get } toSet

    (BSec.Type(funcsAndNames.map { case (f, _) => f }), names)
  }

  private def getSections(m: A.Module): Seq[B.Section] = {
    val sectionsContainingNames: Seq[(B.Section, Context)] = Seq(
      getSectionType(m.typedefs),
    )
    val names = sectionsContainingNames.map { n => n._2 }
    // TODO getOthers(names) and apply names to resolve
    sectionsContainingNames.map { case (s, _) => s }
  }

  def apply(m: A.Module): Either[ParsingError, B.Preamble] = Right(B.Preamble(getSections(m)))
}
