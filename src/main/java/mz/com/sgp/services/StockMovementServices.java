package mz.com.sgp.services;

import mz.com.sgp.validation.QuantityRules;

import static mz.com.sgp.mapper.ObjectMapper.parseObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.controllers.StockMovementController;
import mz.com.sgp.data.dto.StockDTO;
import mz.com.sgp.data.dto.StockMovementDTO;
import mz.com.sgp.exception.ResourceNotFoundException;
import mz.com.sgp.model.MovementType;
import mz.com.sgp.model.StockMovementEntity;
import mz.com.sgp.repository.StockMovementRepository;

@Service
public class StockMovementServices {

	private Logger logger = LoggerFactory.getLogger(StockMovementServices.class.getName());

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	PagedResourcesAssembler<StockMovementDTO> assembler;

	@Autowired
	private StockServices stockServices;

    @Autowired private mz.com.sgp.repository.StockRepository stocks;

	public PagedModel<EntityModel<StockMovementDTO>> findAll(Pageable pageable) {
		logger.info("A obter todos os Movimentos do Produto!");

		var stockMovement = stockMovementRepository.findAll(pageable);
		return buildPagedModel(pageable, stockMovement);
	}

	@Transactional
	public StockMovementDTO create(StockMovementDTO stockMovement) {

		logger.info("Foi adicionado um novo movimento: " + stockMovement);

        QuantityRules.positive(stockMovement == null ? null : stockMovement.getQuantity());
        if (stockMovement.getType() == null) throw new IllegalArgumentException("Tipo de movimento obrigatório");
        StockDTO stockDTO = parseObject(stocks.lockById(stockMovement.getStockId())
            .orElseThrow(() -> new ResourceNotFoundException("Stock não encontrado")), StockDTO.class);
        stockDTO.setQuantity(QuantityRules.balance(stockDTO.getQuantity(), stockMovement.getQuantity(),
                stockMovement.getType() == MovementType.ENTRY));

        var entity = parseObject(stockMovement, StockMovementEntity.class);
        var dto = parseObject(stockMovementRepository.save(entity), StockMovementDTO.class);
        this.stockServices.update(stockDTO);

		// addHateoasLinks(dto);
		return dto;
	}

	public StockMovementDTO update(StockMovementDTO stockMovement) {
        QuantityRules.positive(stockMovement == null ? null : stockMovement.getQuantity());

		logger.info("Atualizando Estoque!");
		StockMovementEntity entity = stockMovementRepository.findById(stockMovement.getId()).orElseThrow(
				() -> new ResourceNotFoundException("Não foi encontrado movimento de estoque para esse Id!"));

		entity.setType(stockMovement.getType());
		;
		entity.setStatus(stockMovement.getStatus());
		entity.setQuantity(stockMovement.getQuantity());

		return parseObject(stockMovementRepository.save(entity), StockMovementDTO.class);
	}

	public StockMovementDTO findByStockId(Long id) {
		logger.info("Procurar um Estoque com o id: " + id);

		var entity = stockMovementRepository.findByStockIdAndStatus(id, EntityState.ACTIVE);
		if (entity.isEmpty()) {
			throw new RuntimeException("Não foi encontrado movimento de estoque com o id: " + id);
		}

		var dto = parseObject(entity.get(0), StockMovementDTO.class);
		// addHateoasLinks(dto);
		return dto;
	}

	public PagedModel<EntityModel<StockMovementDTO>> findByStockIdAndStatus(Long productId, EntityState state,
			Pageable pageable) {

		logger.info("Procurar movimentos do produto com id: " + productId + " | Status: " + state);

		// 1. Buscar página do repository
		var entity = stockMovementRepository.findByStockIdAndStatus(productId, state, pageable);

		// 3. Construir PagedModel com HATEOAS
		return buildPagedModel(pageable, entity);
	}

	private PagedModel<EntityModel<StockMovementDTO>> buildPagedModel(Pageable pageable,
			Page<StockMovementEntity> stockMovement) {

		var stockMovementWithLinks = stockMovement.map(category -> {
			var dto = parseObject(category, StockMovementDTO.class);
			// addHateoasLinks(dto);
			return dto;
		});

		Link findAllLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(StockMovementController.class)
				.findAll(pageable.getPageNumber(), pageable.getPageSize(), String.valueOf(pageable.getSort())))
				.withSelfRel();
		return assembler.toModel(stockMovementWithLinks, findAllLink);
	}

    public record History(java.util.List<StockMovementDTO> items, long totalElements,
            long movements, java.math.BigDecimal entries, java.math.BigDecimal exits) {}

    public History history(Long productId, int page, int size, MovementType type,
            java.time.LocalDate from, java.time.LocalDate to) {
        org.springframework.data.jpa.domain.Specification<StockMovementEntity> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("stock").get("product").get("id"), productId));
            predicates.add(cb.equal(root.get("status"), EntityState.ACTIVE));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(root.get("createdDate"), to.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var results = stockMovementRepository.findAll(filter, org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate", "id")));
        var totals = stockMovementRepository.totals(productId);
        var dtos = results.getContent().stream().map(entity -> {
            var dto = parseObject(entity, StockMovementDTO.class);
            dto.setDescription(entity.getDescription());
            dto.setCreatedBy(entity.getCreatedBy());
            return dto;
        }).toList();
        return new History(dtos, results.getTotalElements(), totals.getMovements(), totals.getEntries(), totals.getExits());
    }
}
