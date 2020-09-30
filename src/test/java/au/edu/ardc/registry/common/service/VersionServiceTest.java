package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.dto.VersionDTO;
import au.edu.ardc.registry.common.dto.mapper.VersionMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.repository.RecordRepository;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.exception.*;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.VersionRepository;
import au.edu.ardc.registry.common.repository.specs.VersionSpecification;
import au.edu.ardc.registry.oai.service.OAIPMHService;
import org.apache.commons.codec.digest.DigestUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import au.edu.ardc.registry.TestHelper;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.entity.Record;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VersionService.class, SchemaService.class, VersionMapper.class, ModelMapper.class })
public class VersionServiceTest {

	@Autowired
	@Qualifier("VersionService")
	VersionService service;

	@MockBean
	RecordService recordService;

	@MockBean
	VersionRepository repository;

	@MockBean
	ValidationService validationService;

	@MockBean
	private RecordRepository recordRepository;

	@Test
	@DisplayName("when creating with a valid request, a DTO is returned properly")
	public void create_ValidRequest_returnsDTO() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// given a version dto
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		// setup repository mock
		Version expected = TestHelper.mockVersion(record.getId());
		when(recordService.exists(anyString())).thenReturn(true);
		when(recordService.findById(anyString())).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
		when(repository.save(any(Version.class))).thenReturn(expected);

		// when the service creates the version, verify the save method is called
		VersionDTO resultDTO = service.create(dto, user);
		assertThat(resultDTO).isNotNull();
		assertThat(resultDTO).isInstanceOf(VersionDTO.class);
		verify(repository, times(1)).save(any(Version.class));
	}

	@Test
	public void create_HashAlreadyExists_throwsException() {
		// given a creation request
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		// setup repository mock
		Version expected = TestHelper.mockVersion(record.getId());
		when(recordService.exists(anyString())).thenReturn(true);
		when(recordService.findById(anyString())).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
		when(repository.save(any(Version.class))).thenReturn(expected);

		// throws ForbiddenOpereationException if repository has
		// existsBySchemaAndHashAndCurrent
		when(repository.existsBySchemaAndHashAndCurrent(anyString(), anyString(), anyBoolean())).thenReturn(true);
		Assert.assertThrows(VersionContentAlreadyExisted.class, () -> {
			service.create(dto, user);
		});
	}

	@Test
	public void create_InvalidSchema_throwsException() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// given a version dto
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema("not-supported");
		dto.setContent("blah");

		// when the service creates the version, expects exception
		Assert.assertThrows(SchemaNotSupportedException.class, () -> {
			service.create(dto, user);
		});
	}

	@Test
	public void create_RecordNotFound_throwsException() {
		// given a version dto
		VersionDTO dto = new VersionDTO();
		dto.setRecord(UUID.randomUUID().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		when(recordService.exists(dto.getId())).thenReturn(false);

		// when the service creates the version, expects exception
		Assert.assertThrows(RecordNotFoundException.class, () -> {
			service.create(dto, TestHelper.mockUser());
		});
	}

	@Test
	void create_failValidateRecordOwnership_throwsException() {
		// given a user & record that doesn't belong to that user
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());

		// given a version dto for that record
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		// setup the world
		when(recordService.exists(record.getId().toString())).thenReturn(true);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			service.create(dto, user);
		});
	}

	@Test
	void delete_VersionNotFound_throwException() {
		Assert.assertThrows(VersionNotFoundException.class, () -> {
			service.delete(UUID.randomUUID().toString(), TestHelper.mockUser());
		});
	}

	@Test
	void delete_VersionNotOwned_throwsException() {
		// given a record + version
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());
		Version version = TestHelper.mockVersion(record.getId());

		// mock repository
		when(repository.existsById(version.getId())).thenReturn(true);
		when(repository.findById(version.getId())).thenReturn(Optional.of(version));
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

		// throws ForbiddenOperationException
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			service.delete(version.getId().toString(), TestHelper.mockUser());
		});
	}

	@Test
	void delete_ValidRequest_returnsTrue() {
		// given a record + version
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());
		Version version = TestHelper.mockVersion(record.getId());

		// mock repository
		when(repository.existsById(version.getId())).thenReturn(true);
		when(repository.findById(version.getId())).thenReturn(Optional.of(version));
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);

		// when delete, repository.deleteById is called
		service.delete(version.getId().toString(), TestHelper.mockUser());
		verify(repository, times(1)).deleteById(version.getId().toString());
	}

	@Test
	void getHash_ValidVersion_returnsHash() {
		Version version = TestHelper.mockVersion();
		version.setContent("random".getBytes());
		String actual = service.getHash(version);
		assertThat(actual).isEqualTo(DigestUtils.sha1Hex("random"));
	}

	@Test
	void findAllVersionsForRecord() {
		// given a record
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		// and 5 versions
		List<Version> mockResult = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Version version = TestHelper.mockVersion(record);
			mockResult.add(version);
		}

		// setup the world
		Page<Version> mockPage = new PageImpl(mockResult);
		when(repository.findAll(any(VersionSpecification.class), any(Pageable.class))).thenReturn(mockPage);

		// when findAllVersionsForRecord
		Page<VersionDTO> actual = service.findAllVersionsForRecord(record, PageRequest.of(0, 5));

		// is a valid Page<VersionDTO>
		assertThat(actual.getContent()).hasSize(10);
		assertThat(actual.getTotalElements()).isEqualTo(10);
		assertThat(actual.getTotalPages()).isEqualTo(1);

		// contains only RecordDTO
		long countOfVersionDTO = actual.stream().filter(t -> t instanceof VersionDTO).count();
		assertThat(countOfVersionDTO).isEqualTo(10);

		// repository is called
		verify(repository, times(1)).findAll(any(VersionSpecification.class), any(Pageable.class));
	}

	// todo update
	// todo end
	// todo findById
	// todo findOwned

	@Test
	public void end() {
		Version version = TestHelper.mockVersion();
		User user = TestHelper.mockUser();
		when(repository.save(any(Version.class))).thenReturn(version);

		Version endedVersion = service.end(version, user);

		// ensure the repository call save
		verify(repository, times(1)).save(version);

		assertThat(endedVersion.isCurrent()).isFalse();
		assertThat(endedVersion.getEndedAt()).isNotNull();
		assertThat(endedVersion.getEndedAt()).isInstanceOf(Date.class);
		assertThat(endedVersion.getEndedBy()).isEqualTo(user.getId());
	}

	@Test
	public void it_can_find_version_by_id() {
		UUID id = UUID.randomUUID();
		Version version = TestHelper.mockVersion(id);
		when(repository.findById(id)).thenReturn(Optional.of(version));

		Version actual = service.findById(id.toString());

		// ensure repository call findById
		verify(repository, times(1)).findById(any(UUID.class));

		assertThat(actual).isInstanceOf(Version.class);
	}

	@Test
	public void it_can_find_version_existence_by_id() {
		UUID id = UUID.randomUUID();
		when(repository.existsById(id)).thenReturn(true);

		assertThat(service.exists(id.toString())).isTrue();

		// ensure repository call findById
		verify(repository, times(1)).existsById(any(UUID.class));

		// false case
		assertThat(service.exists(UUID.randomUUID().toString())).isFalse();
	}

	@Test
	public void it_can_find_versions_existence_by_schema_and_visible_record() throws ParseException {

		String from = "2019-06-25";
		String until = "2020-10-28";

		Date fromDate = convertDate(from);
		Date untilDate = convertDate(until);

		for (int i = 0; i < 10; i++) {
			Record record = TestHelper.mockRecord(UUID.randomUUID());
			record.setVisible(true);
			record.setModifiedAt(fromDate);
			recordRepository.saveAndFlush(record);

			String created = "2019-07-25";
			Date createdAtDate = convertDate(created);
			System.out.println(createdAtDate);
			Version version = TestHelper.mockVersion(record);
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(createdAtDate);
			version.setCurrent(true);
			repository.saveAndFlush(version);
		}

		// ensure repository call findAllCurrentVersionsOfSchema

		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("schema", SchemaService.ARDCv1, SearchOperation.EQUAL));
		specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
		specs.add(new SearchCriteria("createdAt", fromDate,  SearchOperation.DATE_GREATER_THAN_EQUAL));
		//specs.add(new SearchCriteria("createdAt", untilDate,  SearchOperation.DATE_LESS_THAN_EQUAL));
		Page<Version> versions = service.searchVersions(specs,PageRequest.of(0, 5));
		System.out.print(versions);
		Page<Version> actual = service.findAllCurrentVersionsOfSchema(SchemaService.ARDCv1, fromDate, null, PageRequest.of(0, 5));
		System.out.print(actual);
		// is a valid Page<Version>
		// Assertions.assertThat(actual.getContent()).hasSize(1);
		// Assertions.assertThat(actual.getTotalElements()).isEqualTo(1);
		// Assertions.assertThat(actual.getTotalPages()).isEqualTo(1);

	}

	public Date convertDate(String inputDate){

		try {
			if(inputDate. indexOf('T')>0){
				LocalDateTime parsedDate = LocalDateTime.parse(inputDate, DateTimeFormatter.ISO_DATE_TIME);
				Date out = Date.from(parsedDate.atZone(ZoneId.systemDefault()).toInstant());
				return out;
			}else{
				LocalDateTime parsedDate = LocalDate.parse(inputDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
				Date out = Date.from(parsedDate.atZone(ZoneId.systemDefault()).toInstant());
				return out;
			}
		}
		catch(Exception e){
			return null;
		}
	}

}