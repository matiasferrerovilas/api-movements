package api.m2.movements.services.publishing.websockets;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.events.MemberRemovedReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static api.m2.movements.configuration.RabbitConfig.QUEUE_MEMBER_REMOVED;

/**
 * Consumes the member-removed event published by api-identity when someone is kicked from a
 * workspace, and pushes it over STOMP so the removed user's UI reacts live (drop the workspace
 * from their list, redirect out of it if they're currently viewing it) instead of only finding
 * out the next time some unrelated request 404s.
 */
@Slf4j
@Service
public class WorkspaceMembershipPublishServiceWebSocket extends WebSocketMessageService {

    public WorkspaceMembershipPublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @RabbitListener(queues = QUEUE_MEMBER_REMOVED)
    public void onMemberRemoved(MemberRemovedReceivedEvent event) {
        log.debug("{} fue eliminado del workspace {} (por {})",
                event.removedUserEmail(), event.workspaceId(), event.removedByEmail());
        this.publish(event, WebSocketTopics.membershipRemoved(event.removedUserEmail()), EventType.WORKSPACE_LEFT);
    }
}
