package org.ekstep.jobs.samza.util;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.ekstep.common.dto.Response;
import org.ekstep.common.enums.TaxonomyErrorCodes;
import org.ekstep.common.exception.ResponseCode;
import org.ekstep.common.exception.ServerException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CreateResourceContent {

	private static final String KP_CS_BASE_URL = "http://11.2.6.6/content";
	private static final String KP_LEARNING_BASE_URL = "http://11.2.4.22:8080/learning-service";
	private static final String DEFAULT_CONTENT_TYPE = "application/json";
	private static final String CHANNEL_ID = "in.ekstep";
	private static final String FILE_URL = "https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/do_1130484729343590401758/artifact/test.pdf";

	public static void main(String[] args) throws Exception {
		for(int i =570; i<=1000; i++){
			String id = create(i);
			upload(id, getFile(id, FILE_URL));
			publish(id);
			System.out.println(id);
//			if (i % 100 == 0)
//				delay(30000);
		}

	}

	private static void delay(int time){
		try {
			Thread.sleep(time);
		}catch(Exception e){

		}
	}

	private static String create(int i) throws Exception {
		String contentId = "";
		final String id = "do_test_content_"+i;
		String url = KP_CS_BASE_URL + "/content/v3/create";
		Map<String, Object> request = new HashMap<String, Object>() {{
			put("request", new HashMap<String, Object>() {{
				put("content", new HashMap<String, Object>(){{
					put("identifier",id);
					put("name", "G-TEST-PDF-"+i);
					put("code","test.res."+i);
					put("mimeType","application/pdf");
					put("contentType","Resource");
					put("framework", "NCFCOPY");
				}});
			}});
		}};
		Map<String, String> header = new HashMap<String, String>() {{
			put("X-Channel-Id", CHANNEL_ID);
			put("Content-Type", DEFAULT_CONTENT_TYPE);
		}};
		Response resp = UnirestUtil.post(url, request, header);
		if ((null != resp && resp.getResponseCode() == ResponseCode.OK) && MapUtils.isNotEmpty(resp.getResult())) {
			contentId = (String) resp.getResult().get("identifier");
		} else {
			throw new ServerException(TaxonomyErrorCodes.SYSTEM_ERROR.name(), "Invalid Response received while creating content for : " + i);
		}
		return contentId;
	}

	private static void upload(String identifier, File file) throws Exception {
		/*if (null != file && !file.exists())
			System.out.println("ContentUtil :: upload :: File Path for " + identifier + "is : " + file.getAbsolutePath() + " | File Size : " + file.length());*/
		String url = KP_CS_BASE_URL + "/content/v3/upload/" + identifier;
		Map<String, String> header = new HashMap<String, String>() {{
			put("X-Channel-Id", CHANNEL_ID);
		}};
		Response resp = UnirestUtil.post(url, "file", file, header);
		if ((null != resp && resp.getResponseCode() == ResponseCode.OK) && MapUtils.isNotEmpty(resp.getResult())) {
			String artifactUrl = (String) resp.getResult().get(AutoCreatorParams.artifactUrl.name());
		} else {
			throw new ServerException(TaxonomyErrorCodes.SYSTEM_ERROR.name(), "Invalid Response received while uploading : " + identifier);
		}
	}

	private static Boolean publish(String identifier) throws Exception {
		String url = KP_LEARNING_BASE_URL + "/content/v3/publish/" + identifier;
		Map<String, Object> request = new HashMap<String, Object>() {{
			put("request", new HashMap<String, Object>() {{
				put("content", new HashMap<String, Object>() {{
					put(AutoCreatorParams.lastPublishedBy.name(), "EkStep");
				}});
			}});
		}};
		Map<String, String> header = new HashMap<String, String>() {{
			put("X-Channel-Id", CHANNEL_ID);
			put("Content-Type", DEFAULT_CONTENT_TYPE);
		}};
		Response resp = UnirestUtil.post(url, request, header);
		if ((null != resp && resp.getResponseCode() == ResponseCode.OK) && MapUtils.isNotEmpty(resp.getResult())) {
			String publishStatus = (String) resp.getResult().get("publishStatus");
			if (StringUtils.isNotBlank(publishStatus)) {
				//LOGGER.info("ContentUtil :: publish :: Content sent for publish successfully for : " + identifier);
				return true;
			}
			else
				throw new ServerException(TaxonomyErrorCodes.SYSTEM_ERROR.name(), "Content Publish Call Failed For : " + identifier);
		} else {
			//LOGGER.info("ContentUtil :: publish :: Invalid Response received while publishing content for : " + identifier + " | Response Code : " + resp.getResponseCode().toString() + " | Result : " + resp.getResult() + " | Error Message : " + resp.getParams().getErrmsg());
			throw new ServerException(TaxonomyErrorCodes.SYSTEM_ERROR.name(), "Invalid Response received while publishing content for : " + identifier);
		}

	}

	private static String getBasePath(String objectId) {
		return StringUtils.isNotBlank(objectId) ? "/tmp" + File.separator + objectId + "_temp_" + System.currentTimeMillis(): "";
	}

	private static String getFileNameFromURL(String fileUrl) {
		String fileName = FilenameUtils.getBaseName(fileUrl) + "_" + System.currentTimeMillis();
		if (!FilenameUtils.getExtension(fileUrl).isEmpty())
			fileName += "." + FilenameUtils.getExtension(fileUrl);
		return fileName;
	}

	private static File getFile(String identifier, String fileUrl) {
		try {
			String fileName = getBasePath(identifier) + File.separator + getFileNameFromURL(fileUrl);
			File file = new File(fileName);
			FileUtils.copyURLToFile(new URL(fileUrl), file);
			return file;
		} catch (IOException e) {
			//LOGGER.info("Invalid fileUrl received for : " + identifier + " | fileUrl : " + fileUrl + "Exception is : " + e.getMessage());
			throw new ServerException(TaxonomyErrorCodes.ERR_INVALID_UPLOAD_FILE_URL.name(), "Invalid fileUrl received for : " + identifier + " | fileUrl : " + fileUrl);
		}
	}
}
