package api.m2.movements.records.movements.file;

import api.m2.movements.enums.BanksEnum;

import java.time.LocalDateTime;

public record MovementFileRequest(String extractedText,
                                  BanksEnum bank,
                                  String group,
                                  Long userId,
                                  LocalDateTime timestamp
) { }
