package org.ekstep.jobs.samza.util;

import org.apache.commons.collections4.MapUtils;
import org.ekstep.common.dto.Response;
import org.ekstep.common.enums.TaxonomyErrorCodes;
import org.ekstep.common.exception.ResponseCode;
import org.ekstep.common.exception.ServerException;

import java.util.HashMap;
import java.util.Map;

public class UpdateResourceContent {

	private static final String KP_CS_BASE_URL = "http://11.2.6.6/content";
	private static final String KP_LEARNING_BASE_URL = "http://11.2.4.22:8080/learning-service";
	private static final String DEFAULT_CONTENT_TYPE = "application/json";
	private static final String CHANNEL_ID = "in.ekstep";

	public static void main(String[] args) throws Exception {
		for (int i = 998; i <= 1000; i++) {
			String id = "do_test_content_" + i;
			String versionKey = getVersionKey(id);
			updateContent(id, versionKey);
			System.out.println(id);
		}

	}

	public static String getVersionKey(String id) throws Exception {
		String versionKey = "";
		String url = KP_CS_BASE_URL + "/content/v3/read/" + id;
		Map<String, String> header = new HashMap<String, String>() {{
			put("X-Channel-Id", CHANNEL_ID);
			put("Content-Type", DEFAULT_CONTENT_TYPE);
		}};
		Response resp = UnirestUtil.get(url, null, header);
		if ((null != resp && resp.getResponseCode() == ResponseCode.OK) && MapUtils.isNotEmpty(resp.getResult())) {
			versionKey = (String) ((Map<String, Object>) resp.getResult().get("content")).get("versionKey");
		} else {
			throw new ServerException(TaxonomyErrorCodes.SYSTEM_ERROR.name(), "Invalid Response received while fetching content for : " + id);
		}
		return versionKey;
	}

	public static void updateContent(String id, String versionKey) throws Exception {
		String contentId = "";
		String url = KP_CS_BASE_URL + "/content/v3/update/" + id;
		Map<String, Object> request = new HashMap<String, Object>() {{
			put("request", new HashMap<String, Object>() {{
				put("content", new HashMap<String, Object>() {{
					put("versionKey", versionKey);
					put("description", "updated for " + id);
				}});
			}});
		}};
		Map<String, String> header = new HashMap<String, String>() {{
			put("X-Channel-Id", CHANNEL_ID);
			put("Content-Type", DEFAULT_CONTENT_TYPE);
		}};
		Response resp = UnirestUtil.patch(url, request, header);
		if ((null != resp && resp.getResponseCode() == ResponseCode.OK) && MapUtils.isNotEmpty(resp.getResult())) {
			contentId = (String) resp.getResult().get("identifier");
		} else {
			throw new ServerException(TaxonomyErrorCodes.SYSTEM_ERROR.name(), "Invalid Response received while updating content for : " + id);
		}
	}
}
