
   (function() {
          var stss = document.createElement('script');
	       stss.type = 'text/javascript';
	            stss.async = true;
		         stss.src = 'http://stats.nekapuzer.at/hit/?referer=' +
               escape(document.referrer) +
               '&site=ringojs' +
               '&random=' + (new Date()).getTime();
        var s = document.getElementsByTagName('script')[0];
	     s.parentNode.insertBefore(stss, s);
	        })();
