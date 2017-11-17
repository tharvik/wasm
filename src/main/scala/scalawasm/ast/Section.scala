package scalawasm.ast

import scalawasm.{ast => A}
import scalawasm.ast.Trait.Term

abstract class Section(val id: Short, val name: Option[String] = None) extends Term {
  require((id == 0 && name.isDefined) || (id > 0 && name.isEmpty))
}

object Section {
  // TODO case class Custom(name: String) extends Section(0x00, Some(name))
  final case class Type(entries: Seq[A.Type.Function]) extends Section(1)
  final case class Import(entries: Seq[Content.import_entry]) extends Section(2)
  final case class Function(indices: Seq[Int]) extends Section(3)
  final case class Table(entries: Seq[A.Type.Table]) extends Section(4)
  final case class Memory(entries: Seq[A.Type.Memory]) extends Section(5) {
    require(entries.size <= 1)
  }
  final case class Global(globals: Seq[Content.global_variable]) extends Section(6)
  final case class Export(field: String, kind: A.Type.external_kind, index: Int) extends Section(7)
  final case class Start(index: Int) extends Section(8)
  final case class Element(entries: Seq[Content.elem_segment]) extends Section(9)
  // TODO "The count of function declared in the function section and function
  // bodies defined in this section must be the same and the ith declaration
  // corresponds to the ith function body"
  final case class Code(bodies: Seq[Content.function_body]) extends Section(10)
  final case class Data(entries: Seq[Content.data_segment]) extends Section(11)

  // TODO add "name" extra-section

  object Content {
    final case class data_segment(index: Int, offset: init_expr, data: Seq[Opcode])
    final case class elem_segment(offset: init_expr, elems: Seq[Int])
    final case class function_body(locals: Seq[local_entry], code: Seq[Byte])
    final case class local_entry(count: Int, tpe: A.Type.Trait.Value)


    // FIXME temporary to document correctly
    sealed trait init_expr
    final case class global_variable(varType: A.Type.Global, init: init_expr)

    sealed trait import_entry {
      def module: String
      def field: String
    }

    // TODO auto-discovery of index
    final case class import_entry_function(module: String, field: String, funcTypeIndex: Int) extends import_entry
    final case class import_entry_table(module: String, field: String, importType: A.Type.Table) extends import_entry
    final case class import_entry_memory(module: String, field: String, importType: A.Type.Memory) extends import_entry
    final case class import_entry_global(module: String, field: String, importType: A.Type.Global) extends import_entry
  }
}
