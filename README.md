# `wasm`
WebAssembly for Scala

Provide some AST and a way to convert them to wasm binary. There is also a printer for binary wasm.

There is two way to use `wasm`, either as a cli or a library.

# install
It's using `sbt` for building, simply ensure that you have it.

To ensure that it's working, you may want to run `sbt test` prior to using `wasm`.

## cli
It can be used directly from the `sbt` prompt, there is no system-wide binary installation.

## library
To add to the local ivy storage
```sh
sbt publishLocal
```
and add `libraryDependencies += "default" %% "scalawasm" % "0.1"` to your `build.sbt`

# use

## cli
To compile wat file to wasm file
```sh
sbt run compile watfile1 watfile2 ...
```

To print the content of a wasm file
```sh
sbt run print wasmfile1 wasmfile2 ...
```

## library
The main target is to be able to have a library to generate wasm, usable in scala, from a compiler perspective.

The compilation pipeline and data type used at each stage is
```
pipeline         text.Lexer -> text.Parser -> binary.ToBinaryAst -> binary.ToBinary
datatype    String  ->  ast.Token   ->   ast.Tree     ->     ast.Binary   ->   Stream[Byte]
```

The most suitable coding entrypoint is `wasm.ast.Tree`, it provide a convenient, yet nearly binary, tree.

A good code starter is
```scala
for {
  ast <- Module(
    name = Some("My Module"),
    funcs = Seq(
      Function(
        sig = Signature.Function(),
        instrs = Seq(
          Opcode.Nop,
      ))))
  p <- ToBinaryAst(ast)
} yield ToBinary(p)

```

# todo

## short
 * `ToBinaryAst.Section.Export` works only for function, it's trivial to port it to
   supports others
 * `ToBinaryAst.getSections` could use some refactoring, to nicely separate
   which spaces can be used in which section
 * find a good macro system for Scala, to reduce some value duplication in
   `LEB128.Type`

## medium
 * there is structural support for some wasm extension, such has multi return or
   multi memory, but it still have to be defined in spec and fully
   implemented/tested
 * Data section is implemented until `Tree` but not in the remaining pipeline
 * Custom section is not implemented, there is partial structural support for it
 * remove many warning by using dedicated case object instead of having a
   generic way to do it

## long
 * parser is too simple, it support only "regular" wat, but the spec
   documentation is not very precise either, that would need some improvement
   based on the new version of the spec
   having a better parser would lead to very easy testing, as drop-in of files
   would works, we could reuse directly the core testing suite of the spec repo
   (there is still some issues, such as them using some undefined opcode)
   * correctly handle multi lines comments
   * parse the full syntax
   * int/float values are badly handle, it will break on some corner cases,
     also, the parsing is based on naïve understanding (previously undefined);
     see the "toInt" TODOs
   * there is some structural check we can push to parser, such as i32 can't
     load a value bigger that the actual available range
   * order of `module` opcode is either not well defined or wrong
 * `Printer` can be much cooler than that and try to regenerate the wat input
   and having a way to print wasm and also a decompiler
 * support other compilers than the reference one
 * remove quirks used for spec compatibility, but that would require that
   WebAssembly/spec#625 be fixed first; check for `Config.enableSpecCompat`
