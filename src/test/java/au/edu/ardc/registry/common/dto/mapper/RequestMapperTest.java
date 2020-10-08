package au.edu.ardc.registry.common.dto.mapper;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.entity.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RequestMapper.class, ModelMapper.class })
class RequestMapperTest {

	@Autowired
	RequestMapper requestMapper;

	@Test
	void convertToEntity() {
		// given a dto, convert to a request retains data
		RequestDTO dto = new RequestDTO();
		dto.setType("igsn-import");

		Request actual = requestMapper.getConverter().reverse().convert(dto);

		assertThat(actual.getType()).isEqualTo("igsn-import");
	}

}