### Run the application
mvn -pl bff-spring-projs/spring.oidc.bff spring-boot:run -P berun -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET]


Visit http://localhost:8081/   

<img src="images/angular_home.png" alt="The angular bff app" width="400" />
Few quick checks.  
Press the Invoke . check post button.  
 
<img src="images/angular_checkpost.png" alt="The angular bff app" width="400" />

After Show Inaccessible and Pressing all buttons.   
<img src="images/angular_allbuttons.png" alt="The angular bff app" width="400" />

Lets look at the Sign In button now and click it.    


<img src="images/login.png" alt="The angular bff app" width="400" />

Lets look at the Sign In button now and click it.    

<img src="images/password.png" alt="The angular bff app" width="400" />


<img src="images/okta-verify.png" alt="The angular bff app" width="400" />

You can force the scope slection as next screen if needed by appending "&prompt=consent"  in the url.   
Scope selection screen may show up first time automatically and this appending need not be done.  

<img src="images/scopes_selection.png" alt="The angular bff app" width="400" />

Lets deselect bar from the scopes.  
<img src="images/scopes_selected.png" alt="The angular bff app" width="400" />

Post login   
<img src="images/angular_post_login.png" alt="The angular bff app" width="400" />

Lets click show Inaccessible and click all the buttons there:  

<img src="images/angular_postlogin_showall.png" alt="The angular bff app" width="400" />