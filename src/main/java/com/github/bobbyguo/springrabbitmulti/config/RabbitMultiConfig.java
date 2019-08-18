package com.github.bobbyguo.springrabbitmulti.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 
 * multi rabbit config
 * for now the declare of queue/echange/binding are not supported in @RabbitListener.
 * @author: bobby guo
 *
 */
@EnableRabbit
@Configuration
@EnableConfigurationProperties(MultiRabbitProperties.class)
public class RabbitMultiConfig {

    @Autowired
    private ConfigurableBeanFactory beanFactory;
    @Autowired
    private MultiRabbitProperties multiConfig;

    @Primary
    @Bean
    public ConnectionFactory connectionFactory(RabbitProperties config,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        if (multiConfig.getMulti() == null || multiConfig.getMulti().isEmpty()) {
            return rabbitConnectionFactory(config);
        }

        // init routing factory
        SimpleRoutingConnectionFactory routingFactory = new SimpleRoutingConnectionFactory();
        ConnectionFactory defaultTargetConnectionFactory = rabbitConnectionFactory(config);
        routingFactory.setDefaultTargetConnectionFactory(defaultTargetConnectionFactory);
        Map<Object, ConnectionFactory> targetFactories = build(config, multiConfig);
        routingFactory.setTargetConnectionFactories(targetFactories);
        
        // init multi listener container factory
        targetFactories.entrySet().forEach(entry -> {
            beanFactory.registerSingleton(String.valueOf(entry.getKey()),
                    rabbitListenerContainerFactory(configurer, entry.getValue()));
        });
        return routingFactory;
    }

    private Map<Object, ConnectionFactory> build(RabbitProperties config, MultiRabbitProperties multiConfig) {
        Map<Object, ConnectionFactory> map = new HashMap<>();
        multiConfig.getMulti().entrySet().forEach(entry -> {
            RabbitProperties clone = new RabbitProperties();
            String[] ignoredProperties = getIgnorePropertyNames(entry.getValue(), clone);
            BeanUtils.copyProperties(config, clone);
            BeanUtils.copyProperties(entry.getValue(), clone, ignoredProperties);
            map.put(entry.getKey(), rabbitConnectionFactory(clone));
        });
        return map;
    }

    private static String[] getIgnorePropertyNames(Object source, Object base) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        final BeanWrapper bs = new BeanWrapperImpl(base);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            Object baseValue = bs.getPropertyValue(pd.getName());
            if (Objects.equals(srcValue, baseValue)) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    private SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    private CachingConnectionFactory rabbitConnectionFactory(RabbitProperties config) {
        try {
            RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
            if (config.determineHost() != null) {
                factory.setHost(config.determineHost());
            }
            factory.setPort(config.determinePort());
            if (config.determineUsername() != null) {
                factory.setUsername(config.determineUsername());
            }
            if (config.determinePassword() != null) {
                factory.setPassword(config.determinePassword());
            }
            if (config.determineVirtualHost() != null) {
                factory.setVirtualHost(config.determineVirtualHost());
            }
            if (config.getRequestedHeartbeat() != null) {
                factory.setRequestedHeartbeat(config.getRequestedHeartbeat());
            }
            RabbitProperties.Ssl ssl = config.getSsl();
            if (ssl.isEnabled()) {
                factory.setUseSSL(true);
                if (ssl.getAlgorithm() != null) {
                    factory.setSslAlgorithm(ssl.getAlgorithm());
                }
                factory.setKeyStore(ssl.getKeyStore());
                factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
                factory.setTrustStore(ssl.getTrustStore());
                factory.setTrustStorePassphrase(ssl.getTrustStorePassword());
            }
            if (config.getConnectionTimeout() != null) {
                factory.setConnectionTimeout(config.getConnectionTimeout());
            }
            factory.afterPropertiesSet();
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(factory.getObject());
            connectionFactory.setAddresses(config.determineAddresses());
            connectionFactory.setPublisherConfirms(config.isPublisherConfirms());
            connectionFactory.setPublisherReturns(config.isPublisherReturns());
            if (config.getCache().getChannel().getSize() != null) {
                connectionFactory.setChannelCacheSize(config.getCache().getChannel().getSize());
            }
            if (config.getCache().getConnection().getMode() != null) {
                connectionFactory.setCacheMode(config.getCache().getConnection().getMode());
            }
            if (config.getCache().getConnection().getSize() != null) {
                connectionFactory.setConnectionCacheSize(config.getCache().getConnection().getSize());
            }
            if (config.getCache().getChannel().getCheckoutTimeout() != null) {
                connectionFactory.setChannelCheckoutTimeout(config.getCache().getChannel().getCheckoutTimeout());
            }
            return connectionFactory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
