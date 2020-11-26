package au.edu.ardc.registry.igsn.dto.mapper;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.entity.Embargo;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.provider.StatusProvider;
import au.edu.ardc.registry.common.service.EmbargoService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.igsn.config.IGSNApplicationConfig;
import au.edu.ardc.registry.igsn.dto.IGSNRecordDTO;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNRecordMapper {

	final ModelMapper modelMapper;

	final IGSNApplicationConfig igsnApplicationConfig;

	final EmbargoService embargoService;

	final SchemaService schemaService;

	protected Converter<Record, IGSNRecordDTO> converter;

	public IGSNRecordMapper(ModelMapper modelMapper, IGSNApplicationConfig igsnApplicationConfig,
							EmbargoService embargoService, SchemaService schemaService) {
		this.modelMapper = modelMapper;
		this.igsnApplicationConfig = igsnApplicationConfig;
		this.embargoService = embargoService;
		this.schemaService = schemaService;
	}

	@PostConstruct
	public void init() {
		this.converter = buildConverter();
	}

	public Converter<Record, IGSNRecordDTO> getConverter() {
		return converter;
	}

	private Converter<Record, IGSNRecordDTO> buildConverter() {
		return new Converter<Record, IGSNRecordDTO>() {
			@Override
			protected IGSNRecordDTO doForward(Record record) {
				IGSNRecordDTO dto = modelMapper.map(record, IGSNRecordDTO.class);

				//dto.setCurrentVersions(null);
				//dto.setIdentifiers(null);

				// set IGSN
				IdentifierDTO igsn = record.getIdentifiers().stream()
						.filter((identifier -> identifier.getType().equals(Identifier.Type.IGSN)))
						.map(identifier -> modelMapper.map(identifier, IdentifierDTO.class)).findFirst().orElse(null);
				dto.setIgsn(igsn);

				// set Embargo
				Embargo embargo = embargoService.findByRecord(record);
				if (embargo != null) {
					dto.setEmbargoDate(embargo.getEmbargoEnd());
				}

				// set Status
				Schema schema = schemaService.getSchemaByID(SchemaService.ARDCv1);
				StatusProvider provider = (StatusProvider) MetadataProviderFactory.create(schema, Metadata.Status);
				String status = provider.get(record);
				dto.setStatus(status);

				// set portalUrl
				String portalBaseUrl = igsnApplicationConfig.getPortalUrl() != null
						? igsnApplicationConfig.getPortalUrl().replaceAll("/$", "") : "";
				if (igsn != null) {
					dto.setPortalUrl(String.format("%s/view/%s", portalBaseUrl, igsn.getValue()));
				}

				return dto;
			}

			@Override
			protected Record doBackward(IGSNRecordDTO igsnRecordDTO) {
				return modelMapper.map(igsnRecordDTO, Record.class);
			}
		};
	}

}
