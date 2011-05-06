
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

if (q == w) {
  w += 10
}

for (var i = 0; i < 10; ++i) {
  global /= i
}

while (global > 10) {
  global -= 1
}

do {
  global += 1
} while (global < 0)

switch (global) {
  case q:
  case 10:
    a = 2;
    break;
  case 'asdf':
    b = 3;
    break;
  default:
    break;
}

with (global) {
  a.b = 10;
  a["c"] = 20;
  a[1 + 3] = 30;
}

for (i in global.a) 
  i += 10

for (; ; global.b++) {
}

for (;;) {
}

var output = '';

try {
  fun1(10, 3, 400);
} catch (e ) {
  output = 'array';
} finally {
  output = 'fin';
}

try {
  fun1(10, 3, 400);
} finally {
  output = 'fin';
}

try {
  fun1(10, 3, 400);
  throw new Date("10.03.20");
} catch (e) {
  output = 'fin';
}

var t = r++;
var y = --r;
this.t++;
++this["y"];
