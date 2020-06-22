package org.ekstep.jobs.samza.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CreateHierarchyRequest {

	private static ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) throws Exception {
		Map<String, Object> hierarchy = getHierarchyRequest("do_11304859797605580811789", "TB-1000-NODES_1",1000);
		System.out.println("hierarchy: "+mapper.writeValueAsString(hierarchy));
	}

	public static Map<String, Object> getHierarchyRequest(String tbId, String tbName, int count) {
		Map<String, Object> request = new HashMap<String, Object>();
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, Object> nodesModified = new HashMap<String, Object>();
		Map<String, Object> hierarchy = new HashMap<String, Object>();
		for (int i = 1; i <= count; i++) {
			String resourceId = "do_test_content_"+i;
			String unitId = UUID.randomUUID().toString();
			String name = "U-"+i;
			nodesModified.put(unitId, new HashMap<String, Object>(){{
				put("isNew", true);
				put("root", false);
				put("metadata", new HashMap<String, Object>(){{
					put("mimeType", "application/vnd.ekstep.content-collection");
					put("contentType", "TextBookUnit");
					put("code", unitId);
					put("name", name);
					put("description", name);
					put("framework", "NCFCOPY");
				}});
			}});
			if(i==1){
				hierarchy.put(tbId, new HashMap<String, Object>(){{
					put("name", tbName);
					put("contentType", "TextBook");
					put("children", new ArrayList<String>(){{
						add(unitId);
					}});
					put("root", true);
				}});
			}else {
				List<String> children = (List<String>)((Map<String, Object>)hierarchy.get(tbId)).get("children");
				children.add(unitId);
			}
			hierarchy.put(unitId, new HashMap<String, Object>(){{
				put("name", name);
				put("contentType","TextBookUnit");
				put("children", Arrays.asList(resourceId));
				put("root", false);
			}});
		}
		data.put("nodesModified", nodesModified);
		data.put("hierarchy", hierarchy);
		request.put("data", data);
		return new HashMap<String, Object>(){{
			put("request", request);
		}};
	}
}
