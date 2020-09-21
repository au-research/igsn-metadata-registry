package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.oai.exception.BadVerbException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Base64;

public class OAIProvider {

	@Autowired
	SchemaService schemaService;

	public String getNamespace(Schema schema) {
		return schema.getNamespace();
	}

	public String getPrefix(Schema schema) {
		return schema.getId();
	}

	public String getFormatSchema(Schema schema) {
		return schema.getSchemaLocation();
	}

	public static String getResumptionToken(String resumptionToken, Integer pageSize) throws JsonProcessingException {
		String newResumptionToken = "";
		ObjectMapper objectMapper = new ObjectMapper();
		if(resumptionToken!=null) {
			try {
				byte[] decodedBytes = Base64.getDecoder().decode(resumptionToken);
				String resumptionDecoded = new String(decodedBytes);
				JsonNode jsonNode = objectMapper.readTree(resumptionDecoded);
				Pageable newPageable = PageRequest.of(jsonNode.get("pageNumber").asInt()+1, pageSize);
				String newPageableAsString = objectMapper.writeValueAsString(newPageable);
				newResumptionToken = Base64.getEncoder().encodeToString(newPageableAsString.getBytes());
			}
			catch(Exception e){
				throw new BadVerbException("The value of the resumptionToken argument is invalid or expired",
						"badResumptionToken");
			}
		}else{
			Pageable newPageable = PageRequest.of(1, pageSize);
			String newPageableAsString = objectMapper.writeValueAsString(newPageable);
			newResumptionToken = Base64.getEncoder().encodeToString(newPageableAsString.getBytes());
		}
		return newResumptionToken;
	}

	public static int getCursor(String resumptionToken, int pageSize){
		ObjectMapper objectMapper = new ObjectMapper();
		int cursor = pageSize;
		if(resumptionToken!=null) {
			try {
				byte[] decodedBytes = Base64.getDecoder().decode(resumptionToken);
				String resumptionDecoded = new String(decodedBytes);
				JsonNode jsonNode = objectMapper.readTree(resumptionDecoded);
				cursor = (jsonNode.get("pageNumber").asInt() + 1) * pageSize;
			}
			catch(Exception e){
				throw new BadVerbException("The value of the resumptionToken argument is invalid or expired",
						"badResumptionToken");
			}
		}
		return cursor;
	}

	public static Pageable getPageable(String resumptionToken, int pageSize){
		ObjectMapper objectMapper = new ObjectMapper();
		Pageable pageable = PageRequest.of(1, pageSize);
		if(resumptionToken!=null) {
			try {
				byte[] decodedBytes = Base64.getDecoder().decode(resumptionToken);
				String resumptionDecoded = new String(decodedBytes);
				JsonNode jsonNode = objectMapper.readTree(resumptionDecoded);
				pageable = PageRequest.of(jsonNode.get("pageNumber").asInt(), pageSize);

			}
			catch(Exception e){
				throw new BadVerbException("The value of the resumptionToken argument is invalid or expired",
						"badResumptionToken");
			}
		}
		return pageable;
	}
}
