/* eslint-disable no-undef */
window.onload = function() {
	function getCsrfTokenFromCookie(name) {
	  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
	  return match ? match[2] : null;
	}
  window["SwaggerUIBundle"] = window["swagger-ui-bundle"]
  window["SwaggerUIStandalonePreset"] = window["swagger-ui-standalone-preset"]
  // Build a system
  const ui = SwaggerUIBundle({
    url: "http://localhost:8081/v3/api-docs",
    dom_id: "#swagger-ui",
	
	requestInterceptor: (req) => {
	    console.log("TOP-LEVEL requestInterceptor:", req.url)
	    if (req.url && !req.url.startsWith("/")) {
	      // Swagger UI running on different origin (dev mode)
	      req.credentials = "include"
	    }
		
		// 2. Attach CSRF token for state-changing methods
		 const stateChangingMethods = ["POST", "PUT", "PATCH", "DELETE"]

		 if (
		   req.method &&
		   stateChangingMethods.includes(req.method.toUpperCase())
		 ) {
		   const csrfToken = getCsrfTokenFromCookie("XSRF-TOKEN")

		   if (csrfToken) {
		     req.headers = req.headers || {}
		     req.headers["X-XSRF-TOKEN"] = csrfToken
		   }
		 }
		 
	    return req
	  },
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    // requestSnippetsEnabled: true,
    layout: "StandaloneLayout"
  })

  window.ui = ui

  ui.initOAuth({
    clientId: "your-client-id",
    clientSecret: "your-client-secret-if-required",
    realm: "your-realms",
    appName: "your-app-name",
    scopeSeparator: " ",
    scopes: "openid profile email phone address",
    additionalQueryStringParams: {},
    useBasicAuthenticationWithAccessCodeGrant: false,
    usePkceWithAuthorizationCodeGrant: false
  })
}
