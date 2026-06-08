package br.com.inter.controller;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.service.RemittanceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.validation.Valid;

@Controller("/remittances")
public class RemittanceController {

    private final RemittanceService remittanceService;

    public RemittanceController(RemittanceService remittanceService) {
        this.remittanceService = remittanceService;
    }

    @Post
    public HttpResponse<RemittanceResponse> create(@Body @Valid CreateRemittanceRequest request) {
        return HttpResponse.created(remittanceService.create(request));
    }
}
