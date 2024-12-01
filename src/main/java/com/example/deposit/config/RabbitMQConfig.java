package com.example.deposit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Constants for exchange and queues
    public static final String WALLET_EXCHANGE = "wallet.notifications";
    
    public static final String CREDIT_WALLET_QUEUE = "credit.wallet";
    public static final String DEBIT_WALLET_QUEUE = "debit.wallet";
    public static final String DEPOSIT_WALLET_QUEUE = "deposit.wallet";
    public static final String MAINTENANCE_DEDUCTION_QUEUE = "maintenance.service";
    // Routing keys
    public static final String ROUTING_KEY_CREDIT_WALLET = "wallet.credit";
    public static final String ROUTING_KEY_DEBIT_WALLET = "wallet.debit";
    public static final String ROUTING_KEY_DEPOSIT_WALLET = "wallet.deposit";
    public static final String ROUTING_KEY_WALLET_MAINTENANCE_SERVICE_DEDUCTION = "wallet.deduction-fee";

    // Declare the exchange
    @Bean
    public TopicExchange walletExchange() {
        return new TopicExchange(WALLET_EXCHANGE);
    }

    // Declare the queues
    @Bean
    public Queue creditWalletQueue() {
        return new Queue(CREDIT_WALLET_QUEUE);
    }

    @Bean
    public Queue debitWalletQueue() {
        return new Queue(DEBIT_WALLET_QUEUE);
    }

    @Bean
    public Queue depositWalletQueue() {
        return new Queue(DEPOSIT_WALLET_QUEUE);
    }
    
    @Bean
    public Queue walletMaintenanceDeductionQueue() {
        return new Queue(MAINTENANCE_DEDUCTION_QUEUE);
    }

    // Bindings
    @Bean
    public Binding creditWalletBinding(Queue creditWalletQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(creditWalletQueue).to(walletExchange).with(ROUTING_KEY_CREDIT_WALLET);
    }

    @Bean
    public Binding debitWalletBinding(Queue debitWalletQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(debitWalletQueue).to(walletExchange).with(ROUTING_KEY_DEBIT_WALLET);
    }

    @Bean
    public Binding depositWalletBinding(Queue depositWalletQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(depositWalletQueue).to(walletExchange).with(ROUTING_KEY_DEPOSIT_WALLET);
    }

    @Bean
    public Binding walletMaintenanceBinding(Queue walletMaintenanceDeductionQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(walletMaintenanceDeductionQueue).to(walletExchange).with(ROUTING_KEY_WALLET_MAINTENANCE_SERVICE_DEDUCTION);
    }

    // Configure RabbitTemplate with Jackson2JsonMessageConverter for JSON serialization
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
