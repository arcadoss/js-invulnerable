
global = 10;

function fun1 (a, b) {
  var local = a;
  global = b;
  a = -b;

  c = (a > 0 ? 10 : "string");
  return c;
}

fun1(10, 2);

this["prop"] = 10;

if (10 < 2) {
  global += 1
}

if (7 > 2) {
  global -= 2
} else {
  this["prop2"] = this["prop"]
}

for (var i = 0; i < 10; ++i) {
  global /= i
}
