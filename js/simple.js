
global = 10;

function fun1 (a, b) {
  var local = a;
  global = b;
  return;
}

fun1(10, 2);
