This project is called BFF because it demonstrates Backend-for-Frontend (BFF) security patterns, particularly with OIDC.
It also explores BFF-style integrations in Swagger using Swagger UI’s plugin mechanism—ideas I hope will eventually influence mainstream Swagger UI.



![OpenAPI](https://img.shields.io/badge/OpenAPI-3.x-green?style=flat-square)
![Swagger UI](https://img.shields.io/badge/Swagger-UI-brightgreen?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square)
![Spring Security](https://img.shields.io/badge/Spring%20Security-enabled-brightgreen?style=flat-square)
![OIDC](https://img.shields.io/badge/Security-OIDC-blue?style=flat-square)
![Okta](https://img.shields.io/badge/Security-Okta-blue?style=flat-square)


![Java](https://img.shields.io/badge/Java-24-blue?style=flat-square)
![Angular](https://img.shields.io/badge/Angular-red?style=flat-square)
![Node.js](https://img.shields.io/badge/Node.js-blue?style=flat-square)
![React](https://img.shields.io/badge/React-blue?style=flat-square)

![Maven](https://img.shields.io/badge/Build-Maven-red?style=flat-square)



*React is used for custom Swagger UI extensions to integrate BFF features.*

# Introduction – BFF (Backend for Frontend)

When working with OIDC for web applications in general, the Authorization Code flow is preferred.  
For SPAs such as Angular or React, Authorization Code with PKCE is commonly used.

While PKCE is considered sufficiently secure for SPAs, it is often regarded as weaker than architectures where access tokens never enter the front channel and are kept entirely in the back channel.  
The **Backend for Frontend (BFF)** pattern addresses this by avoiding PKCE altogether and ensuring that sensitive tokens never reach the browser. It also reduces inter-layer security traffic.

This project demonstrates the BFF pattern through two setups:

- An OIDC-based application with an Angular frontend and a Spring Boot (Java) backend  
- A Spring Boot application with an Angular frontend using simpler Spring Security (non-OIDC)

---

# Swagger and BFF

A challenge arises when using Swagger with BFF-based applications.  
Currently, Swagger UI typically works only when it uses the Authorization Code flow with PKCE.

As a result, when optimizing security using BFF, some compromise has traditionally been required for Swagger integration.  
That has been the case until now.

---

# BFF-based Swagger UI – Main Topic

One of the key highlights of these projects is the use of a **BFF-based Swagger UI**.  
This is achieved by combining a custom **BFF Swagger UI plugin** with **springdoc**.   

The implementation leverages Swagger UI’s official plugin and extension mechanisms,  
ensuring that the BFF behavior is integrated cleanly rather than through ad-hoc hacks.   

**To the best of my knowledge, this is the first fully working Swagger UI plugin designed specifically for BFF architectures.It relies purely on Swagger UI’s extension mechanism and does not require proxying, spec transformation, or token handling in the UI.While this project demonstrates the approach using Java and Spring, the plugin itself is backend- and language-agnostic and can be used with any platform that serves Swagger UI (Java, .NET, Python, and others)**  

The Maven build process is slightly time-consuming. This slowness comes from a setup where a specified version of Swagger UI is downloaded and then extended by merging custom Swagger UI extensions. Because of this, and the fact that we are building multiple projects the build process is somewhat slow. 

The build process also demonstrating compatibility with multiple recent Swagger UI versions.

You can think of this project both as:

- a development playground for editors of the Swagger UI BFF plugin (currently myself), and  
- a demonstration of the plugin’s capabilities

That said, anyone reviewing this project should be able to extract the essential ideas and adapt them to:

- a standard Swagger UI setup using a BFF webjar, or  
- a static web folder integrated with Springdoc for end-user applications

Details on how this can be achieved are shared later below.

# CORS and CSRF

The demos are fully compliant with CORS and CSRF security requirements.


# Setup

This section is intended for users who want to build and run the applications
to observe the BFF-based Swagger UI behavior, without modifying the source code.   

Even if you plan to modify the code, it is recommended to follow this setup
at least once to understand the baseline behavior and ensure that the
environment is correctly configured.   

Prerequisites:   

- Maven 3.9 (minimum) in 3.x.  
- Java 24.   
- Spring boot - 
- Springdoc
- swagger ui - 5.30.1 to 5.31.0

## Okta OIDC Setup
*   Manual
    *   [Manual Okta OIDC Setup Steps](okta-setup-4-oidc/manual-okta-oidc-setup.MD)
*   Automated
    *   [Automatic Okta OIDC Setup Steps](okta-setup-4-oidc/auto-okta-oidc-setup.MD)
    


## Actual Project Setup

At the root of the project execute in command prompt the following:

### Full build
"mvn clean package -P fullbuild -Dquickbuild=false".
Like I said this will be a long build. 
There are ways to speed up the build.
Also its a lot easier when using an IDE and using dev mode - 4200/4201 for angular and 3200/3201 for swagger. Will explain more later.

### Run the oidc application
mvn -pl bff-spring-projs/spring.oidc.bff spring-boot:run -P berun -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET]

#### Run the BFF based Angular UI

Visit http://localhost:8081/   

<img src="images/angular_home.png" alt="The angular bff app" width="400" />   
For a quick look at related screen flows including Authorization code flow login have a look at   

 [Quick Checks](./oidc_angular_app.MD) .    


#### Run the BFF based Swagger UI
This is the main part.   
I have leveraged the swagger UI plugin architecture and added this BFF plugin.  


Visit http://localhost:8081/swagger-ui.html


<img src="images/swagger-ui-bff.png" alt="swagger with bff" width="400" /> 


Note the lock symbol. It should show as "locked" if you were signed in when using the angular app.  It would show as "unlocked" if you were not signed in when using the angular app. Can also refresh the page if needed.  

Here I am showing as not yet logged on.  

The APIs are expected to behave same way as we saw in the angular app.   

Before going there lets have a look at the login / logout flow by clicking on the lock symbol(s).  

<img src="images/swagger-ui-bff-redirecting-to-okta.png" alt="swagger with bff" width="400" /> 

If you are quick you might notice this above screen.

<img src="images/swagger-bff-login.png" alt="swagger with bff" width="400" /> 

Remember you can force the scope selection screen if so desired by appending "&prompt=consent" without the quotes into the url and submitting

This is the same okta login screen we saw earlier.

<img src="images/swagger-bff-oktaverify.png" alt="swagger with bff" width="400" /> 

<img src="images/swagger-bff-password.png" alt="swagger with bff" width="400" /> 

The flow is same as the login flow we saw with angular.   

<!-- img src="images/swagger-bff-scope-selection.png" alt="swagger with bff" width="400" / --> 

In case you had appended the "&prompt=consent" without the quotes into the url we mentioned earlier you should also see this scope selection screen.


<img src="images/swagger-bff-scope-selected.png" alt="swagger with bff" width="400" /> 

Showing here how we deselected scope bar.   


<img src="images/swagger-ui-bff-locked.png" alt="swagger with bff" width="400" /> 

Note how the lock symbol is locked now.   

Now that user is logged on lets check what happens if we click the lock symbol(s) again.   


<img src="images/swagger-ui-bff-authorizations.png" alt="swagger with bff" width="400" />

Click logout.   

<img src="images/swagger-ui-bff-unlocked.png" alt="swagger with bff" width="400" />

The locks are now unlocked again.   

Lets relogin.
Dont bother about scope selection screen. It wont show up unless we login first time or force it as explained earlier.


We are now logged on.
Lets test some functionalities.
Foo   
<img src="images/swagger-bff-foo.png" alt="swagger with bff" width="400" />  

Bar  
<img src="images/swagger-bff-bar.png" alt="swagger with bff" width="400" />  

Why was Bar forbidden?   

Remember the scope selection during earlier login.  
Scopes selected are remembered across logins by Okta.  Of course -also our code expected foo api to be available to foo scope and bar api to be available to the bar scope.  

Click on the lock symbol to verify the  scopes if needed.

Now lets test other functionalities quickly.


User  
<img src="images/swagger-bff-user.png" alt="swagger with bff" width="400" />  

Admin  
<img src="images/swagger-bff-admin-forbidden.png" alt="swagger with bff" width="400" />  


Admin API is available on to people who belong to myadmin group.
User API is available on to people who belong to myuser group.

We have demonstrated scopes and roles based security so far.

Lets logout again.  
Now that we have logged out lets try accessing any of the APIs we tried so far when logged in.
Lets try with Foo API
<img src="images/swagger-bff-foo-unloggedon.png" alt="swagger with bff" width="400" />  
We get the expected 401.  
Expect same behaviour in Bar, User and Admin APIs when not logged on.  

Lets now look at some APIs which done need Authentication the ones without the lock symbol.  

Lets try the checkpost API.   
<img src="images/swagger-bff-checkpost.png" alt="swagger with bff" width="400" />

Of Particular interest is the checkpost API not so much as seen now.  
This API helps demonstrate csrf security when running swagger bff in development mode under 3200/3201. Also when running the angular app in development mode under 4200/4201.

Will cover those when we look aspects in a short while further down after discussing SAML and Simple Spring Web Security.

# SAML
The OIDC project and its swagger and angular integration using BFF we demonstrated is exactly the same approach when we use SAML with BFF.  If time permits might later add a SAML demo. But from BFF point of view the concepts are exactly the same.

# Simple Spring Web Security with BFF

To  demistify BFF further this multimodule project also includes a module "bff\bff-spring-projs\spring.simple.bff".

Unlike with above discussed OIDC or SAML in this more traditional project applications are supposed to provide their own login screens.  
One way for this is to leverage spring security basic Authentication or Form Login Screen.   

What we have done however is leverage the swagger ui login screens when in swagger and custom angular login screens when in angular.  This is a valid variation of the BFF pattern and so is included in this demo in same detail as was OIDC.  


### Run the application
mvn -pl bff-spring-projs/spring.simple.bff spring-boot:run -P berun 


API wise this will be similar to the OIDC app.However there will be no concept of Scope. 
Foo and Bar wont be found. Other functionality will be the same with just the difference in how login is done.  
This time the application runs of port 8080.  

#### BFF Angular app


Visit http://localhost:8080/   
<img src="images/simple_angular_home_checkpost.png" alt="swagger with bff" width="400" />   
Showing above the landing page after clicking on checkpost button.  

<img src="images/simple_angular_home_inaccessible.png" alt="swagger with bff" width="400" />  

Showing above the landing page after checking  the "Show Inaccessible" checkbox and pressing all the buttons.

A quick look at the login.   


<img src="images/simple_angular_login.png" alt="swagger with bff" width="400" />  
Thats the custom angular login screen.   

<img src="images/simple_angular_post_login_all_buttons.png" alt="swagger with bff" width="400" /> 

Showing Post login. Ensured "Show Inaccessible" checkbox is checked  all the buttons are pressed.  

Sign out will do the expected.   


#### BFF Swagger UI

I realise this document is getting longer. So will focus on the login and logout process. Will leave it to the reader to try the APIs and verify the expectations.  


<img src="images/simple-swagger-bff-home.png" alt="swagger with bff" width="400" /> 

As can be seen the lock is unlocked because user is not logged on through either application.  
Click the lock(s). 

We can see the swagger Ui BFF login screen.  
<img src="images/simple-swagger-bff-login.png" alt="swagger with bff" width="400" />   

<img src="images/simple-swagger-bff-loggedon.png" alt="swagger with bff" width="400" />   

From here we can either logout or close this dialogue.  

Lets choose close. 

<img src="images/simple-swagger-bff-home-loggedon.png" alt="swagger with bff" width="400" />  

Notice how the locks are locked.  

 
Lets click the lock(s).  

We are back at   
<img src="images/simple-swagger-bff-loggedon.png" alt="swagger with bff" width="400" />  

Lets this time try logout.   


<img src="images/simple-swagger-bff-postlogout.png" alt="swagger with bff" width="400" /> 

From here we can choose to login or close.  

Lets choose close.   

we should be back at   
<img src="images/simple-swagger-bff-home.png" alt="swagger with bff" width="400" />   

Notice how the locks are unlocked.  
Feel free to try out the various functionalities.   



# Dev Setup

This section is intended for developers who want to modify the source code,
including the BFF Swagger UI plugin, frontend assets, or backend behavior.   

It assumes that the basic setup has already been completed and focuses on
development-specific tools, workflows, and build steps required for making
and testing changes.   

Prerequisites: 

- Maven 3.9 (minimum) in 3.x.  
- Java 24.   
- Spring boot - 3.5.9
- Springdoc
- swagger ui - 5.30.1 to 5.31.0
- IDEs


I wont show how to do this with IDES. It should be easy to figure out.
But i will show how this can be done without IDES. 


### Full build
"mvn clean package -P fullbuild -Dquickbuild=false".

Remember how we did the full build.   
Once full build has run through can avoid some time consuming steps by using below
"mvn clean package -P fullbuild -Dquickbuild=false".

Here are a few points.  
By default we prefer explicitly not to specify any heavy activity in the maven build process and thereby choke the IDE.  
Thats why fullbuild steps are in a profile and are run only when needed.   

### Running the apps

#### Run the  BFF server  applications
mvn -pl bff-spring-projs/spring.oidc.bff spring-boot:run -P berun -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET]   
OR  
mvn -pl bff-spring-projs/spring.simple.bff spring-boot:run -P berun 

Usually if you want to make channges in angular or swagger you can specify additional OPTIONAL arguments as shown below

For Angular OIDC BFF app:    
-Dfebaseurl=http://localhost:4201  
Or  
For Angular Simple BFF app:    
-Dfebaseurl=http://localhost:4200  

For Swagger OIDC BFF app:    
-Dswaggeruiurl=http://localhost:3201  
Or  
For Swagger Simple BFF app:    
-Dswaggeruiurl=http://localhost:3200  

In other words you should start the server using:   

mvn -pl bff-spring-projs/spring.oidc.bff spring-boot:run -P berun -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET] -Dfebaseurl=http://localhost:4201 -Dswaggeruiurl=http://localhost:3201 
OR  
mvn -pl bff-spring-projs/spring.simple.bff spring-boot:run -P berun -Dfebaseurl=http://localhost:4200 -Dswaggeruiurl=http://localhost:3200

This will start the back end server and configure the CORS and CSRF correctly.
It will also configure a correct front-end\src\assets\serverenv.json file and its presence or absence in both the BFF angular projects or simple and oidc spring security.  

Now how to launch the UIs?

Note: Do not launch the UIs without launching the back end projects as shown above to avoid runnning into issues related to the front-end\src\assets\serverenv.json file.

#### Launching BFF Angular APPs

Both the projects bff\bff-spring-projs\spring.oidc.bff and bff\bff-spring-projs\spring.simple.bff have a batch file and shell script named 
angularshell.bat  OR
angularshell.sh (I haven't yet used the .sh file. If using the .sh please adjust it as needed for now. )

I am using the batchfile. I launch it by right clicking on it and launching it.

<img src="images/angular_dev_shell.png" alt="swagger with bff" width="400" /> 

This shell has a very controlled environment PATH. 
It uses the node, npm, angular exactly as setup in the pom.xml. 
It does not get influenced by the PATH configured at OS level.

Once in this shell I can use all my node, npm, ng commands.

Showing below what happens when i type npm start on the shells for the two projects.

The angular project for the simple BFF project runs on 4200.   
<img src="images/angular_simple_dev_shell_start.png" alt="angular with bff" width="400" />   
The angular project for the OIDC BFF project runs on 4201.   
<img src="images/angular_oidc_dev_shell_start.png" alt="angular with bff" width="400" />   

Note: Do not launch the UIs without launching the back end projects as shown earlier to avoid runnning into issues related to the front-end\src\assets\serverenv.json file.

An environment variable you may see in the files  swagger-ui-shell.bat/.sh and angularshell.bat/.sh is VS_CODE_HOME. 
If such an environment variable is set in these scripts. expect VS Code to be launchable.   

If needed feel free to edit the batch/shell scripts to your convenience.  

#### Launching BFF Swagger APPs
Both the projects bff\bff-spring-projs\spring.oidc.bff and bff\bff-spring-projs\spring.simple.bff have a batch file and shell script named 
swagger-ui-shell.bat  OR
swagger-ui-shell.sh (I haven't yet used the .sh file. If using the .sh please adjust it as needed for now. )

I am using the batchfile. I launch it by right clicking on it and launching it.

<img src="images/swaggerui_dev_shell.png" alt="swagger with bff" width="400" />  

This shell has a very controlled environment PATH. 
It uses the node, npm,  exactly as setup in the pom.xml. 
It does not get influenced by the PATH configured at OS level.

Once in this shell I can use all my node, npm commands.

Showing below what happens when i type npm run dev on the shells for the two projects.

The swaggerui  for the simple BFF project runs on 3200.   

Use the command "npm run dev"  

<img src="images/swaggerui_simple_dev_shell_start.png" alt="swagger with bff" width="400" />  
Use the command ">npm run dev -- --port 3201"  
 
The swaggerui project for the OIDC BFF project runs on 3201.   
<img src="images/swaggerui_oidc_dev_shell_start.png" alt="swagger with bff" width="400" />   

Unlike with the angular projects i haven't configured ports in the swagger ui bffs. 
So we must specify the port when not using 3200 for the oidc swaggerui.   

Notes: 
The swagger ui related build is expecting git to be in the Path.  
We also do not use the OS Path.   
In the files - swagger-ui-shell.bat/.sh there is a GIT_HOME variable used for construction the Path variable specific to as needed by the node, npm etc setup by the project independent of OS Path.   
As long as you ensure an environment variable named GIT_HOME expect this shell to work without issues.  
  

Another variable you may see in the files  swagger-ui-shell.bat/.sh and angularshell.bat/.sh is VS_CODE_HOME. 
If such an environment variable is set in these scripts. expect VS Code to be launchable.   

If needed feel free to edit the batch/shell scripts.  

### Fastest development steps
Once full build has run through I tend to rely on the IDEs auto build features.   

#### For oidc app:  
1. Start the Backend via IDE   
Run the main class (e.g., bff\bff-spring-projs\spring.oidc.bff\src\main\java\com\example\demo\OidcBffApplication.java) in Eclipse, IntelliJ, or VS Code as a "Spring Boot App" or "Java Application" the way you would normally do in your IDE.  

Important: You must add the following properties to your Run Configuration VM arguments:

-Dokta.tenant.id=[TENANT_ID]    
-Dokta.oauth2.client-id=[CLIENT_ID]   
-Dokta.oauth2.client-secret=[CLIENT_SECRET]   
-Dfebaseurl=http://localhost:4201   
-Dswaggeruiurl=http://localhost:3201  
2. Launch UI Dev Shells   
For the frontend and Swagger plugin  use the provided batch/shell scripts to ensure the correct Node/NPM environment:  


For Angular: Launch angularshell.bat(.sh) and run npm start.  

**Important Note:** For angular when running on 4201 ensure you copy the file :   
bff\bff-spring-projs\spring.oidc.bff\front-end\src\templates\serverenv.json   
and paste it into :   
bff\bff-spring-projs\spring.oidc.bff\front-end\src\assets\serverenv.json   

You must do this before running npm start.   

(This is automated when you are using the previously described steps based on maven.)   

For Swagger UI Plugin: Launch swagger-ui-shell.bat and run :   
"npm run dev -- --port 3201".   

**Important Note:** For swagger-ui when running on 3201 ensure you copy the file :   
bff\swagger-files\configs\spring.oidc.bff\dev-helper-initializer.js   
and paste it into :   
bff\bff-spring-projs\spring.oidc.bff\swagger-ui\dev-helpers\dev-helper-initializer.js

#### For simple security app:  

1. Start the Backend via IDE   
Run the main class (e.g., bff\bff-spring-projs\spring.simple.bff\src\main\java\com\example\demo\SimpleBffApplication.java) in Eclipse, IntelliJ, or VS Code as a "Spring Boot App" or "Java Application" the way you would normally do in your IDE.  

Important: You must add the following properties to your Run Configuration VM arguments:

-Dfebaseurl=http://localhost:4200   
-Dswaggeruiurl=http://localhost:3200  
2. Launch UI Dev Shells   
For the frontend and Swagger plugin  use the provided batch/shell scripts to ensure the correct Node/NPM environment:  


For Angular: Launch angularshell.bat(.sh) and run npm start.  

**Important Note:** For angular when running on 4200 ensure you copy the file :   
bff\bff-spring-projs\spring.simple.bff\front-end\src\templates\serverenv.json   
and paste it into :   
bff\bff-spring-projs\spring.simple.bff\front-end\src\assets\serverenv.json   

You must do this before running npm start.   

(This is automated when you are using the previously described steps based on maven.)   

For Swagger UI Plugin: Launch swagger-ui-shell.bat and run :   
"npm run dev".   

**Important Note:** For swagger-ui when running on 3200 ensure you copy the file :   
bff\swagger-files\configs\spring.simple.bff\dev-helper-initializer.js   
and paste it into :   
bff\bff-spring-projs\spring.simple.bff\swagger-ui\dev-helpers\dev-helper-initializer.js


# Swagger UI Versions

This project has been tested with Swagger UI versions 5.30.1 through 5.31.0.  

By default, the build uses the latest specified Swagger UI version.  
However, you can override this using a system property when running Maven. For example:

mvn clean package -Pfullbuild -Deffective-sw.ui.version=5.31.0   

You can use any version number listed under the path bff\swagger-files  

namely any one of [5.30.1, 5.30.2, 5.30.3, 5.31.0]


# How to Use in a Regular Project

In the projects `bff\bff-spring-projs\spring.oidc.bff` and `bff\bff-spring-projs\spring.simple.bff`, a functional Swagger UI with BFF capability is assembled in a folder named `swagger-ui` directly under each project path.  
This folder is dynamically built based on the specified Swagger UI version.

Both projects contain **identical assembled BFF-enabled Swagger UI** with no code differences.  
The only expected difference is in the file:   

swagger-ui/dev-helpers/dev-helper-initializer.js

You can easily reuse this folder in any project as a static web resource.  
If using springdoc, make sure to **exclude the default `swagger-ui` WebJar** from your pom file.  

Alternatively, instead of using it as a static folder, you can package it as a BFF-flavored WebJar (this may be added in the future to demonstrate use in regular projects).

While on the topic: Springdoc is configured via Swagger extensions to control the BFF-enabled Swagger UI.  

You can find the details in the following files:

- `bff\bff-spring-projs\spring.oidc.bff\src\main\java\com\example\demo\SpringdocConfig.java`  
- `bff\bff-spring-projs\spring.simple.bff\src\main\java\com\example\demo\SpringdocConfig.java`

Specifically, check the `customOpenAPI` method in each class to see how the BFF plugin is integrated with springdoc.  





# What’s Not Currently Included

- Session-Token Synchronization: 
This demo does not currently coordinate HTTP session timeouts with access token or refresh token lifetimes.  
Implementing this correctly depends on the specific requirements and constraints of each application, so it would need to be handled flexibly and thoughtfully.  
It may be included in a future version.  
- Spring Boot 4 migration: A manual Spring Security OIDC configuration could enable Spring Boot 4 today, but this would replace the Okta starter with custom code. We have deferred this until official Okta support is available.


