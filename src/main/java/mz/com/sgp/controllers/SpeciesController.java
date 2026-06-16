package mz.com.sgp.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import mz.com.sgp.data.dto.SpeciesDTO;
import mz.com.sgp.services.SpeciesServices;

@RestController
@RequestMapping("api/species/v1")
@Tag(name = "Species", description = "Endpoints for Managing Species")
public class SpeciesController {

	@Autowired
	SpeciesServices speciesServices;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PagedModel<EntityModel<SpeciesDTO>>> findAll(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "12") Integer size,
			@RequestParam(value = "direction", defaultValue = "asc") String direction,
			@RequestParam(value = "sortField", defaultValue = "name") String sortField,
			@RequestParam(value = "search", required = false) String search) {
		var sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

		Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

		return ResponseEntity.ok(speciesServices.findAll(pageable, search));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public SpeciesDTO create(@RequestBody SpeciesDTO species) {
		return speciesServices.create(species);
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public SpeciesDTO update(@RequestBody SpeciesDTO species) {
		return speciesServices.update(species);
	}
	
	@GetMapping(value = "findByCategoryId/{id}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
			MediaType.APPLICATION_YAML_VALUE })
	public List<SpeciesDTO> findByCategoryId(@PathVariable("id") Long id) {
		return speciesServices.findByCategoryId(id);
	}

}
