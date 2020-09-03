package au.edu.ardc.registry.common.dto.mapper;

import au.edu.ardc.registry.common.dto.URLDTO;
import au.edu.ardc.registry.common.entity.URL;
import au.edu.ardc.registry.common.service.RecordService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class URLMapper {

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	RecordService recordService;

	public URL convertToEntity(URLDTO urlDTO) {
		URL url = modelMapper.map(urlDTO, URL.class);
		if (urlDTO.getRecord() != null) {
			url.setRecord(recordService.findById(urlDTO.getRecord().toString()));
		}
		return url;
	}

	public URLDTO convertToDTO(URL url) {
		URLDTO urlDTO = modelMapper.map(url, URLDTO.class);
		urlDTO.setRecord(url.getRecord().getId());
		return urlDTO;
	}

}
