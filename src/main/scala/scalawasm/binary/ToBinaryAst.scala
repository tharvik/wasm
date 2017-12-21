package scalawasm.binary

import scalawasm.ast.Binary.{Section => BSec, Signature => BSig, Type => BT}
import scalawasm.ast.Tree.{Signature => TSig, Type => TT}
import scalawasm.ast.{Binary => B, Tree => T}
import scalawasm.text.ParsingError
import scalawasm.Config

object ToBinaryAst {
  case class NamesFound(funcs: Seq[String] = Seq(),
                        globals: Seq[String] = Seq(),
                        memories: Seq[String] = Seq(),
                        tables: Seq[String] = Seq()) {
    def +(other: NamesFound): NamesFound = NamesFound(
      funcs = funcs ++ other.funcs,
      globals = globals ++ other.globals,
      memories = memories ++ other.memories,
      tables = tables ++ other.tables)
  }

  type Index = Int
  case class Spaces(func: Map[String, Index],
                    global: Map[String, Index],
                    memory: Map[String, Index],
                    table: Map[String, Index])

  object Type {
    def toValue(t: TT.Value): BT.Value = t match {
      case T.Type.i32 => BT.i32
      case T.Type.i64 => BT.i64
      case T.Type.f32 => BT.f32
      case T.Type.f64 => BT.f64
    }

    def toElement(t: TT.Element): BT.Element = t match {
      case TT.AnyFunc => BT.AnyFunction
    }
  }

  private def toResizableLimits(rl: T.ResizableLimits): B.ResizableLimits = B.ResizableLimits(rl.initial, rl.maximum)

  private def toGlobal(g: TSig.Global): BT.Global = BT.Global(Type.toValue(g.type_), g.mutable)

  private def toFunction(t: T.TypeDef): (BT.Function, Option[String]) =
    (BT.Function(
      t.sig.params.map(p => Type.toValue(p.type_)),
      t.sig.results map Type.toValue
    ), t.name)

  private def extractFuncSig(imports: Seq[T.Import]): Seq[TSig.Function] =
    imports.flatMap { i =>
      i.kind match {
        case T.Import.Kind.Function(_, typeref, sig) => typeref.map(_ => Seq()).getOrElse(Seq(sig))
        case _ => Seq()
      }
    }

  private def toImport(imp: T.Import, spaces: Spaces, funcsSig: Map[TSig.Function, Int]): (BSig.Import, NamesFound) = {
    import T.Import.{Kind => TK}
    import BSig.{Import => BI}

    imp.kind match {
      // ref[int] is directly mapped in order of definition, referencing type signature
      case TK.Function(name, typeref, sig) =>
        val index = typeref.map { ref =>
            ref.id match {
              case Left(id) => id toInt // TODO casting..
              case Right(n) => spaces.func(n)
            }
          }.getOrElse(funcsSig(sig))
        (BI.Function(
           imp.module,
           imp.field,
          index),
         NamesFound(funcs = name.toList))
      case TK.Table(name, TSig.Table(rl, t)) =>
        (BI.Table(
           imp.module,
           imp.field,
           BT.Table(
             Type.toElement(t),
             toResizableLimits(rl))),
         NamesFound(globals = name.toList))
      case TK.Memory(name, sig) =>
        (BI.Memory(
          imp.module,
          imp.field,
          BT.Memory(
            toResizableLimits(sig.resizableLimits))),
        NamesFound(memories = name.toList))
      case TK.Global(name, sig) =>
        (BI.Global(
          imp.module,
          imp.field,
          BT.Global(
            Type.toValue(sig.type_),
            sig.mutable)),
          NamesFound(memories = name.toList))
    }
  }

  private def extractNames[A](itemsAndNames: Seq[(A, Option[String])]): (Seq[A], Seq[String]) = {
    val names = itemsAndNames.filter { case (_, o) => o.isDefined } map { case (_, o) => o.get }
    val items = itemsAndNames.filter { case (_, o) =>
      Config.enableSpecCompat || o.isDefined } map { case (f, _) => f }

    (items, names)
  }

  private def getSectionType(types: Seq[T.TypeDef]): (BSec.Type, NamesFound) = {
    val (funcs, names) = extractNames(types.map(toFunction))
    (BSec.Type(funcs), NamesFound(funcs = names))
  }

  private def getSectionImport(imports: Seq[T.Import], spaces: Spaces): (BSec.Import, NamesFound) = {
    val funcSigs = extractFuncSig(imports).zipWithIndex.toMap // TODO order with zip done at two place

    val (ret, seqNames) = imports.map { i => toImport(i, spaces, funcSigs) }.unzip

    val names = seqNames.fold(NamesFound())(_+_)

    (BSec.Import(ret), names)
  }

  private def namesFoundToContext(nf: NamesFound): Spaces =
    Spaces(
      func = nf.funcs.zipWithIndex.toMap,
      global = nf.globals.zipWithIndex.toMap,
      memory = nf.memories.zipWithIndex.toMap,
      table = nf.tables.zipWithIndex.toMap,
    )

  private def getSections(m: T.Module): Seq[B.Section] = {
    val sectionsOnlyGeneratingNames: Seq[(B.Section, NamesFound)] = Seq(
      getSectionType(m.typedefs),
    )
    val namesFound: NamesFound = sectionsOnlyGeneratingNames.map(_._2).fold(NamesFound())(_+_)

    val spaces = namesFoundToContext(namesFound)
    val (imp, importNamesFound) = getSectionImport(m.imports, spaces)

    sectionsOnlyGeneratingNames.map(_._1) :+ imp
  }

  def apply(m: T.Module): Either[ParsingError, B.Preamble] = Right(B.Preamble(getSections(m)))
}
