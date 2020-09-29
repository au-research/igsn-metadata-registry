package au.edu.ardc.registry.common.dto.mapper;

import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class RequestMapper {

    final ModelMapper modelMapper;

    protected Converter<Request, RequestDTO> converter;

    public RequestMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        converter = buildConverter();
    }

    public Converter<Request, RequestDTO> getConverter() {
        return converter;
    }

    public RequestDTO convertToDTO(Request request) {
        return converter.convert(request);
    }

    private Converter<Request, RequestDTO> buildConverter() {
        return new Converter<Request, RequestDTO>() {
            @Override
            protected RequestDTO doForward(Request request) {
                return modelMapper.map(request, RequestDTO.class);
            }

            @Override
            protected Request doBackward(RequestDTO requestDTO) {
                return modelMapper.map(requestDTO, Request.class);
            }
        };
    }

}
