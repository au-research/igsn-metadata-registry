package au.edu.ardc.igsn.dto;

import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.service.RecordService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class VersionMapper {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    RecordService recordService;

    public Version convertToEntity(VersionDTO versionDTO) {
        Version version = modelMapper.map(versionDTO, Version.class);
        if (versionDTO.getRecord() != null) {
            version.setRecord(recordService.findById(versionDTO.getRecord()));
        }
        version.setContent(Base64.getDecoder().decode(versionDTO.getContent()));
        return version;
    }

    public VersionDTO convertToDTO(Version version) {
        VersionDTO versionDTO = modelMapper.map(version, VersionDTO.class);
        versionDTO.setRecord(version.getRecord().getId().toString());
        if (versionDTO.getId() == null && version.getId() != null) {
            versionDTO.setId(version.getId().toString());
        }
        versionDTO.setContent(null);
        // todo hide the content but still store it somehow
//        versionDTO.setContent(new String(version.getContent()));

        return versionDTO;
    }
}
