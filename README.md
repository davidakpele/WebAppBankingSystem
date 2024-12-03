# Global Banking and Crypto Trading System 

> Collaborated with the front-developers team producing endpoints that best suits the web and mobile application. Developed api endpoints to manage the users Global Digital Payment System and crypto trading, supporting both local currency operations and blockchain integration for cryptocurrency trading. Implemented features for platform and P2P crypto trading, integrating blockchain technology with the potential for future currency exchange functionalities.

## Micro-Service Architecture With Spring Security and Go-Lang Gin Framework
# Service 1
### Authentication Service

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
> This Authentication service Handles User authentication and authorization which enables Users to access and operate on the entire application e.g "Deposit money into their wallet which need to be authenticated with a specific user details, Transfer to bank or another user in the platform using their username, pay bills and trade crypto."
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

## What are the challenges encounter from the stated project (if any)?

- Challenge(1): Seamlessly integrating Spring Security, Go services, and Spring Boot for a unified user experience while maintaining consistent data flow across platforms.
- Challenge(2): Ensuring secure authentication and authorization across multiple services (Spring Security for authentication, Go for trading). Token sharing and validation in a distributed architecture are critical.

## **How were you able to overcome it?**
- Solution(1): Use message brokers like RabbitMQ on authentication service and Kafka on deposit/withdraw wallet to enable smooth communication between services and ensure robust API documentation for cross-language compatibility.
- Solution(2): Implement a centralized token service (using OAuth2/JWT) to ensure uniform security policies across all services.

# Service 2
### Deposit & Withdral Service

> This Service is responsible of creating e-wallet for user where User can make deposit in their currency and withdral or transfer.

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
* **File Handling as Receipt Generated**
* **Data Transfer Objects**
* **Rest API Controller**
* **Repositories**
* **Services**
* **Responses**
* **Mockito Testing**

## Introduction
> Authentication Service provides JSON Web Tokens to users during login process that enable them to operate on their wallet.

## Quick Start
> This service forcus on designing wallet balance, deposit action, withdraw action and history
#### How This Work?
- First time user login into, User will be ask to created transfer pin, this pin consist of 4 digit number of their choice.
- Pin will be use to process transfer or any withdraws for security reasons and user can change it anytime.
- User has `Level 2 Option` to set up 2FA-Authentication, means every transfer or any withdraws `OPT` pin will be send to User email instead of having default PIN.
- To Deposit user has options to choose the payment service they want to use "Paystack or flutterWave" and when deposit request is send application check which platform user select to make their deposit so that the application can user request to the platform requesting credit and debit action should taken. ```Deposit from user bank and credit user wallet in the platform``` .
- Deposit success check and send notification to user about their current wallet balance and action taken with the help of RabbitMQ.
- User can transfer money to another user in same platform using recipient username example ***```{'recipientUsername':'John'}```*** and if transaction is successful application automatically send notification message to both users ***```Debit message to sender and Credit message to the recipient```***.
- User can view their Transaction history, filter by date, days and year.
- User can always print transaction receipt because every transaction generate receipt and save it into the database, User can either download immidiately after transaction is successful or comes back to download it from transaction history. This is keep transparency in the platform service which they provide to customers and helps users to keep track of their transactions.
- Design the system with secure data storage, encryption, and audit trails. Regularly update policies to align with evolving regulations.
- Implement centralized logging (e.g., ELK stack) and distributed tracing (e.g., Zipkin, Jaeger) to diagnose and resolve issues promptly.
- Built middlware Detecting Sophisticated Fraud Patterns with asynchronous processing.
- Use machine learning models to enhance behavioral analysis and detect sophisticated patterns.
- Implement regular audits and updates to ensure compliance with the latest regulations.
- Employ real-time monitoring tools to provide instant alerts and actionable insights for flagged activities.
- Created Mockito Unit testing / Junit testing for every phase of the application.

## Project Structure 

```
C:.
├───.mvn
│   └───wrapper
├───src
│   ├───main
│   │   ├───java
│   │   │   └───com
│   │   │       └───example      
│   │   │           └───deposit  
│   │   │               ├───config     
│   │   │               ├───controllers
│   │   │               ├───dto        
│   │   │               ├───enums      
│   │   │               ├───exceptions
│   │   │               ├───messageProducer
│   │   │               │   └───requests
│   │   │               ├───middleware
│   │   │               ├───models
│   │   │               ├───payloads
│   │   │               ├───properties
│   │   │               ├───repository
│   │   │               ├───responses
│   │   │               ├───serviceImplementations
│   │   │               ├───services
│   │   │               └───utils
│   │   └───resources
│   │       ├───static
│   │       │   └───image
│   │       └───templates
│   └───test
│       └───java
│           └───com
│               └───example
│                   └───deposit
└───target
    ├───classes
    │   ├───com
    │   │   └───example
    │   │       └───deposit
    │   │           ├───config
    │   │           ├───controllers
    │   │           ├───dto
    │   │           ├───enums
    │   │           ├───exceptions
    │   │           ├───messageProducer
    │   │           │   └───requests
    │   │           ├───middleware
    │   │           ├───models
    │   │           ├───payloads
    │   │           ├───properties
    │   │           ├───repository
    │   │           ├───responses
    │   │           ├───serviceImplementations
    │   │           ├───services
    │   │           └───utils
    │   └───templates
    ├───generated-sources
    │   └───annotations
    ├───generated-test-sources
    │   └───test-annotations
    ├───maven-status
    │   └───maven-compiler-plugin
    │       ├───compile
    │       │   └───default-compile
    │       └───testCompile
    │           └───default-testCompile
    └───test-classes
        └───com
            └───example
                └───deposit
```
## Application Configuration 
> This ApplicationConfiguration class is slightly different from authentication service `Application Configuration class` because we only create `User Model class` in `application service` so we will capitalize on that and use `WebClient`.

  - Why creating another ApplicationConfiguration or why even trying to locked this service again when we already have authentication service?
      - Well it easy to think that as a beginner. Even though we have authentication service that generate our json web token, that doesn't guaranty this deposit service is secure because it isn't and it vulnerable if we fail to secure it so to secure it we need to lock the entire application down and use the json web token generated from authentication service login to access this service.
    > **NOTE This service by defualt microservices architecture we need to be divided into _FOUR SERVICES_  as follow:**
      - Wallet Service
      - Deposit Service
      - Withdraw Service
      - History Service 
    > But this is a prototype and other developers might find something useful to learn from it.
    
    _IF YOU ARE TO CREATE REAL LIFE PROJECT LIKE THIS WITH MICRO-SERVICE ARCHITECTURE, I WOULD ADVICE YOU TO SEPARATE THIS PARTICULAR SERVICE INTO SMALL PIECES LIKE WHAT I JUST SHOW ABOVE_


```
@Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            try {
                String token = tokenExtractor.extractToken(request);
                UserDTO userDTO = userServiceClient.getUserByUsername(username, token);

                if (userDTO != null) {
                    // Map user roles if necessary, here defaulting to ROLE_USER
                    List<SimpleGrantedAuthority> authorities = userDTO.getRecords().stream()
                            .map(record -> new SimpleGrantedAuthority("ROLE_USER"))
                            .toList();

                    return new org.springframework.security.core.userdetails.User(
                            userDTO.getUsername(),
                            "", 
                            userDTO.isEnabled(),
                            true, // Account non-expired
                            true, // Credentials non-expired
                            !isAccountLocked(userDTO), // Account non-locked
                            authorities
                    );
                } else {
                    throw new UsernameNotFoundException("User not found: " + username);
                }
            } catch (Exception e) {
                // Log the error and throw an exception to prevent unauthorized access
                System.err.println("Error retrieving user details: " + e.getMessage());
                throw new UsernameNotFoundException("Unable to fetch user details for: " + username, e);
            }
        };
    }

    // Helper method to determine if the account is locked
    private boolean isAccountLocked(UserDTO userDTO) {
        return userDTO.getRecords().stream().anyMatch(UserRecordDTO::isLocked);
    }
```
- This code above shows us instead of using userRepository to fetch user directly from the database we are using `UserServiceClient` and below you will see `UserServiceClient` is using WebClient class.

> This class, UserServiceClient, is a Spring service responsible for making HTTP requests to an external user-related API using WebClient, a reactive, non-blocking HTTP client.
```
@Service
public class UserServiceClient {

    private final WebClient webClient;

    @Autowired
    public UserServiceClient(WebClient.Builder webClientBuilder, @Value("${auth-service.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public UserDTO getUserByUsername(String username, String token) {
        return this.webClient.get()
                .uri("/api/v1/user/by/username/{username}", username)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    if (clientResponse.statusCode().is4xxClientError()) {
                                        String details = extractDetailsFromError(errorMessage);
                                        return Mono.error(new UserNotFoundException("User not found", details));
                                    }
                                    return Mono.error(new RuntimeException("Server error"));
                                }))
                .bodyToMono(UserDTO.class)
                .block();
    }
}
```
### Dependencies Injected
- `WebClient.Builder:` Configures the `WebClient` with a base URL and other settings.
- `@Value("${auth-service.base-url}"):` Reads the `auth-service.base-url` property from the application configuration `application.yml` for the API's base URL.
    - Key Points:
      - `Error Handling:` Uses onStatus to handle 4xx and 5xx HTTP errors.
        - For 4xx errors, extracts a detailed error message using `extractDetailsFromError`.
        - Throws `UserNotFoundException` for user-related issues.
       
- One thing you will also notice is we are using `USERDTO class`, Yes
- In microservice application you create alot of `DTO'S` class objects to read data that is why it call **`Data Transfer Objects`** you can say it serve as user representative in this this service which consist both **`User model`** and **`UserRecord model`**

## What are the challenges encounter from the stating of the project?

- `Challenge(1)` Providing real-time updates for crypto prices and transactions, which requires efficient communication between the backend and the front-end.
- `Challenge(2)` Supporting both local currency operations and some foreign currency swapping involves accurate and up-to-date exchange rate handling, which can be resource-intensive.
- `Challenge(3)` The platform must handle high traffic and complex operations like trading, wallet transactions, and blockchain calls without degrading performance.
- `Challenge(4)` Ensuring compliance with local and international regulations for payment systems.
- `Challenge(5)` Detecting Sophisticated Fraud Patterns- Fraudsters often use complex strategies that blend legitimate and illegitimate activities, making detection difficult.

## How were i able to overcome it?

- `Solution(1)` Use WebSockets or server-sent events for real-time updates and ensure the trading engine in Go can handle concurrent transactions effectively.
- `Solution(2)` Leverage flutterWave APIs for real-time exchange rates like naira-to-dollar, dollar-to-euro, naira-to-pounds exchange and implement robust error handling to deal with third-party API failures.
- `Solution(3)` Use microservices architecture, load balancing, and database optimization. Incorporate caching for frequently accessed data and monitor system performance.
- `Solution(4)` Design the system with secure data storage, encryption, and audit trails. Regularly update policies to align with evolving regulations
- `Solution(5)` Created middleware for advance security fraud detechtion:
    - Implemented middleware for IP address monitoring and transaction interception based on several criteria
         - **Large Transactions:** Flagged transactions exceeding platform-defined thresholds for further review.
         - **High-Frequency Transactions:** Monitored accounts for unusually high transaction volumes within short time-frames to detect suspicious behavior or could indicate potential money laundering or illegal activity.
         - **Geographic and Risk-Based Monitoring:**  Identified transactions involving high-risk regions/countries or blacklisted wallet addresses to comply with AML regulations.
         - **Behavioral Analysis:** Detected inconsistent behavior, such as large deviations from typical transaction amounts, to prevent fraud.
         - **Multiple Accounts Sharing the Same IP:** Checked for potential sybil attacks by monitoring accounts initiating transactions from the same IP address.
             - **Reason:**
                 - This could be a sign of suspicious activity such as a single entity controlling multiple accounts.
        - **Deposits Followed by Immediate Transfers:** Flagged immediate fund transfers after deposits to prevent potential money laundering activities.
            - **Reason:**
                - This behavior could indicate attempts to obfuscate the origin of the funds (layering phase of money laundering).
        - Implement data Encryption.
        - Implement Event Sourcing to make history difficult to tamper.
  
 # VIEW THE DEPOSIT WITHDRAW TRANSFER SERVICE APPLICATION
 
 - [Go Deposit Service branch](https://github.com/davidakpele/globalBankingSystem/tree/ewallet)
 - [Go Crypto Service Branch](https://github.com/davidakpele/globalBankingSystem/tree/cryptoservice)
 - [Go BillPayment Service Branch](https://github.com/davidakpele/globalBankingSystem/tree/billservice)
