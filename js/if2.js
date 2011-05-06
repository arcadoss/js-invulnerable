var a = 0;

function foo() {
  return 1;
}

a.b = 10;

if (foo()) {
  delete a.b;
}

