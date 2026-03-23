package api.m2.movements.services.groups;

import api.m2.movements.mappers.MembershipMapper;
import api.m2.movements.projections.MembershipSummaryProjection;
import api.m2.movements.repositories.MembershipRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final UserService userService;
    private final MembershipMapper membershipMapper;

    public List<MembershipSummaryProjection> getAllMemberships() {
        var user = userService.getAuthenticatedUserRecord();
        return membershipRepository.findAllByUserId(user.id());
    }
}