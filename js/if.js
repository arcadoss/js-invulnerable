var a = 0;

b = 10;

if (1 > 2) {
  a +=1;
} else {
  b = 'st';
}

var log = require("ringo/logging").getLogger(module.id);
log.info("Hello {}", "world", a, b);

if (2 ) {
  b += 2;
}
