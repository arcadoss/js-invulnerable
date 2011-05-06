
var a = 10;
var b = 'str';
var c = true;

function f() {

  c = new Date();

  function f1() {
  d = new Date();
  print(arguments.toString())
    print(a);
    print(b);
    print(c);
    print(d);
  }

  f2 = function () {
    print(a);
    print(b);
    print(c);
    print(d);
  }

  f1();
  f2();
}


f();
