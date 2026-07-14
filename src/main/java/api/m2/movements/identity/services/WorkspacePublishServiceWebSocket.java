package api.m2.movements.identity.services;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.movements.enums.EventType;
import api.m2.movements.identity.records.workspaces.WorkspaceDetail;
import api.m2.movements.movements.services.publishing.websockets.WebSocketMessageService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorkspacePublishServiceWebSocket extends WebSocketMessageService {

    public WorkspacePublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    public void publishWorkspaceMembershipUpdated(WorkspaceDetail workspaceDetail, String keycloakSubject) {
        this.publish(workspaceDetail, WebSocketTopics.workspacesDefault(keycloakSubject),
                EventType.MEMBERSHIP_UPDATED);
    }
}
