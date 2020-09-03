package au.edu.ardc.registry.common.dto.mapper;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.service.RecordService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdentifierMapper {

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	RecordService recordService;

	public Identifier convertToEntity(IdentifierDTO dto) {
		Identifier entity = modelMapper.map(dto, Identifier.class);
		if (dto.getRecord() != null) {
			entity.setRecord(recordService.findById(dto.getRecord().toString()));
		}
		return entity;
	}

	public IdentifierDTO convertToDTO(Identifier entity) {
		IdentifierDTO dto = modelMapper.map(entity, IdentifierDTO.class);
		dto.setRecord(entity.getRecord().getId());
		return dto;
	}

}
