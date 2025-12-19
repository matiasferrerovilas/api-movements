package api.expenses.expenses.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Configuration
public class RabbitConfig {
    public static final String AMQ_TOPIC_EXCHANGE = "movement.topic";
    public static final String QUEUE_MOVEMENT_FILE_IMPORTED = "n8n.import.file.finished";
    private static final String ROUTING_KEY = "n8n.import.file.finished";

    @Bean
    public JacksonJsonMessageConverter jackson2JsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        factory.setAfterReceivePostProcessors(message -> {
            var props = message.getMessageProperties();

            Optional.of(props.getContentType())
                    .orElseGet(() -> {
                        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return null;
                    });

            Optional.of(props.getPriority())
                    .orElseGet(() -> {
                        props.setPriority(0);
                        return null;
                    });

            return message;
        });

        return factory;
    }
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(AMQ_TOPIC_EXCHANGE);
    }
    @Bean
    Queue queue() {
        return QueueBuilder.durable(QUEUE_MOVEMENT_FILE_IMPORTED).build();
    }
    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ROUTING_KEY);
    }
    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
