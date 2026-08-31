package api.m2.movements.services.publishing.websockets;

import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO;
import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.enums.InvitationStatus;
import api.m2.movements.events.InvitationAcceptedReceivedEvent;
import api.m2.movements.events.InvitationReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static api.m2.movements.configuration.RabbitConfig.QUEUE_INVITATION_ACCEPTED;
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
        // El frontend cachea esto igual que la respuesta de GET /v1/workspace/invitations
        // (WorkspaceInvitationDTO: id/workspaceId/workspaceName/invitedByEmail/status/role/createdAt),
        // así que el payload debe tener esa misma forma en vez del RabbitMQ event crudo — de
        // lo contrario "id" llega undefined y el PATCH de aceptar/rechazar rompe en el backend.
        var invitationDTO = new WorkspaceInvitationDTO(
                event.invitationId(),
                event.workspaceId(),
                event.workspaceName(),
                event.invitedByEmail(),
                InvitationStatus.PENDING,
                event.role(),
                event.createdAt());
        this.publish(invitationDTO, WebSocketTopics.invitationsNew(event.invitedUserEmail()), EventType.INVITATION_ADDED);
    }

    /**
     * Alguien aceptó una invitación y se sumó a un workspace compartido — avisamos a quien tenga
     * ese workspace abierto para que refresque la lista de miembros, en vez de mostrar datos
     * desactualizados hasta el próximo refetch.
     */
    @RabbitListener(queues = QUEUE_INVITATION_ACCEPTED)
    public void onInvitationAccepted(InvitationAcceptedReceivedEvent event) {
        log.debug("Invitación {} aceptada por {} en workspace {}",
                event.invitationId(), event.acceptedByEmail(), event.workspaceId());
        this.publish(event, WebSocketTopics.workspaceMembersUpdate(event.workspaceId()), EventType.MEMBERSHIP_UPDATED);
    }
}
