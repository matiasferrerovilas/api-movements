package api.m2.movements.movements.records.movements.file;

import java.time.LocalDateTime;

public record MovementFileRequest(String extractedText,
                                  String bank,
                                  String group,
                                  Long userId,
                                  LocalDateTime timestamp
) { }
