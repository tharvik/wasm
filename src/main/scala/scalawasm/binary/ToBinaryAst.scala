package scalawasm.binary

import scala.annotation.tailrec
import scalawasm.ast.Binary.{Section => BSec, Signature => BSig}
import scalawasm.ast.Tree.{Signature => TSig}
import scalawasm.ast.{Binary => B, Tree => T}
import scalawasm.{ast => A}
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

  trait BasicSpace[T, A] {
    val seq: Seq[T]
    val tr: A => T
    type implType
    val impl: Seq[T] => implType

    def apply(a: A): Int = {
      val ret = seq.indexOf(tr(a))
      assert(ret >= 0)
      ret
    }

    def +(t: T): implType = impl(seq :+ t)
  }

  trait OptStringBasicSpace extends BasicSpace[Option[String], String] {
    override val tr = Some(_)
  }

  case class TypeSpace(seq: Seq[BSig.Function] = Seq.empty, map: Map[String, Int] = Map.empty) {
    def apply(sig: BSig.Function): Int = {
      val ret = seq.indexOf(sig)
      assert(ret >= 0)
      ret
    }

    def +(name: Option[String], sig: BSig.Function): TypeSpace = {
      val newMap = name.fold(map) {n => map + (n -> seq.size) }
      val newSeq = if (!seq.contains(sig)) seq :+ sig else seq
      TypeSpace(newSeq, newMap)
    }
  }

  case class FunctionSpace(seq: Seq[Option[String]] = Seq.empty) extends OptStringBasicSpace {
    override val impl = FunctionSpace
    override type implType = FunctionSpace
  }

  case class GlobalSpace(seq: Seq[Option[String]] = Seq.empty) extends OptStringBasicSpace {
    override val impl = GlobalSpace
    override type implType = GlobalSpace
  }

  case class MemorySpace(seq: Seq[Option[String]] = Seq.empty) extends OptStringBasicSpace {
    override val impl = MemorySpace
    override type implType = MemorySpace
  }

  case class TableSpace(seq: Seq[Option[String]] = Seq.empty) extends OptStringBasicSpace {
    override val impl = TableSpace
    override type implType = TableSpace
  }

  case class Spaces(types: TypeSpace, funcs: FunctionSpace, globals: GlobalSpace, memories: MemorySpace, tables: TableSpace)
  object Spaces {
    val empty = Spaces(TypeSpace(), FunctionSpace(), GlobalSpace(), MemorySpace(), TableSpace())
  }

  object Signature {
    def Global(g: TSig.Global): BSig.Global = BSig.Global(g.type_, g.mutable)

    def Function(t: TSig.Function): BSig.Function =
      BSig.Function(
        t.params.map(_.type_),
        t.results,
      )
  }

  private def extractNames[A](itemsAndNames: Seq[(A, Option[String])]): (Seq[A], Seq[String]) = {
    val names = itemsAndNames.filter { case (_, o) => o.isDefined } map { case (_, o) => o.get }
    val items = itemsAndNames.filter { case (_, o) =>
      Config.enableSpecCompat || o.isDefined } map { case (f, _) => f }

    (items, names)
  }

  object Section {
    def Type(types: Seq[T.TypeDef]): TypeSpace = {
      val newSeq = types.map(t => Signature.Function(t.sig))
      val newMap = types.zipWithIndex.flatMap { case (t, i) => t.name.map((_, i))}.toMap
      TypeSpace(newSeq, newMap)
    }

    // improvement on stdlib
    def distinct[T, U](seq: Seq[T])(extractor: T => U): Seq[T] = {
      @tailrec
      def loop(cur: Seq[T], ret: Seq[T]): Seq[T] = cur match {
          case Nil =>
            assert(ret.size <= seq.size)
            ret
          case head :: tail =>
            loop(tail,
              if (ret.map(extractor).contains(extractor(head)))
                ret
              else
                ret :+ head)
        }
      loop(seq, Nil)
    }

    def Import(imports: Seq[T.Import], spaces: Spaces): (Seq[BSig.Import], Spaces) = {
      val sigs = imports.flatMap {
        case T.Import.Function(_, _, _, typeref, sig) =>
          typeref.map(_ => None).getOrElse(Some(Signature.Function(sig)))
        case _ => None
      }

      val spacesWithSigs = sigs.distinct.foldLeft(spaces) { case (s, sig) =>
        s.copy(types = s.types + (None, sig))
      }

      imports.foldLeft((Seq[BSig.Import](), spacesWithSigs)) {
        case ((sigs, s: Spaces), i) =>
          val (ret, spaces) = ImportEntry(i, s)
          (sigs :+ ret, spaces)
      }
    }

    private def ImportEntry(imp: T.Import, spaces: Spaces): (BSig.Import, Spaces) = {
      import BSig.{Import => BI}

      imp match {
        case T.Import.Function(_, _, name, typeref, sig) =>
          val index: Int = typeref.map { ref =>
            ref.id match {
              case Left(id) => id toInt // TODO toInt
              case Right(n) => spaces.types.map(n)
            }
          }.getOrElse(spaces.types(Signature.Function(sig)))
          assert(index >= 0)
          (BI.Function(
            imp.module,
            imp.field,
            index),
            spaces) // no need to add new, it already done
        case T.Import.Table(_, _, name, TSig.Table(rl, t)) =>
          (BI.Table(
            imp.module,
            imp.field,
            BSig.Table(t, rl)),
            spaces.copy(tables = spaces.tables + name))
        case T.Import.Memory(_, _, name, sig) =>
          (BI.Memory(
            imp.module,
            imp.field,
            BSig.Memory(sig.resizableLimits)),
            spaces.copy(memories = spaces.memories + name))
        case T.Import.Global(_, _, name, sig) =>
          (BI.Global(
            imp.module,
            imp.field,
            BSig.Global(sig.type_, sig.mutable)),
            spaces.copy(globals = spaces.globals + name))
      }
    }

    def Function(funcs: Seq[T.Function], spaces: Spaces): (Seq[BSig.Function], Spaces) =
      funcs.foldLeft((Seq[BSig.Function](), spaces)) { case ((seq, s), f) =>
        (seq :+ Signature.Function(f.sig),
          s.copy(types = s.types + (f.name, Signature.Function(f.sig)), funcs = s.funcs + f.name)) }

    def Global(globals: Seq[T.Global], spaces: Spaces): (Seq[(BSig.Global, Seq[B.Opcode])], Spaces) =
    globals.foldLeft((Seq[(BSig.Global, Seq[B.Opcode])](), spaces)) { case ((seq, s), g) =>
      (seq :+ (BSig.Global(g.sig.type_, g.sig.mutable), expr(g.instrs)),
        s.copy(globals = s.globals + g.name))}

    def Export(exports: Seq[T.Export], spaces: Spaces): Seq[(String, A.Kind, Int)] = exports.map { e =>
      val index: Int =
        e.var_.id.map { name =>
          e.kind match {
            case A.Kind.Function => spaces.funcs(name) toLong
          }
        }.merge.toInt // TODO toInt
      (e.field, e.kind, index)
    }
  }

  private def expr(e: Seq[T.Expr]): Seq[B.Opcode] = e.map(op)
  private def op(e: T.Expr): B.Opcode = {
    import T.{Opcode => TO}
    import B.{Opcode => BO}

    e match {
      case TO.Unreachable => BO.Unreachable
      // TODO add others
      case TO.Const(t, v) => BO.Const(t, v)
    }
  }

  private def getSections(m: T.Module): Seq[B.Section] = {
    val ts = Section.Type(m.typedefs)
    val typeSpaces = Spaces.empty.copy(types = ts)
    val (imports, typeAndImportSpaces) = Section.Import(m.imports, typeSpaces)
    val (funcs, typeImportAndFuncSpaces) = Section.Function(m.funcs, typeAndImportSpaces)
    val (globals, typeImportFuncAndGlobalsSpaces) = Section.Global(m.globals, typeImportAndFuncSpaces)
    val exports = Section.Export(m.exports, typeImportFuncAndGlobalsSpaces) // TODO can't export every spaces

    Seq(
      BSec.Type(typeImportFuncAndGlobalsSpaces.types.seq),
      BSec.Import(imports),
      BSec.Function(funcs.map(typeImportAndFuncSpaces.types(_))),
      // TODO add more
      BSec.Global(globals),
      BSec.Export(exports),
    )
  }

  def apply(m: T.Module): Either[ParsingError, B.Preamble] = Right(B.Preamble(getSections(m)))
}
