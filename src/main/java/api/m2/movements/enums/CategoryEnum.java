package api.m2.movements.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // Gen
public enum CategoryEnum {
    SIN_CATEGORIA("Sin Categoria"),
    HOGAR("Hogar"),
    REGALOS("Regalos"),
    RESTAURANTE("Restaurante"),
    ROPA("Ropa"),
    SERVICIOS("Servicios"),
    STREAMING("Streaming"),
    SUPERMERCADO("Supermercado"),
    TECNOLOGIA("Tecnologia"),
    TRANSPORTE("Transporte"),
    VIAJE("Viaje");
    private final String descripcion;
}