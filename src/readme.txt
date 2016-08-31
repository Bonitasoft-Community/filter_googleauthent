
How to set the filter
--------------------------
See the PDF documentation 
 
1. Principle
-------------
 * on login page, some Javascript to capture the Google Sign On
 * on the filter, use the GoogleIdToken to get the user name, then log in the Bonita server
 
2. Main step for the filter
---------------------------
	 register the filter in Tomcat
	To register the filter in Tomcat, edit the file <TOMCAT>/webapps/bonita/WEB-INF/web.xml

	<filter>
		<filter-name>GoogleFilter</filter-name>
		<filter-class>com.bonitasoft.googleauthent.FilterGoogle</filter-class>	
		<init-param>
      		<param-name>googleServerClientId</param-name>
      		<param-value>494358836642-hnu0gufcrur2tupb2cq4cgigc51l3g00.apps.googleusercontent.com</param-value>
    	</init-param>   
	
	
		<init-param>
      		<param-name>technicalsUsers</param-name>
      		<param-value>user|patrick.gardenier,user|walter.bates,group|/acme/sales,role|qualityManager,profile|t3</param-value>
    	</init-param>  

		<init-param>
      		<param-name>technicalLoginPassword</param-name>
      		<param-value>walter.bates/bpm</param-value>
    	</init-param>   
		
		<init-param>
      		<param-name>acceptClassicalLogin</param-name>
      		<param-value>true</param-value>
    	</init-param>   
	
		<init-param>
      		<param-name>bonitaPassword</param-name>
      		<param-value>bpm</param-value>
    	</init-param>   
		
		<init-param>
      		<param-name>log</param-name>
      		<param-value>true</param-value>
    	</init-param>   
		<init-param>
      		<param-name>ping</param-name>
      		<param-value>true</param-value>
    	</init-param>   
	</filter>

	
	<filter-mapping>
		<filter-name>GoogleFilter</filter-name>
		<url-pattern>/portal/*</url-pattern>
	</filter-mapping>
	<filter-mapping>
		<filter-name>GoogleFilter</filter-name>
		<url-pattern>/loginservice</url-pattern>
	</filter-mapping>
	<filter-mapping>
		<filter-name>GoogleFilter</filter-name>
		<url-pattern>/login.jsp</url-pattern>
	</filter-mapping>
	
    
-----------------------------------
3.	Change the login page

You can create your own login page, or modify the Bonitasoft page.
Note: if you change the Bonitasoft page, you should apply the change after each Bonita Upgrade.
In the login Page, add just BEFORE the </head> balise (you have to place here your KEY)
<meta name="google-signin-client_id" content="HERE_THE_CLIENT_KEY">
<script src="https://apis.google.com/js/platform.js" async defer></script> 
<script>
function onSignIn(googleUser) {
  var form = $('#LoginForm');
  var actionUrlElement = form.attr('action');
  var profile = googleUser.getBasicProfile();
  console.log('ID              : ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
  console.log('Name            : ' + profile.getName());
  console.log('Image URL       : ' + profile.getImageUrl());
  console.log('Email           : ' + profile.getEmail());
  console.log('actionUrlElement: ' + actionUrlElement);
  var id_token = googleUser.getAuthResponse().id_token;
  console.log("ID Token: " + id_token);
	
  /*  .get('../bonita/portal/homepage?idtokengoogle='+id_token,function(data,status) { alert("get it"); }) */
  var dataLogin={}
  dataLogin.idtokengoogle = googleUser.getAuthResponse().id_token;
  dataLogin.namegoogle=profile.getName();
  if (actionUrlElement == null)
  {
	
	$.post("../bonita/portal/homepage", dataLogin, function(result){
		window.location.replace('../bonita');
	});
   }
  else {
	redirectUrl="../bonita";
	var redirectUrlPos = actionUrlElement.indexOf("redirectUrl=");
	if (redirectUrlPos!=-1)
	{
		redirectUrl = actionUrlElement.substring(redirectUrlPos+"redirectUrl=".length );
		// string is like /bonita/portal.... so the final should be ../bonita 
		redirectUrl = ".."+decodeURIComponent( redirectUrl );
	}
		
	$.post(actionUrlElement, dataLogin, function(result){
		window.location.replace(redirectUrl);
	} );
	}
  
}
</script>
And then in the page, add 
	<div class="g-signin2" data-onsuccess="onSignIn"></div>
For example, we add it just after the Login button

