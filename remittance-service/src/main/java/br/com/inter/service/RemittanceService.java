package br.com.inter.service;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.RemittanceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RemittanceService {

    RemittanceResponse create(CreateRemittanceRequest request);

    List<RemittanceResponse> list(LocalDate startDate, LocalDate endDate, UUID userId);
}
