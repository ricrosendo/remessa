package br.com.inter.client;

import br.com.inter.dto.PtaxQuotationResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;

@Client("${clients.ptax.url}")
public interface PtaxClient {

    @Get("/CotacaoDolarDia(dataCotacao=@dataCotacao)")
    PtaxQuotationResponse findDollarQuotation(
            @QueryValue("@dataCotacao") String quotationDate,
            @QueryValue("$top") int top,
            @QueryValue("$format") String format
    );
}
