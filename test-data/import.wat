(module
  (type $forward (func (param i32)))

  (import "spectest" "print" (func (param i32)))
  (import "spectest" "print" (func $print_i32 (param i32)))
  (import "spectest" "print" (func $print_f32 (param f32)))
  (import "spectest" "print" (func $print_f64 (param f64)))
  (import "spectest" "print" (func $print_i32_f32 (param i32 f32)))
  (import "spectest" "print" (func $print_f64_f64 (param f64 f64)))

  (import "spectest" "print" (func (type $forward)))

  (import "spectest" "global" (global i32))

  (import "spectest" "global" (global $x i32))

  (import "spectest" "global" (global f32))
  (import "spectest" "global" (global f64))

  (import "spectest" "table" (table 10 20 anyfunc))

  (import "spectest" "memory" (memory 1 2))
)
