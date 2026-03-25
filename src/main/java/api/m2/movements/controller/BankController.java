package api.m2.movements.controller;

import api.m2.movements.records.banks.BankRecord;
import api.m2.movements.services.banks.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/banks")
@Tag(name = "Bancos", description = "API para la gestión de bancos")
public class BankController {

    private final BankService bankService;

    @Operation(
        summary = "Obtener todos los bancos",
        description = "Recupera una lista de todos los bancos disponibles",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Lista de bancos encontrados",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = BankRecord.class))
                )
            )
        }
    )
    @GetMapping
    public List<BankRecord> getAllBanks() {
        return bankService.getAllBanks();
    }
}
