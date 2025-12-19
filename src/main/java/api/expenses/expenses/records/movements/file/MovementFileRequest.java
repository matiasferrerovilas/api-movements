package api.expenses.expenses.records.movements.file;

import api.expenses.expenses.enums.BanksEnum;

import java.time.LocalDateTime;

public record MovementFileRequest(String extractedText,
                                  BanksEnum bank,
                                  String group,
                                  Long userId,
                                  LocalDateTime timestamp
) { }
