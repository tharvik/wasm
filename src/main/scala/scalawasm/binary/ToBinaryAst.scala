package scalawasm.binary

import scala.annotation.tailrec
import scalawasm.ast.Binary.{Section => BSec, Signature => BSig}
import scalawasm.ast.Tree.{Signature => TSig}
import scalawasm.ast.{Binary => B, Tree => T}
import scalawasm.{ast => A}
import scalawasm.text.ParsingError

object ToBinaryAst {
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

  case class LocalSpace(seq: Seq[Option[String]] = Seq.empty) extends OptStringBasicSpace {
    override val impl = LocalSpace
    override type implType = LocalSpace
  }

  case class BranchSpace(seq: Seq[Option[String]] = Seq.empty) extends OptStringBasicSpace {
    override val impl = BranchSpace
    override type implType = BranchSpace
  }

  case class Spaces(types: TypeSpace, funcs: FunctionSpace, globals: GlobalSpace, memories: MemorySpace,
                    tables: TableSpace, locals: LocalSpace, branches: BranchSpace)
  object Spaces {
    val empty = Spaces(
      TypeSpace(), FunctionSpace(), GlobalSpace(), MemorySpace(), TableSpace(), LocalSpace(),
      BranchSpace())
  }

  object Signature {
    def Global(g: TSig.Global): BSig.Global = BSig.Global(g.type_, g.mutable)

    def Function(t: TSig.Function): BSig.Function =
      BSig.Function(
        t.params.map(_.type_),
        t.results,
      )
  }

  object Section {
    def runlength[A](seq: Seq[A]): Seq[(Int, A)] = {
      @tailrec
      def loop(rem: Seq[A], ret: Seq[(Int, A)]): Seq[(Int, A)] = rem match {
        case Nil => ret
        case head :: _ =>
          val (front, tail) = rem.span(_ == head)
          loop(tail, ret :+ (front.size, head))
      }
      loop(seq, Seq.empty)
    }

    def Code(funcs: Seq[T.Function], spaces: Spaces): Seq[(Seq[(Int, A.Type.Value)], Seq[B.Opcode])] = {
      assert(spaces.branches.seq.isEmpty)
      funcs.map { f =>
        val newSpace = f.locals.foldLeft(spaces) { case (s, l) =>
            s.copy(locals = s.locals + l.name)
        }
        (runlength(f.locals.map(_.type_)), expr(f.instrs, newSpace))
      }
    }

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
        case ((seq, s: Spaces), i) =>
          val (ret, spaces) = ImportEntry(i, s)
          (seq :+ ret, spaces)
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
            spaces.copy(funcs = spaces.funcs + name)) // only need to add name, types is done with signature
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

    def Global(globals: Seq[T.Global], spaces: Spaces): (Seq[(BSig.Global, Seq[B.Opcode])], Spaces) = {
      assert(spaces.branches.seq.isEmpty)
      assert(spaces.locals.seq.isEmpty)
      globals.foldLeft((Seq[(BSig.Global, Seq[B.Opcode])](), spaces)) { case ((seq, s), g) =>
        (seq :+ (BSig.Global(g.sig.type_, g.sig.mutable), expr(g.instrs, spaces)),
          s.copy(globals = s.globals + g.name))
      }
    }

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

  private def expr(e: Seq[T.Expr], spaces: Spaces): Seq[B.Opcode] = e.flatMap(op(_, spaces)) :+ B.Opcode.End
  private def op(e: T.Expr, spaces: Spaces): Seq[B.Opcode] = {
    import T.{Opcode => TO}
    import B.{Opcode => BO}

    def resolveVar(v: T.Variable, space: String => Long): Int =
      v.id.map(space).merge.toInt // TODO toInt

    implicit def OpToSeq(op: B.Opcode): Seq[B.Opcode] = Seq(op)

    def blockSig(seq: Seq[A.Type.Block]): A.Type.Block =
      seq.headOption.getOrElse(A.Type.Empty)

    e match {
      case TO.Unreachable => BO.Unreachable
      case TO.Nop => BO.Nop
      case T.Expr.Block(name, sig, exprs) =>
        val newSpace = spaces.copy(branches = spaces.branches + name)
        BO.Block(blockSig(sig.results)) +: expr(exprs, newSpace)
      case T.Expr.Loop(name, sig, exprs) =>
        val newSpace = spaces.copy(branches = spaces.branches + name)
        BO.Loop(blockSig(sig)) +: expr(exprs, newSpace)
      case T.Expr.If(name, sig, thn, els) =>
        val newSpace = spaces.copy(branches = spaces.branches + name)
        BO.If(blockSig(sig)) +:
          (expr(thn, newSpace) ++ (
            BO.Else +:
              expr(els, newSpace)))
      case TO.Br(l) => BO.Br(resolveVar(l, spaces.branches(_)))
      case TO.BrIf(l) => BO.BrIf(resolveVar(l, spaces.branches(_)))
      case TO.BrTable(labels, default) => BO.BrTable(
        labels.map(l => resolveVar(l, spaces.branches(_))),
        resolveVar(default, spaces.branches(_)))
      case TO.Return => BO.Return

      case TO.Call(v) => BO.Call(resolveVar(v, spaces.funcs(_)))
      case TO.CallIndirect(sig) => BO.CallIndirect(spaces.types(Signature.Function(sig)))

      case TO.Drop => BO.Drop
      case TO.Select => BO.Select

      case TO.GetLocal(v) => BO.GetLocal(resolveVar(v, spaces.locals(_)))
      case TO.SetLocal(v) => BO.SetLocal(resolveVar(v, spaces.locals(_)))
      case TO.TeeLocal(v) => BO.TeeLocal(resolveVar(v, spaces.locals(_)))
      case TO.GetGlobal(v) => BO.GetGlobal(resolveVar(v, spaces.globals(_)))
      case TO.SetGlobal(v) => BO.SetGlobal(resolveVar(v, spaces.globals(_)))

      case TO.Load(t, ss, off, ali) => BO.Load(t,
        ss.map { case TO.Load.SizeAndSign(size, sign) => BO.Load.SizeAndSign(size, sign) },
        BO.MemoryImmediate(off, ali))
      case TO.Store(t, size, off, ali) => BO.Store(t, size, BO.MemoryImmediate(off, ali))
      case TO.CurrentMemory => BO.CurrentMemory
      case TO.GrowMemory => BO.GrowMemory

      case TO.Const(t, v) => BO.Const(t, v)

      case TO.EqualZero(t) => BO.EqualZero(t)
      case TO.Equal(t) => BO.Equal(t)
      case TO.NotEqual(t) => BO.NotEqual(t)
      case TO.LessThan(t, s) => BO.LessThan(t, s)
      case TO.GreaterThan(t, s) => BO.GreaterThan(t, s)
      case TO.LessOrEqual(t, s) => BO.LessOrEqual(t, s)
      case TO.GreaterOrEqual(t, s) => BO.GreaterOrEqual(t, s)

      case TO.CountLeadingZeros(t) => BO.CountLeadingZeros(t)
      case TO.CountTrailingZeros(t) => BO.CountTrailingZeros(t)
      case TO.CountNumberOneBits(t) => BO.CountNumberOneBits(t)
      case TO.Add(t) => BO.Add(t)
      case TO.Substract(t) => BO.Substract(t)
      case TO.Multiply(t) => BO.Multiply(t)
      case TO.Divide(t, s) => BO.Divide(t, s)
      case TO.Remainder(t, s) => BO.Remainder(t, s)
      case TO.And(t) => BO.And(t)
      case TO.Or(t) => BO.Or(t)
      case TO.Xor(t) => BO.Xor(t)
      case TO.ShiftLeft(t) => BO.ShiftLeft(t)
      case TO.ShiftRight(t, s) => BO.ShiftRight(t, s)
      case TO.RotateLeft(t) => BO.RotateLeft(t)
      case TO.RotateRight(t) => BO.RotateRight(t)
      case TO.Absolute(t) => BO.Absolute(t)
      case TO.Negative(t) => BO.Negative(t)
      case TO.Ceiling(t) => BO.Ceiling(t)
      case TO.Floor(t) => BO.Floor(t)
      case TO.Nearest(t) => BO.Nearest(t)
      case TO.Sqrt(t) => BO.Sqrt(t)
      case TO.Min(t) => BO.Min(t)
      case TO.Max(t) => BO.Max(t)
      case TO.CopySign(t) => BO.CopySign(t)

      case TO.Wrap(from, to) => BO.Wrap(from, to)
      case TO.Truncate(from, to, s) => BO.Truncate(from, to, s)
      case TO.Extend(from, to, s) => BO.Extend(from, to, s)
      case TO.Demote(from, to) => BO.Demote(from, to)
      case TO.Convert(from, to, s) => BO.Convert(from, to, s)
      case TO.Promote(from, to) => BO.Promote(from, to)

      case TO.Reinterpret(from, to) => BO.Reinterpret(from, to)
    }
  }

  private def getSections(m: T.Module): Seq[B.Section] = {
    val ts = Section.Type(m.typedefs)
    val typeSpaces = Spaces.empty.copy(types = ts)
    val (imports, typeAndImportSpaces) = Section.Import(m.imports, typeSpaces)
    val (funcs, typeImportAndFuncSpaces) = Section.Function(m.funcs, typeAndImportSpaces)

    val (globals, typeImportFuncAndGlobalsSpaces) = Section.Global(m.globals, typeImportAndFuncSpaces)
    val exports = Section.Export(m.exports, typeImportFuncAndGlobalsSpaces) // TODO can't export every spaces

    val codes = Section.Code(m.funcs, typeImportFuncAndGlobalsSpaces)

    Seq(
      BSec.Type(typeImportFuncAndGlobalsSpaces.types.seq),
      BSec.Import(imports),
      BSec.Function(funcs.map(typeImportAndFuncSpaces.types(_))),
      // TODO add more
      BSec.Global(globals),
      BSec.Export(exports),
      // TODO add more
      BSec.Code(codes),
    )
  }

  def apply(m: T.Module): Either[ParsingError, B.Preamble] = Right(B.Preamble(getSections(m)))
}
