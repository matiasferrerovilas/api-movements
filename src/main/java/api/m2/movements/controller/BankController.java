package api.m2.movements.controller;

import api.m2.movements.records.banks.AddBankRequest;
import api.m2.movements.records.banks.BankRecord;
import api.m2.movements.services.banks.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/banks")
@Tag(name = "Bancos", description = "API para la gestión de bancos del usuario")
public class BankController {

    private final BankService bankService;

    @Operation(
        summary = "Obtener bancos del usuario",
        description = "Recupera la lista de bancos asociados al usuario autenticado",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Lista de bancos del usuario",
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

    @Operation(
        summary = "Agregar banco al usuario",
        description = "Agrega un banco a la lista del usuario. Si el banco no existe en el catálogo, "
            + "lo crea automáticamente. La descripción se sanitiza (trim + uppercase) antes de buscar o crear.",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Banco agregado correctamente",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BankRecord.class)
                )
            )
        }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankRecord addBank(@RequestBody AddBankRequest request) {
        return bankService.addBankToUser(request.description());
    }

    @Operation(
        summary = "Quitar banco del usuario",
        description = "Elimina un banco de la lista del usuario. El banco sigue existiendo en el catálogo global.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Banco eliminado de la lista del usuario"),
            @ApiResponse(responseCode = "404", description = "El banco no está en la lista del usuario")
        }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBank(@PathVariable Long id) {
        bankService.removeBankFromUser(id);
    }
}
