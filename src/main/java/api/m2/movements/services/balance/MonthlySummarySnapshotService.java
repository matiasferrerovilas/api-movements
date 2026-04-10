package api.m2.movements.services.balance;

import api.m2.movements.entities.MonthlySummarySnapshot;
import api.m2.movements.entities.User;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.repositories.MonthlySummarySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonthlySummarySnapshotService {

    private final MonthlySummarySnapshotRepository snapshotRepository;
    private final JsonMapper jsonMapper;

    public void save(User user, Integer year, Integer month, MonthlySummaryResponse summary) {
        String payload = this.serialize(summary);

        MonthlySummarySnapshot snapshot = snapshotRepository
                .findByUserAndYearAndMonth(user, year, month)
                .orElseGet(() -> MonthlySummarySnapshot.builder()
                        .user(user)
                        .year(year)
                        .month(month)
                        .build());

        snapshot.setPayload(payload);
        snapshotRepository.save(snapshot);
        log.info("Snapshot guardado para userId={} year={} month={}", user.getId(), year, month);
    }

    public Optional<MonthlySummaryResponse> find(User user, Integer year, Integer month) {
        return snapshotRepository.findByUserAndYearAndMonth(user, year, month)
                .map(s -> this.deserialize(s.getPayload()));
    }

    private String serialize(MonthlySummaryResponse summary) {
        try {
            return jsonMapper.writeValueAsString(summary);
        } catch (JacksonException e) {
            throw new BusinessException("Error serializando el resumen mensual: " + e.getMessage());
        }
    }

    private MonthlySummaryResponse deserialize(String payload) {
        try {
            return jsonMapper.readValue(payload, MonthlySummaryResponse.class);
        } catch (JacksonException e) {
            throw new BusinessException("Error deserializando el resumen mensual: " + e.getMessage());
        }
    }
}
