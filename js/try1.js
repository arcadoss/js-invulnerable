
var j = 0;

function f() {
  print('in function')
  throw new Date();
}

function g() {
  print('in function g')
}

loop : while(j <= 1) {
	 print ('while begin');
         j += 1;
	 try {
	   print ('try begin');
	   f()
	   print ('try end');
	 } catch (e) {
	   // print('before continue');
	   // continue loop;
	   // print('after continue');
	   // print('before call');
	   // g();
	   // print('after call');
	   print('before break');
	   break ;
	   print('after break');
	 } finally {
	   print('finally');
	 }
	 print ('while end');
       }
