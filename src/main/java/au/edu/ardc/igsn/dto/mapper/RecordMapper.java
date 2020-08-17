package au.edu.ardc.igsn.dto.mapper;

import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecordMapper {

    @Autowired
    ModelMapper modelMapper;

    public Record convertToEntity(RecordDTO dto) {
        return modelMapper.map(dto, Record.class);
    }

    public RecordDTO convertToDTO(Record record) {
        return modelMapper.map(record, RecordDTO.class);
    }

}
