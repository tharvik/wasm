package scalawasm.ast

import scalawasm.ast.{Type => AT}

sealed trait Section
object Section {
  object Enum {
    final case object Type extends Section
    final case object Import extends Section
    final case object Function extends Section
    final case object Table extends Section
    final case object Memory extends Section
    final case object Global extends Section
    final case object Export extends Section
    final case object Start extends Section
    final case object Element extends Section
    final case object Code extends Section
    final case object Data extends Section
  }

  // TODO case class Custom(name: String) extends Section(0x00, Some(name))
  final case class Type(entries: Seq[AT.Function]) extends Section(1)
  final case class Import(entries: Seq[Content.import_entry]) extends Section(2)
  final case class Function(indices: Seq[Int]) extends Section(3)
  final case class Table(entries: Seq[AT.Table]) extends Section(4)
  final case class Memory(entries: Seq[AT.Memory]) extends Section(5) {
    require(entries.size <= 1)
  }
  final case class Global(globals: Seq[Content.global_variable]) extends Section(6)
  final case class Export(field: String, kind: AT.external_kind, index: Int) extends Section(7)
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
    final case class local_entry(count: Int, tpe: AT.Trait.Value)

    // FIXME temporary to document correctly
    sealed trait init_expr
    final case class global_variable(varType: AT.Global, init: init_expr)

    sealed trait import_entry {
      def module: String
      def field: String
    }

    // TODO auto-discovery of index
    final case class import_entry_function(module: String, field: String, funcTypeIndex: Int) extends import_entry
    final case class import_entry_table(module: String, field: String, importType: AT.Table) extends import_entry
    final case class import_entry_memory(module: String, field: String, importType: AT.Memory) extends import_entry
    final case class import_entry_global(module: String, field: String, importType: AT.Global) extends import_entry
  }
}
