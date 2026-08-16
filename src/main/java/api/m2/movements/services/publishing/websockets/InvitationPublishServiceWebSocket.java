package api.m2.movements.services.publishing.websockets;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.events.InvitationReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static api.m2.movements.configuration.RabbitConfig.QUEUE_INVITATION_RECEIVED;

/**
 * Consumes the invitation-sent event published by api-identity (see {@code RabbitConfig}) and
 * pushes it over STOMP so the invited user sees it live instead of having to refresh
 * GET /v1/workspace/invitations. Addressed by email since that's the only identifier the event
 * carries and it's already how the authenticated principal is resolved elsewhere in this backend.
 */
@Slf4j
@Service
public class InvitationPublishServiceWebSocket extends WebSocketMessageService {

    public InvitationPublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @RabbitListener(queues = QUEUE_INVITATION_RECEIVED)
    public void onInvitationReceived(InvitationReceivedEvent event) {
        log.debug("Invitación recibida desde api-identity para {}", event.invitedUserEmail());
        this.publish(event, WebSocketTopics.invitationsNew(event.invitedUserEmail()), EventType.INVITATION_ADDED);
    }
}
