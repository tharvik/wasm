package scalawasm.binary

import LEB128.{Signed => S}
import LEB128.{Unsigned => U}

object Decompiler {

  type SB = Seq[Byte]
  type Checker = SB => SB

  private def out(v: SB, msg: String): Unit = {
    val hex = v.map(b => f"$b%02x").mkString(" ")

    println(f"$hex%20s\t$msg")
  }

  private def out(msg: String): Unit = println((" " * 20) + s"\t$msg")

  object Read {
    type Log = String => Unit
    type Op[V] = (Log, V, SB) => SB

    private def ValidSize(s: SB): Int = s.takeWhile(b => (b & 0x80) != 0).size + 1
    private def Var[V](toValue: SB => V)(s: SB)(op: Op[V]): SB = {
      val (bytes, tail) = s.splitAt(ValidSize(s))

      op({ m: String => out(bytes, m) }, toValue(bytes), tail)
    }


    def VarU(s: SB)(op: Op[Long]): SB = Var[Long](U.unpack)(s)(op)
    def VarS(s: SB)(op: Op[Double]): SB = Var[Double](S.unpack)(s)(op)
    def Uint(s: SB, numberOfBytes: Int)(op: Op[Long]): SB = Bytes(s, numberOfBytes) { (log, bytes, tail) =>
      val v: Long = bytes.zipWithIndex.map { case (b: Byte, i: Int) => b.toLong << (i * 8) }.reduce { _|_ }
      op(log, v, tail)
    }
    def Bytes(s: SB, numberOfBytes: Int)(op: Op[Seq[Byte]]): SB = {
      val (bytes, tail) = s.splitAt(numberOfBytes)

      op({ m: String => out(bytes, m) }, bytes, tail)
    }

    private def loop(count: Long, s: SB, op: SB => SB): SB = count match {
      case 0 => s
      case _ => loop(count - 1, op(s), op)
    }
    def Vector(s: SB, id: String)(op: SB => SB): SB =
      Read.VarU(s) { (log, count, tail) =>
        log(s"$id count = $count")
        loop(count, tail, op)
      }

    def WithBytesLength(s: SB, id: String)(op: Op[Seq[Byte]]): SB =
      Read.VarU(s) { (log, length, tail) =>
        log(s"$id length")
        Read.Bytes(tail, length.toInt)(op)
      }
  }

  implicit def SimplifyUnit(t: (Unit, SB)): SB = t._2

  private def magic(s: SB): SB =
    Read.Uint(s, 4) { (log, magic, tail) =>
      log("magic")
      assert(magic == 0x6d736100)

      tail
    }

  private def version(s: SB): SB =
    Read.Uint(s, 4) { (log, version, tail) =>
      log(s"version = $version")
      assert(version == 1)

      tail
    }

  object Type {
    def Func(s: SB): SB =
      Read.VarS(s) { (log, form, tail) =>
        log(s"function form")
        assert(form == -0x20)

        val after = Read.Vector(tail, "param")(Value)
        Read.Vector(after, "return")(Value)
      }

    def Value(s: SB): SB = Read.VarS(s) { (log, v, tail) =>
      val id: String = v match {
        case -0x01 => "i32"
        case -0x02 => "i64"
        case -0x03 => "f32"
        case -0x04 => "f64"
      }
      log(s"value type = $id")

      tail
    }
  }

  object Section {
    def Type(s: SB): SB = Read.Vector(s, "func_type")(Decompiler.Type.Func)
    def Import(s: SB): SB = Read.Vector(s, "import_entry")(Import.Entry)
    object Import {
      def Entry(s: SB): SB = ???
    }
  }

  private def section(s: SB): SB =
    Read.VarU(s) { (log, id, tail) =>
      val (idName, section): (String, Checker) = id match {
        case 1 => ("Type", Section.Type)
        case 2 => ("Import", Section.Import)
      }
      log(s"id = $idName")

      // TODO if id == 0, add name support

      Read.VarU(tail) { (log, totalPayloadSize, tail) =>
        log(s"total payload size = $totalPayloadSize")

        Read.Bytes(tail, totalPayloadSize toInt) { (log, payload, _) =>
          section(payload)
        }
      }
    }

    /*

      val (name_len_bytes, tail3) = Read.Var(tail2)
      val name_len = U.unpack(name_len_bytes).toInt
      out(name_len_bytes, s"name = $name_len")

      val (name_bytes, tail4) = tail3.splitAt(name_len)
      val name = new String(name_bytes.toArray, "UTF-8")
      out(name_bytes, s"name = $name")

      tail4
    } else {
      tail2
    }

    val (payload, tail) = tail5.splitAt(payload_size)

    tail*/

  def check(s: SB): Unit = {
    // TODO add support for multi sections
    val ret = (magic _).andThen(version).andThen(section).apply(s)
    assert(ret.isEmpty, ret.toList)
  }
}
