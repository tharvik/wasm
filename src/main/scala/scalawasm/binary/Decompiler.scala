package scalawasm.binary

import LEB128.{Signed => S}
import LEB128.{Unsigned => U}
import scalawasm.binary.{Decompiler => This}

object Decompiler {

  type SB = Seq[Byte]
  type Checker = SB => SB

  private def out(v: SB, msg: String): Unit = {
    val hex = v.map(b => f"$b%02x").mkString(" ")

    println(f"$hex%30s\t$msg")
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
    def VarS(s: SB)(op: Op[Long]): SB = Var[Long](S.unpack)(s)(op)
    def Uint(s: SB, numberOfBytes: Int)(op: Op[Long]): SB = Bytes(s, numberOfBytes) { (log, bytes, tail) =>
      val v: Long = bytes.zipWithIndex.map { case (b: Byte, i: Int) => b.toLong << (i * 8) }.reduce { _|_ }
      op(log, v, tail)
    }
    def Bytes(s: SB, numberOfBytes: Int)(op: Op[Seq[Byte]]): SB = {
      val (bytes, tail) = s.splitAt(numberOfBytes)

      op({ m: String => out(bytes, m) }, bytes, tail)
    }
    def Byte(s: SB)(op: Op[Byte]): SB = Read.Bytes(s, 1) { (log, bytes, tail) =>
      op(log, bytes.head, tail)
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
        log(s"$id length = $length")
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

    def ExternalKind(s: SB): SB = Read.Byte(s) {(log, b, tail) =>
      val kind: String = b match {
        case 0 => "Function"
        case 1 => "Table"
        case 2 => "Memory"
        case 3 => "Global"
      }
      log(s"kind = $kind")

      tail
    }

    def ResizableLimits(s: SB): SB = Read.VarU(s) { (log, flags, tail) =>
      log(s"maximum field is present = $flags")

      Read.VarU(tail) { (log, initial, tail) =>
        log(s"initial size = $initial")

        if (flags == 0) tail
        else Read.VarU(tail) { (log, maximum, tail) =>
          log(s"maximum size = $maximum")
          tail
        }
      }
    }

    def Elem(s: SB): SB = Read.VarS(s) { (log, t, tail) =>
      log("elem type = anyfunc")
      assert(t == -0x10)
      tail
    }

    def Global(s: SB): SB = {
      val tail = Value(s)
      Read.VarU(tail) { (log, mutability, tail) =>
        log(s"mutable = $mutability")
        assert(mutability == 0 || mutability == 1)
        tail
      }
    }
    def Table(s: SB): SB = {
      val tail = Elem(s)
      ResizableLimits(tail)
    }
    def Memory(s: SB): SB = ResizableLimits(s)
  }

  object Section {
    def Type(s: SB): SB = Read.Vector(s, "func_type")(Decompiler.Type.Func)
    def Function(s: SB): SB = Read.Vector(s, "signature")(Read.VarU(_) { (log, index, tail) =>
      log(s"type $index")
      tail
    })

    def Import(s: SB): SB = Read.Vector(s, "import_entry") { s =>
      Read.WithBytesLength(s, "module") { (log, moduleBytes, tail) =>
        val module = new String(moduleBytes.toArray, "UTF-8")
        log(s"module = $module")

        Read.WithBytesLength(tail, "field") { (log, fieldBytes, tail) =>
          val field = new String(fieldBytes.toArray, "UTF-8")
          log(s"field = $field")

          val followed = Decompiler.Type.ExternalKind(tail)

          tail.head match {
            case 0 => Read.VarU(followed) { (log, index, tail) =>
              log(s"type index = $index")
              tail
            }
            case 1 => Decompiler.Type.Table(followed)
            case 2 => Decompiler.Type.Memory(followed)
            case 3 => Decompiler.Type.Global(followed)
          }
        }
      }
    }

    def expr(s: SB, msg: String): SB = {
      val endOp = 0x0b toByte

      val expr = s.takeWhile(_ != endOp) :+ endOp
      out(expr, s"<$msg expr>") // TODO describe further
      s.dropWhile(_ != endOp).tail
    }

    def Global(s: SB): SB = Read.Vector(s, "global") { s =>
      val tail = Decompiler.Type.Global(s)

      expr(tail, "init")
    }

    def Export(s: SB): SB = Read.Vector(s, "export") { s =>
      Read.WithBytesLength(s, "field") { (log, fieldBytes, tail) =>
        val field = new String(fieldBytes.toArray, "UTF-8")
        log(s"field = $field")

        val rest = Decompiler.Type.ExternalKind(tail)
        Read.VarU(rest) { (log, index, tail) =>
          log(s"index = $index")
          tail
        }
      }
    }

    def Code(s: SB): SB = Read.Vector(s, "function body") { s =>
      Read.WithBytesLength(s, "body") { (_, body, tail) =>
        val code = Read.Vector(body, "local entry") { s =>
          Read.VarU(s) { (log, count, tail) =>
            log(s"count of local with next type = $count")
            This.Type.Value(tail)
          }
        }

        expr(code, "function")
        tail
      }
    }
  }

  private def section(s: SB): SB =
    Read.VarU(s) { (log, id, tail) =>
      val (idName, section): (String, Checker) = id match {
        // TODO if id == 0, add name support
        case 1 => ("Type", Section.Type)
        case 2 => ("Import", Section.Import)
        case 3 => ("Function", Section.Function)

        case 6 => ("Global", Section.Global)
        case 7 => ("Export", Section.Export)

        case 10 => ("Code", Section.Code)
      }
      log(s"id = $idName")

      Read.VarU(tail) { (log, totalPayloadSize, tail) =>
        log(s"total payload size = $totalPayloadSize")

        Read.Bytes(tail, totalPayloadSize toInt) { (_, payload, tail) =>
          val ret = section(payload)
          assert(ret.isEmpty, ret.toList)

          tail
        }
      }
    }

  private def sections(s: SB): Unit = s match {
    case Nil =>
    case _ => sections(section(s))
  }

  def check(s: SB): Unit = {
    (magic _).andThen(version).andThen(sections).apply(s)
  }
}
