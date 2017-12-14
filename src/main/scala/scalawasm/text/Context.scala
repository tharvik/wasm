//package scalawasm.text

/*case class Context(names: Map[String, Long]) {
  def + (n: String): Context =
    Context(names + ((n, names.size)))
}*/

/*trait Context[T] {
  val names: Map[String, Long]
  val elem: T
}

object Context {
  case class Add[T](elem: T, c: Context[Any]) extends Context[T] {
    val names = c.names

    def +(n: String): Context[T] =
      Add[T](elem, Names(names + ((n, names.size))))
  }

  case class Names(names: Map[String, Long]) extends Context[Any] {
    val elem = ???
  }
  val empty = Names(Map.empty)
}*/

//case class Context(types: Set[String])

//object Context {
  //val empty = Context(Set.empty)
//}
