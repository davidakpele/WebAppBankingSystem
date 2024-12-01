package com.pesco.authentication.configurations;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
 
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Define constants for queues, exchange, and routing keys
    public static final String AUTH_EXCHANGE = "auth.notifications";

    @Bean
    public DirectExchange authExchange() {
        return new DirectExchange(AUTH_EXCHANGE);
    }

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

    // Configure RabbitTemplate with Jackson2JsonMessageConverter for JSON serialization
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

}
