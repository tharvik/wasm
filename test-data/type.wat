(module
  (type (func))
  (type $t (func))

  (type (func (param i32)))
  (type (func (param $x i32)))
  (type (func (result i32)))
  (type (func (param i32) (result i32)))
  (type (func (param $x i32) (result i32)))

  (type (func (param f32 f64)))

  (type (func (param f32) (param f64)))
  (type (func (param $x f32) (param f64)))
  (type (func (param f32) (param $y f64)))
  (type (func (param $x f32) (param $y f64)))

  (type (func (param f32 f64) (param $x i32) (param f64 i32 i32)))

  (type (func (param) (param $x f32) (param) (param) (param f64 i32) (param)))
)
