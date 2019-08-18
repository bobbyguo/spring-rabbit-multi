package com.github.bobbyguo.springrabbitmulti.config;

import java.util.Map;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * multi rabbit properties config, using spring.rabbitmq.multi as the prefix,
 * the others are as same as spring.rabbitmq.
 * 
 * @author: bobby guo
 */
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class MultiRabbitProperties {

    private Map<String, RabbitProperties> multi;

    public Map<String, RabbitProperties> getMulti() {
        return multi;
    }

    public void setMulti(Map<String, RabbitProperties> multi) {
        this.multi = multi;
    }
}
