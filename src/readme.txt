
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
	
    

