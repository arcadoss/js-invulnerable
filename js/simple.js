
global = 10;
var q, w, e = 'str', r = 10;

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

while (global > 10) {
  global -= 1
}

do {
  gobal += 1
} while (global < 0)

while (True) {
  global += 1
}

switch (global) {
  case 10:
    a = 2;
    break;
  case 'asdf':
    b = 3;
    break;
  default:
    break;
}
