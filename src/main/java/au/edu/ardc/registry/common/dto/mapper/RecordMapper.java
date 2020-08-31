package au.edu.ardc.registry.common.dto.mapper;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.VersionDTO;
import au.edu.ardc.registry.common.entity.Record;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordMapper {

    @Autowired
    ModelMapper modelMapper;

    public Record convertToEntity(RecordDTO dto) {
        return modelMapper.map(dto, Record.class);
    }

    public RecordDTO convertToDTO(Record record) {
        RecordDTO dto = modelMapper.map(record, RecordDTO.class);
        List<IdentifierDTO> identifiers = record.getIdentifiers().stream()
                .map(identifier -> modelMapper.map(identifier, IdentifierDTO.class))
                .collect(Collectors.toList());
        dto.setIdentifiers(identifiers);

        List<VersionDTO> currentVersions = record.getCurrentVersions().stream()
                .map(version -> modelMapper.map(version, VersionDTO.class))
                .peek(versionDTO -> versionDTO.setContent(null))
                .peek(versionDTO -> versionDTO.setRecord(null))
                .collect(Collectors.toList());
        dto.setCurrentVersions(currentVersions);
        return dto;
    }

}
