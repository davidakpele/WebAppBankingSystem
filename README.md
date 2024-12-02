# Authentication Service 

- Built the authentication, authorization and verification with Spring Boot & Spring Security and mysql database. The demo frontend is built using React js.

## Table of Contents
* **Introduction**
* **Quick Start**
* **Folder Structure**
* **Application Configuration**
* **JWT Configuration**
* **Security Configuration**
* **RabbitMQ Configuration**
* **RabbitMQ Message Producer**
* **Error Handling**
* **File Handling**
* **Data Transfer Objects**
* **Rest API Controller**
* **Repositories**
* **Services**
* **Responses**
* **Contributing**
* **Testing**
  
## Introduction
> Spring Boot Security provides mechanisms to secure applications and APIs or endpoints. Both JWT (JSON Web Tokens) and OAuth2 are widely used for authentication and authorization. In this project we take advantage of this mechanisms spring boot provides us.

## Quick Start
> This Authentication service Handles User authentication and authorization which enables them the access to operate on the entire application e.g "Deposit money into their wallet which need to be authenticated with a specific user details, Transfer to bank or another user in the platform using their username, pay bills and trade crypto."
- How this work is User sign-Up with require detials ['firstname, lastname, email, username, password, telephone, gender']
- Request will validate and Store user into the database and send "Account Verification" email notification message with RabbitMQ message broker/message Queue in asynchronously compact.
- If user verify their account, the can login and login success process generate jwt token and return user data like "Username, UserId and User Jwt token"
- Note every users is treated Uniquely and that makes jwt Unique to user and also makes it easy to authentication with spring security Authentication testing against the username in the encryted in the jwt and username username by pass or any datails user may pass along with their request.
- This service also enable users to turn on 2Fa-Authentication process for additional security to their wallet so every time user try to login, OTP Keys will be send to user name from the database and once user provide the OTP key and verified user will be process login success.
- This Service enable user to change password when logged-In, reset password if forgotten, change profile by uploading profile picture.
- Forget password process has RabbitMQ message process, including 2FA-Authentication, Account Verification.

# Folder Structure 
```
C:.
├───.mvn
│   └───wrapper
├───src
│   ├───main
│   │   ├───java
│   │   │   └───com
│   │   │       └───pesco
│   │   │           └───authentication      
│   │   │               ├───configurations  
│   │   │               ├───controllers     
│   │   │               ├───dto
│   │   │               ├───enums
│   │   │               ├───MessageProducers
│   │   │               │   └───requests
│   │   │               ├───micro_services
│   │   │               ├───middleware
│   │   │               ├───models
│   │   │               ├───payloads
│   │   │               ├───properties
│   │   │               ├───repositories
│   │   │               ├───responses
│   │   │               ├───security
│   │   │               ├───serviceImplementations
│   │   │               └───services
│   │   └───resources
│   │       ├───static
│   │       │   ├───css
│   │       │   ├───image
│   │       │   └───js
│   │       └───templates
│   └───test
│       └───java
│           └───com
│               └───pesco
│                   └───authentication
└───target
    ├───classes
    │   ├───com
    │   │   └───pesco
    │   │       └───authentication
    │   │           ├───configurations
    │   │           ├───controllers
    │   │           ├───dto
    │   │           ├───enums
    │   │           ├───MessageProducers
    │   │           │   └───requests
    │   │           ├───micro_services
    │   │           ├───middleware
    │   │           ├───models
    │   │           ├───payloads
    │   │           ├───properties
    │   │           ├───repositories
    │   │           ├───responses
    │   │           ├───security
    │   │           ├───serviceImplementations
    │   │           └───services
    │   ├───static
    │   │   ├───css
    │   │   └───js
    │   └───templates
    ├───generated-sources
    │   └───annotations
    ├───generated-test-sources
    │   └───test-annotations
    ├───maven-archiver
    ├───maven-status
    │   └───maven-compiler-plugin
    │       ├───compile
    │       │   └───default-compile
    │       └───testCompile
    │           └───default-testCompile
    ├───surefire-reports
    └───test-classes
        └───com
            └───pesco
                └───authentication

```

## Application Configuration 
> This ApplicationConfiguration class is a Spring Boot configuration class that sets up essential components for handling user authentication and password encoding. Here's a breakdown of what each part does:

####  We Have Four ```@Bean``` define in this class.
- Class-Level Annotations
     - ```@Configuration:``` Marks this class as a source of Spring Beans for the application context. Spring will scan and register the beans defined here.
     - ```@RequiredArgsConstructor:``` Automatically generates a constructor for any final fields, in this case, UsersRepository. This makes dependency injection more concise.
     -  Bean Definitions
         * **UserDetailsService  ```@Bean method```**
```
@Bean
public UserDetailsService userDetailsService() {
    return username -> {
        Optional<Users> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            return new org.springframework.security.core.userdetails.User(
                    username,
                    "",
                    Collections.emptyList());
        }
    };
}
```
* **Purpose:** Provides a way for Spring Security to fetch user details by username during authentication.
* **How It Works:**
    > Calls ```userRepository.findByUsername(username)``` to retrieve user details from the database.<br/>
    > If a user exists, it returns the Users <br/>
    > If no user is found, it returns a default User object with the provided username and empty password ("") and then No granted authorities (Collections.emptyList()) which means User can't get access the application because user doesn't exist.
    * **AuthenticationProvider ```@Bean method```**
```
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

* **Purpose:** Defines the mechanism for authenticating users.
* ****DaoAuthenticationProvider:**** A standard provider that uses UserDetailsService to fetch user details and validates the password using the provided PasswordEncoder.
* ****setUserDetailsService(userDetailsService()):**** Links the custom UserDetailsService bean for fetching user details.
* ****setPasswordEncoder(passwordEncoder()):**** Configures password encoding using BCrypt.

* **AuthenticationManager ```@Bean method```**
```
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

* **Purpose:** Exposes the AuthenticationManager bean, which coordinates authentication by delegating to AuthenticationProviders.
* ***Why It’s Needed:*** Allows manual injection of AuthenticationManager in other parts of the application (e.g., custom login logic).

  **PasswordEncoder ```@Bean method```**
```
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
  ***Purpose:** Configures password hashing using the BCrypt algorithm.
* *Why It’s Secure?**
  - BCrypt is a robust algorithm designed for password hashing.
  - It includes salting and a configurable work factor, making brute-force attacks computationally expensive.<br/>
  
## JwtAuthenticationFilter Configuration Class
 > This JwtAuthenticationFilter is a custom implementation of a filter that processes incoming HTTP requests to verify JWT tokens, extract user details, and set up security context for authenticated users. It extends OncePerRequestFilter, which ensures the filter is executed only once per request.

```
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtServiceImplementations jwtService;
    private final UserDetailsService userDetailsService;
}
```

* **```@Component:```** Marks the class as a Spring Bean, enabling Spring to manage its lifecycle and include it in the application context.
* **```@RequiredArgsConstructor:```**  Generates a constructor for final fields, allowing dependency injection for JwtServiceImplementations and UserDetailsService.
* The ```doFilterInternal``` Method Retrieves the Authorization header from the HTTP request. i.e
 ```
 final String authHeader = request.getHeader("Authorization");
 ````
- Check Header Validity:
  > Ensures the header is present and starts with "Bearer ".
  > If invalid, the filter skips further processing and lets the request continue.
```
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```
- Extract the JWT and Username:
```
jwt = authHeader.substring(7);
userEmail = jwtService.extractUsername(jwt);
```
- Extracts the JWT by removing the "Bearer " prefix.
- Calls jwtService.extractUsername(jwt) to extract the username encoded in the token.
  * **Authenticate the User:***
```
if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {}
```
- Ensures the username exists and no authentication is already present in the security context.
  * **Load User Details:**
```
UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
```
- Fetches the user’s details from UserDetailsService.
   * **Validate the JWT:**
```
if (jwtService.isTokenValid(jwt, userDetails)) {}
```
- Calls jwtService.isTokenValid(jwt, userDetails) to check if the token is valid and matches the user.
  * **Extract Roles and Authorities:**
```
Claims claims = jwtService.extractAllClaims(jwt);
List<String> roles = claims.get("roles", List.class);
```
- Retrieves roles from the JWT claims.
- Converts roles into SimpleGrantedAuthority objects required by Spring Security.
  ***Set Authentication in the Security Context:**
```
UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
    userDetails, null, authorities);
authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
SecurityContextHolder.getContext().setAuthentication(authToken);
```
- Creates an authentication token with user details and granted authorities.
- Sets it in the SecurityContextHolder.
  **Handle Exceptions:**
```
} catch (ExpiredJwtException e) {
    setErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, response, "Token has expired", "expired_token");
} catch (JwtException | IllegalArgumentException e) {
    setErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, response, "Invalid token", "invalid_token");
}
```
  * **```ExpiredJwtException:```** Handles cases where the token is expired.
  * **```JwtException | IllegalArgumentException:```** Handles invalid or malformed tokens.
  Calls **```setErrorResponse```** to send a standardized error response.
   - Continue the Filter Chain:
```
filterChain.doFilter(request, response);
```
- Ensures the request proceeds through other filters in the chain.

## RabbitMQ Configuration Class

> This RabbitMQConfig class is a Spring configuration class that sets up RabbitMQ messaging components such as exchanges, queues, bindings, and message converters. It simplifies the integration of RabbitMQ with a Spring Boot application and ensures that messages can be serialized and deserialized as JSON objects.
- Message Converter
```
@Bean
public Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
}
```
- Defines a message converter that converts Java objects to JSON and vice versa.
- Ensures that RabbitMQ messages are serialized into JSON when sent and deserialized back into Java objects when received.
   - **RabbitMQ Constants**
```
public static final String AUTH_EXCHANGE = "auth.notifications";
```
- A constant for the exchange name ```(auth.notifications)```
- Used as a central reference to avoid hardcoding the exchange name throughout the code.
   - **Exchange Definition**
```
@Bean
public DirectExchange authExchange() {
    return new DirectExchange(AUTH_EXCHANGE);
}
```
- Created a direct exchange named auth.notifications
- Direct exchanges route messages to queues based on routing keys that exactly match the queue bindings.
  - **Queue Definitions**
```
@Bean
public Queue emailVerificationQueue() {
    return new Queue("email.verification");
}

@Bean
public Queue emailOtpQueue() {
    return new Queue("email.otp");
}

@Bean
public Queue emailResetPasswordQueue() {
    return new Queue("email.reset-password");
}
```
- Creates three distinct RabbitMQ queues:
    - **email.verification:** Used for handling email verification messages.
    - **email.otp:** Used for handling OTP (One-Time Password) emails.
    - **email.reset-password:** Used for handling password reset emails.
  - **Binding Queues to Exchange**
```
@Bean
public Binding emailVerificationBinding(Queue emailVerificationQueue, DirectExchange authExchange) {
    return BindingBuilder.bind(emailVerificationQueue).to(authExchange).with("email.verification");
}

@Bean
public Binding emailOtpBinding(Queue emailOtpQueue, DirectExchange authExchange) {
    return BindingBuilder.bind(emailOtpQueue).to(authExchange).with("email.otp");
}

@Bean
public Binding emailResetPasswordBinding(Queue emailResetPasswordQueue, DirectExchange authExchange) {
    return BindingBuilder.bind(emailResetPasswordQueue).to(authExchange).with("email.reset-password");
}
```
- Bindings connect queues to the exchange with specific routing keys:
  - **email.verification** routing key for the email.verification queue.
  - **email.otp** routing key for the email.otp queue.
  - **email.reset-password** routing key for the email.reset-password queue.
- A direct exchange routes messages to a queue only if the message’s routing key matches the binding key.
    -  **RabbitTemplate Configuration**
```
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter());
    return rabbitTemplate;
}
```
- ```RabbitTemplate:``` A Spring abstraction for sending and receiving messages to/from RabbitMQ
- Configured with
    - **ConnectionFactory:** Manages the RabbitMQ connection.
    - **MessageConverter:** Uses the Jackson2JsonMessageConverter to serialize messages to JSON.


# NOTE
> The ```application.yml``` structure is not the best way to set up your application especially for production, i would recommend ```.env``` file  or file like ```app.key``` to hold jwt private secret key and ```app.pub``` holding public key file and rest like paystack, flutterwave keys should be in ```.env``` file **FOR SECURITY REASONSE**

# What are the challenges encounter from the stated project (if any)?

- Challenge(1): Seamlessly integrating Spring Security, Go services, and Spring Boot for a unified user experience while maintaining consistent data flow across platforms.
- Challenge(2): Ensuring secure authentication and authorization across multiple services (Spring Security for authentication, Go for trading). Token sharing and validation in a distributed architecture are critical.

# **How were you able to overcome it?**
- Solution(1): Use message brokers like RabbitMQ on authentication service and Kafka on deposit/withdraw wallet to enable smooth communication between services and ensure robust API documentation for cross-language compatibility.
- Solution(2): Implement a centralized token service (using OAuth2/JWT) to ensure uniform security policies across all services.

 # VIEW THE DEPOSIT WITHDRAW TRANSFER SERVICE APPLICATION
 - [Go Deposit Service branch](https://github.com/davidakpele/globalBankingSystem/tree/ewallet)
 - [Go Crypto Service Branch](https://github.com/davidakpele/globalBankingSystem/tree/cryptoservice)
 - [Go BillPayment Service Branch](https://github.com/davidakpele/globalBankingSystem/tree/billservice)
