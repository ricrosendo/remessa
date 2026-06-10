package br.com.inter.controller;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.ErrorResponse;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.service.RemittanceService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller("/remittances")
@ExecuteOn(TaskExecutors.BLOCKING)
@Tag(name = "Remittances", description = "Resources for international remittance creation")
public class RemittanceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemittanceController.class);

    private final RemittanceService remittanceService;

    public RemittanceController(RemittanceService remittanceService) {
        this.remittanceService = remittanceService;
    }

    @Get
    @Operation(
            summary = "List international remittances",
            description = "Lists remittances optionally filtered by quotation date period and user id. The user id filter matches sender or receiver."
    )
    @ApiResponse(responseCode = "200", description = "Remittances listed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filters", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal error while listing remittances", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public HttpResponse<List<RemittanceResponse>> list(
            @Nullable @QueryValue LocalDate startDate,
            @Nullable @QueryValue LocalDate endDate,
            @Nullable @QueryValue UUID userId
    ) {
        LOGGER.info("Received request to list remittances. startDate={}, endDate={}, userId={}", startDate, endDate, userId);
        List<RemittanceResponse> response = remittanceService.list(startDate, endDate, userId);
        LOGGER.info("Remittance list request processed successfully. count={}", response.size());
        return HttpResponse.ok(response);
    }

    @Post
    @Operation(
            summary = "Create international remittance",
            description = "Creates a BRL remittance, retrieves the PTAX exchange rate, converts to USD, debits the sender, credits the receiver, and validates PF/PJ daily limits."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Remittance created successfully",
            content = @Content(schema = @Schema(implementation = RemittanceResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal error while processing the remittance", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public HttpResponse<RemittanceResponse> create(@Body @Valid CreateRemittanceRequest request) {
        LOGGER.info("Received request to create remittance. senderUserId={}, receiverUserId={}, quotationDate={}", request.senderUserId(), request.receiverUserId(), request.quotationDate());
        RemittanceResponse response = remittanceService.create(request);
        LOGGER.info("Remittance request processed successfully. senderUserId={}, receiverUserId={}, quotationDate={}", response.senderUserId(), response.receiverUserId(), response.quotationDate());
        return HttpResponse.created(response);
    }
}
