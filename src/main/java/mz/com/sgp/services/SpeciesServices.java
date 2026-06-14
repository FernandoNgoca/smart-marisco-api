package mz.com.sgp.services;

import static mz.com.sgp.mapper.ObjectMapper.parseListObjects;
import static mz.com.sgp.mapper.ObjectMapper.parseObject;

import java.util.List;

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

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.controllers.SpeciesController;
import mz.com.sgp.data.dto.SpeciesDTO;
import mz.com.sgp.exception.ResourceNotFoundException;
import mz.com.sgp.model.SpeciesEntity;
import mz.com.sgp.repository.SpeciesRepository;

@Service
public class SpeciesServices {

	private Logger logger = LoggerFactory.getLogger(CategoryServices.class.getName());

	@Autowired
	SpeciesRepository speciesRepository;

	@Autowired
	PagedResourcesAssembler<SpeciesDTO> assembler;

	public PagedModel<EntityModel<SpeciesDTO>> findAll(Pageable pageable, String search) {

		Page<SpeciesEntity> species;

		if (search != null && !search.isBlank()) {
			species = speciesRepository.search(search.toLowerCase(), EntityState.ACTIVE, pageable);
		} else {
			species = speciesRepository.findAll(pageable, EntityState.ACTIVE);
		}

		return buildPagedModel(pageable, species, search);
	}

	public SpeciesDTO create(SpeciesDTO species) {

		logger.info("Foi criado uma espécie!");

		var entity = parseObject(species, SpeciesEntity.class);

		var dto = parseObject(speciesRepository.save(entity), SpeciesDTO.class);
		// addHateoasLinks(dto);
		return dto;
	}

	public SpeciesDTO update(SpeciesDTO species) {

		logger.info("Atualizando Espécie!");
		SpeciesEntity entity = speciesRepository.findById(species.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Não encotrado nenhuma Espécie!"));

		entity.setName(species.getName());
		entity.setDescription(species.getDescription());

		return parseObject(speciesRepository.save(entity), SpeciesDTO.class);
	}
	
	public List<SpeciesDTO> findByCategoryId(Long id) {
	    logger.info("Procurando espécies pela categoria!");

	    List<SpeciesEntity> entities = speciesRepository.findByCategoryId(id);

	    if (entities.isEmpty()) {
	        throw new ResourceNotFoundException(
	            "Não foi encontrada nenhuma espécie para a categoria informada!"
	        );
	    }

	    return parseListObjects(entities, SpeciesDTO.class);
	}

	private PagedModel<EntityModel<SpeciesDTO>> buildPagedModel(Pageable pageable, Page<SpeciesEntity> speciesEntity,
			String search) {

		var products = speciesEntity.map(p -> {
			var dto = parseObject(p, SpeciesDTO.class);
			return dto;
		});

		// Extrair sort corretamente
		String sortField = pageable.getSort().stream().findFirst().map(order -> order.getProperty()).orElse("name");

		String direction = pageable.getSort().stream().findFirst()
				.map(order -> order.getDirection().name().toLowerCase()).orElse("asc");

		Link findAllLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpeciesController.class)
				.findAll(pageable.getPageNumber(), pageable.getPageSize(), direction, sortField, search)).withSelfRel();

		return assembler.toModel(products, findAllLink);
	}

}
