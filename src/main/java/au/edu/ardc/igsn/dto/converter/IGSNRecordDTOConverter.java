package au.edu.ardc.igsn.dto.converter;
import au.edu.ardc.igsn.config.ApplicationProperties;
import au.edu.ardc.igsn.dto.IGSNRecordDTO;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.IdentifierService;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;

import java.net.URI;

public class IGSNRecordDTOConverter extends Converter<RecordDTO, IGSNRecordDTO>{

    IdentifierService identifierService;
    ApplicationProperties applicationProperties;

    public IGSNRecordDTOConverter(IdentifierService identifierService, ApplicationProperties applicationProperties) {
        this.identifierService = identifierService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected IGSNRecordDTO doForward(RecordDTO recordDTO) {
        ModelMapper mapper = new ModelMapper();
        IGSNRecordDTO dto = mapper.map(recordDTO, IGSNRecordDTO.class);
        Identifier igsn = identifierService.findIGSNByRecord(new Record(dto.getId()));

        // there's no igsn for this record
        if (igsn == null) {
            return dto;
        }

        // there's igsn, set the value and the url
        dto.setIgsn(igsn.getValue());
        try {
            String portalBaseUrl = applicationProperties.getPortalUrl();
            String inputUrl = String.format("%s/view/%s", portalBaseUrl, dto.getIgsn());
            String normalizedUrl = new URI(inputUrl).normalize().toString();
            dto.setPortalUrl(normalizedUrl);
        } catch (Exception e) {
            // todo log URI creation exception
            return dto;
        }

        return dto;

    }

    @Override
    protected RecordDTO doBackward(IGSNRecordDTO igsnRecordDTO) {
        return (new ModelMapper()).map(igsnRecordDTO, RecordDTO.class);
    }
}
