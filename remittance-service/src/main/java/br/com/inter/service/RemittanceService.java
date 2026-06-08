package br.com.inter.service;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.RemittanceResponse;

public interface RemittanceService {

    RemittanceResponse create(CreateRemittanceRequest request);
}
