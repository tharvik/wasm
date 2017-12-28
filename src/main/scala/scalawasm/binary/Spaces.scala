package scalawasm.binary

import scalawasm.ast.Binary.{Signature => BSig}

case class Spaces(types: Spaces.TypeSpace, funcs: Spaces.FunctionSpace, globals: Spaces.GlobalSpace,
                  memories: Spaces.MemorySpace, tables: Spaces.TableSpace, locals: Spaces.LocalSpace,
                  branches: Spaces.BranchSpace)
object Spaces {
  val empty = Spaces(
    TypeSpace(), FunctionSpace(), GlobalSpace(), MemorySpace(), TableSpace(), LocalSpace(),
    BranchSpace())

  trait BasicSpace[T, A] {
    val seq: Seq[T]
    type implType

    def apply(a: A): Int

    def +(t: T): implType
  }

  trait ForStringBasicSpace[T] extends BasicSpace[T, String] {
    val tr: String => T

    override def apply(a: String): Int = {
      val ret = seq.indexOf(tr(a))
      assert(ret >= 0)
      ret
    }
  }

  trait OptStringBasicSpace extends ForStringBasicSpace[Option[String]] {
    type OS = Option[String]

    val tr = Some(_)
    val impl: Seq[OS] => implType
    val add: (Seq[OS], OS) => Seq[OS]

    override def +(t: OS): implType = impl(add(seq, t))
  }

  trait ForwardOptStringBasicSpace extends OptStringBasicSpace {
    val add = { (s: Seq[OS], o: OS) => s :+ o }
  }

  trait ReverseOptStringBasicSpace extends OptStringBasicSpace {
    val add = { (s: Seq[OS], o: OS) => o +: s }
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

  case class FunctionSpace(seq: Seq[Option[String]] = Seq.empty) extends ForwardOptStringBasicSpace {
    override val impl = FunctionSpace
    override type implType = FunctionSpace
  }

  case class GlobalSpace(seq: Seq[Option[String]] = Seq.empty) extends ForwardOptStringBasicSpace {
    override val impl = GlobalSpace
    override type implType = GlobalSpace
  }

  case class MemorySpace(seq: Seq[Option[String]] = Seq.empty) extends ForwardOptStringBasicSpace {
    override val impl = MemorySpace
    override type implType = MemorySpace
  }

  case class TableSpace(seq: Seq[Option[String]] = Seq.empty) extends ForwardOptStringBasicSpace {
    override val impl = TableSpace
    override type implType = TableSpace
  }

  case class LocalSpace(seq: Seq[Option[String]] = Seq.empty) extends ForwardOptStringBasicSpace {
    override val impl = LocalSpace
    override type implType = LocalSpace
  }

  case class BranchSpace(seq: Seq[Option[String]] = Seq.empty) extends ReverseOptStringBasicSpace {
    override val impl = BranchSpace
    override type implType = BranchSpace
  }
}
