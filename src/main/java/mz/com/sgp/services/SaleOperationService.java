package mz.com.sgp.services;

import java.security.MessageDigest;
import java.util.HexFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import mz.com.sgp.data.dto.SaleDTO;
import mz.com.sgp.data.dto.SaleRequestDTO;
import mz.com.sgp.model.SaleOperationEntity;
import mz.com.sgp.repository.SaleOperationRepository;
import mz.com.sgp.repository.UserRepository;
import mz.com.sgp.security.jwt.JwtTokenProvider;

@Service
public class SaleOperationService {
    private final UserRepository users;
    private final SaleOperationRepository operations;
    private final SaleServices sales;
    private final ObjectMapper mapper;

    public SaleOperationService(UserRepository users, SaleOperationRepository operations,
            SaleServices sales, ObjectMapper mapper) {
        this.users = users;
        this.operations = operations;
        this.sales = sales;
        this.mapper = mapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SaleDTO create(String username, String key, SaleRequestDTO request) {
        if (key == null || !key.matches("[A-Za-z0-9-]{16,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de operação inválido");
        }
        if (request == null || request.getSale() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adicione pelo menos um artigo");
        }
        if (request.getItems().stream().anyMatch(item -> item == null || item.getProductId() == null
                || item.getQuantity() == null || item.getQuantity().signum() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artigo ou quantidade inválidos");
        }
        // Serializa operações do mesmo utilizador, inclusive entre instâncias da API.
        // A venda, os movimentos e o comprovativo de operação partilham a mesma transação.
        var user = users.findForUpdateByUsername(username);
        JwtTokenProvider.requireActive(user);
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsBytes(request)));
            var previous = operations.findByUserIdAndRequestKey(user.getId(), key);
            if (previous.isPresent()) {
                if (!previous.get().getRequestHash().equals(hash)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Este identificador já foi usado com dados diferentes");
                }
                return mapper.readValue(previous.get().getResponseJson(), SaleDTO.class);
            }
            var result = sales.create(request.getSale(), request.getItems());
            operations.save(new SaleOperationEntity(user.getId(), key, hash, mapper.writeValueAsString(result)));
            return result;
        } catch (JsonProcessingException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Não foi possível registar a operação", e);
        }
    }
}
