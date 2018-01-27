package wasm

import java.io.File
import java.nio.file._

import scala.io.Source.fromFile
import scala.language.postfixOps
import wasm.binary.{Printer, ToBinary, ToBinaryAst}
import wasm.text._

object Main extends App {
  def pipe(text: String) = for {
    l <- Lexer(text)
    ast <- Parser(l)
    p <- ToBinaryAst(ast)
  } yield ToBinary(p)

  def compile(filename: String): Unit = {
    val in = new File(filename)
    val content = fromFile(in).mkString
    pipe(content) match {
      case Right(bytes) =>
        val name = in.getName.split("\\.").init.mkString
        val path = Paths.get(s"$name.wasm")
        Files.write(path, bytes.toArray)
      case Left(msg) =>
        println(s"error wasming $filename: $msg")
    }
  }

  def print(filename: String): Unit = {
    val in = new File(filename)
    val bytes = Files.readAllBytes(Paths get in.getAbsolutePath) toStream

    Printer.check(bytes)
  }

  if (args.length == 0) {
    println("not args defined")
  } else {
    val action = args.head match {
      case "compile" => compile _
      case "print" => print _
    }

    args.tail foreach { filename =>
      action(filename)
    }
  }
}
