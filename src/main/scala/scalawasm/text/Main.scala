package scalawasm.text

import java.io.File
import java.nio.file._

import scala.io.Source.fromFile

object Main extends App {

  def pipe(text: String): Either[ParsingError, Stream[Byte]] = for {
      l <- Lexer(text)
      ast <- Parser(l)
      p <- ToBinaryAst(ast)
    } yield ToBinary(p)

    args foreach { filename =>
      val in = new File(filename)
      val content = fromFile(in).mkString
    pipe(content) match {
      case Right(bytes) =>
        val path = Paths.get(s"${in.getName}.wasm")
        println(bytes.toList)
        Files.write(path, bytes.toArray)
      case Left(msg) =>
        println(s"error wasming $filename: $msg")
    }
  }
}
