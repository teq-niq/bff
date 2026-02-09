package com.example.demo;



import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import com.example.demo.SecurityConfiguration.SpaCsrfTokenRequestHandler;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	@Value("${febaseurl:#{null}}")
    private String feBaseUrl; // (null if not set)
	
	@Value("${swaggeruiurl:#{null}}")
    private String swaggerUiBaseUrl; // (null if not set)
	
	@PostConstruct
	private void init()
	{
		if(feBaseUrl!=null && feBaseUrl.trim().length()==0)
		{
			feBaseUrl=null;
		}
		if(swaggerUiBaseUrl!=null && swaggerUiBaseUrl.trim().length()==0)
		{
			swaggerUiBaseUrl=null;
		}
	}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.builder()
                .username("user")
                .password(encoder.encode("password"))
                .roles("myuser")
                .build();
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("password"))
                .roles("myadmin")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
   System.out.println("feBaseUrl=["+feBaseUrl+"]"); 	
   System.out.println("swaggerUiBaseUrl=["+swaggerUiBaseUrl+"]"); 	
   boolean feBaseUrlIsNotNull = feBaseUrl!=null;;
   boolean swaggerUiBaseUrlIsNotNull = swaggerUiBaseUrl!=null;;
	if (feBaseUrlIsNotNull||swaggerUiBaseUrlIsNotNull) {
			
			
			if(feBaseUrlIsNotNull) {
				System.out.println("CORS enabled for febaseurl: "+feBaseUrl);
			}
			if(swaggerUiBaseUrlIsNotNull) {
				System.out.println("CORS enabled for swaggeruiurl: "+swaggerUiBaseUrl);
			}
			Customizer<CorsConfigurer<HttpSecurity>> corsCustomizer=new Customizer<CorsConfigurer<HttpSecurity>>() {
				
				@Override
				public void customize(CorsConfigurer<HttpSecurity> http) {
					
					http.configurationSource(request->{
						CorsConfiguration cors=new org.springframework.web.cors.CorsConfiguration();
							if(feBaseUrlIsNotNull) {
								cors.addAllowedOrigin(feBaseUrl);
								System.out.println("added feBaseUrl to CORS:"+feBaseUrl+" for request URL:"+request.getRequestURL());
							}
							if(swaggerUiBaseUrlIsNotNull) {
								cors.addAllowedOrigin(swaggerUiBaseUrl);
								System.out.println("added swaggerUiBaseUrl to CORS:"+swaggerUiBaseUrl+" for request URL:"+request.getRequestURL());
							}
							
						
						
						cors.addAllowedMethod("*");
						cors.addAllowedHeader("*");
						cors.setAllowCredentials(true);
						return cors;
					});
				}
			};
			
			http=http.cors(corsCustomizer);
			///if(swaggerUiBaseUrl==null)
			{
				http=http
					    
					    .csrf(csrf -> 
					    		csrf
					    		.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
					    		
					    		.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()) 
					    		//.csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
					    		//.ignoringRequestMatchers("/v3/api-docs/**","/v2/api-docs/**", "/swagger-ui/**")
					    		//.ignoringRequestMatchers("/logout", "/apilogout")   // allow POST /logout without CSRF
					    	  );
				System.out.println("CSRF protection is enabled");
			}
//			else
//			{
//				  http=http
//		            .csrf(csrf -> csrf.disable()); // Optional for testing REST
//				  System.out.println("CSRF protection is disabled for now only when running swagger in dev mode");
//			}
			
		}
		else
		{
			System.out.println("The application is self-contained, CORS remains deny-by-default and CSRF protection is enabled.");
			
			
http=http
				    
				    .csrf(csrf -> 
				    		csrf
				    		.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				    		
				    		.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()) 
				    		
				    	  );
				    
				    
		}

        http = http
            .authorizeHttpRequests(auth -> auth
            		.requestMatchers("/secured/profile").authenticated()
	        		
    	            .requestMatchers("/secured/admin").hasRole("myadmin")  // ROLE_myadmin
    	            .requestMatchers("/secured/user").hasRole("myuser")    // ROLE_myuser

    	            //.anyRequest().authenticated()
    	            .anyRequest().permitAll()            // everything else allowed
            )
            .formLogin(form -> form
            		  .loginProcessingUrl("/login")
            		  .successHandler((req, res, auth) -> res.setStatus(200))
            		  .failureHandler((req, res, ex) -> res.sendError(401))
            		)
            .logout(logout -> logout
            	    .logoutUrl("/logout")
            	    .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
            	)
            .httpBasic(Customizer.withDefaults()
            		
            		
            		); // optional for API tools
        
        http=http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    //String accept = request.getHeader("Accept");
                    //if (accept != null && accept.contains("application/json")) {
                        // return 401 for API calls
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    //} else {
                        // default redirect to login page
                        //response.sendRedirect("/login");
                    //}
                })
            );
        

        return http.build();
    }
    
    
    final static class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
		private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			/*
			 * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
			 * the CsrfToken when it is rendered in the response body.
			 */
			this.xor.handle(request, response, csrfToken);
			/*
			 * Render the token value to a cookie by causing the deferred token to be loaded.
			 */
			csrfToken.get();
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String headerValue = request.getHeader(csrfToken.getHeaderName());
			/*
			 * If the request contains a request header, use CsrfTokenRequestAttributeHandler
			 * to resolve the CsrfToken. This applies when a single-page application includes
			 * the header value automatically, which was obtained via a cookie containing the
			 * raw CsrfToken.
			 *
			 * In all other cases (e.g. if the request contains a request parameter), use
			 * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
			 * when a server-side rendered form includes the _csrf request parameter as a
			 * hidden input.
			 */
			return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
		}
	}

}
