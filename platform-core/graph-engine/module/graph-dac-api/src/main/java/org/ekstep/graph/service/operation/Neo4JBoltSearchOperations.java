package org.ekstep.graph.service.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.common.dto.Property;
import org.ekstep.common.dto.Request;
import org.ekstep.common.exception.ResourceNotFoundException;
import org.ekstep.common.logger.LoggerEnum;
import org.ekstep.common.logger.PlatformLogger;
import org.ekstep.graph.cache.mgr.impl.NodeCacheManager;
import org.ekstep.graph.dac.enums.GraphDACParams;
import org.ekstep.graph.dac.model.Graph;
import org.ekstep.graph.dac.model.Node;
import org.ekstep.graph.dac.model.Relation;
import org.ekstep.graph.dac.model.SearchCriteria;
import org.ekstep.graph.dac.model.SubGraph;
import org.ekstep.graph.dac.model.Traverser;
import org.ekstep.graph.service.INeo4JBoltSearchOperations;
import org.ekstep.graph.service.common.CypherQueryConfigurationConstants;
import org.ekstep.graph.service.common.DACErrorCodeConstants;
import org.ekstep.graph.service.common.DACErrorMessageConstants;
import org.ekstep.graph.service.common.GraphOperation;
import org.ekstep.graph.service.common.Neo4JOperation;
import org.ekstep.graph.service.util.DriverUtil;
import org.ekstep.graph.service.util.QueryUtil;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.graphdb.Direction;

public class Neo4JBoltSearchOperations implements INeo4JBoltSearchOperations {

	/**
	 * Gets the node by id.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param getTags
	 *            the get tags
	 * @param request
	 *            the request
	 * @return the node by id
	 */
	public Node getNodeById(String graphId, Long nodeId, Boolean getTags, Request request) {
		PlatformLogger.log("Graph Id: "+ graphId);
		PlatformLogger.log("Node Id: " + nodeId);
		PlatformLogger.log("Get Tags ? " + getTags);

		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Node By Id' Operation Failed.]");

		if (nodeId == 0)
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_NODE_ID + " | ['Get Node By Id' Operation Failed.]");

		Node node = new Node();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.nodeId.name(), nodeId);
			parameterMap.put(GraphDACParams.getTags.name(), getTags);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.GET_NODE_BY_ID, parameterMap));
			if (null == result || !result.hasNext())
				throw new ResourceNotFoundException(DACErrorCodeConstants.NOT_FOUND.name(),
						DACErrorMessageConstants.NODE_NOT_FOUND + " | [Invalid Node Id.]");

			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> nodeMap = new HashMap<Long, Object>();
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			for (Record record : result.list()) {
				PlatformLogger.log("'Get Node By Id' Operation Finished.", record);
				if (null != record)
					getRecordValues(record, nodeMap, relationMap, startNodeMap, endNodeMap);
			}
			PlatformLogger.log("Node Map: ", nodeMap);
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!nodeMap.isEmpty()) {
				for (Entry<Long, Object> entry : nodeMap.entrySet())
					node = new Node(graphId, (org.neo4j.driver.v1.types.Node) entry.getValue(), relationMap,
							startNodeMap, endNodeMap);
			}
		}
		PlatformLogger.log("Returning Node By Id: ", node);
		return node;
	}

	/**
	 * Gets the node by unique id.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param getTags
	 *            the get tags
	 * @param request
	 *            the request
	 * @return the node by unique id
	 */
	public Node getNodeByUniqueId(String graphId, String nodeId, Boolean getTags, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Node Id: ", nodeId);
		PlatformLogger.log("Get Tags ? ", getTags);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Node By Unique Id' Operation Failed.]");

		if (StringUtils.isBlank(nodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_IDENTIFIER + " | ['Get Node By Unique Id' Operation Failed.]");

		
		Node node = (Node) NodeCacheManager.getDataNode(graphId, nodeId);
		if (null != node) {
			PlatformLogger.log("Fetched node from in-memory cache: "+node.getIdentifier(), null, LoggerEnum.INFO.name());
			return node;
		} else {
			Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
			PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
			try (Session session = driver.session()) {
				PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

				PlatformLogger.log("Populating Parameter Map.");
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put(GraphDACParams.graphId.name(), graphId);
				parameterMap.put(GraphDACParams.nodeId.name(), nodeId);
				parameterMap.put(GraphDACParams.getTags.name(), getTags);
				parameterMap.put(GraphDACParams.request.name(), request);

				StatementResult result = session
						.run(QueryUtil.getQuery(Neo4JOperation.GET_NODE_BY_UNIQUE_ID, parameterMap));
				if (null == result || !result.hasNext())
					throw new ResourceNotFoundException(DACErrorCodeConstants.NOT_FOUND.name(),
							DACErrorMessageConstants.NODE_NOT_FOUND + " | [Invalid Node Id.]: "+ nodeId);

				PlatformLogger.log("Initializing the Result Maps.");
				Map<Long, Object> nodeMap = new HashMap<Long, Object>();
				Map<Long, Object> relationMap = new HashMap<Long, Object>();
				Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
				Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Node By Unique Id' Operation Finished.", record);
					if (null != record)
						getRecordValues(record, nodeMap, relationMap, startNodeMap, endNodeMap);
				}
				PlatformLogger.log("Node Map: ", nodeMap);
				PlatformLogger.log("Relation Map: ", relationMap);
				PlatformLogger.log("Start Node Map: ", startNodeMap);
				PlatformLogger.log("End Node Map: ", endNodeMap);
				PlatformLogger.log("Initializing Node.");
				if (!nodeMap.isEmpty()) {
					for (Entry<Long, Object> entry : nodeMap.entrySet())
						node = new Node(graphId, (org.neo4j.driver.v1.types.Node) entry.getValue(), relationMap,
								startNodeMap, endNodeMap);
				}
				if (StringUtils.equalsIgnoreCase("Concept", node.getObjectType())) {
					PlatformLogger.log("Saving concept to in-memory cache: "+node.getIdentifier(), null, LoggerEnum.INFO.name());
					NodeCacheManager.saveDataNode(graphId, node.getIdentifier(), node);
				}
			}
			return node;
		}
	}

	/**
	 * Gets the nodes by property.
	 *
	 * @param graphId
	 *            the graph id
	 * @param property
	 *            the property
	 * @param getTags
	 *            the get tags
	 * @param request
	 *            the request
	 * @return the nodes by property
	 */
	public List<Node> getNodesByProperty(String graphId, Property property, Boolean getTags, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Property: ", property);
		PlatformLogger.log("Get Tags ? ", getTags);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Nodes By Property' Operation Failed.]");

		if (null == property)
			throw new ClientException(DACErrorCodeConstants.INVALID_PROPERTY.name(),
					DACErrorMessageConstants.INVALID_PROPERTY + " | ['Get Nodes By Property' Operation Failed.]");

		List<Node> nodes = new ArrayList<Node>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.property.name(), property);
			parameterMap.put(GraphDACParams.getTags.name(), getTags);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session
					.run(QueryUtil.getQuery(Neo4JOperation.GET_NODES_BY_PROPERTY, parameterMap));
			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> nodeMap = new HashMap<Long, Object>();
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Nodes By Property Id' Operation Finished.", record);
					if (null != record)
						getRecordValues(record, nodeMap, relationMap, startNodeMap, endNodeMap);
				}
			}
			PlatformLogger.log("Node Map: ", nodeMap);
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!nodeMap.isEmpty()) {
				for (Entry<Long, Object> entry : nodeMap.entrySet())
					nodes.add(new Node(graphId, (org.neo4j.driver.v1.types.Node) entry.getValue(), relationMap,
							startNodeMap, endNodeMap));
			}
		}
		PlatformLogger.log("Returning Node By Property: ", nodes);
		return nodes;
	}

	/**
	 * Gets the node by unique ids.
	 *
	 * @param graphId
	 *            the graph id
	 * @param searchCriteria
	 *            the search criteria
	 * @param request
	 *            the request
	 * @return the node by unique ids
	 */
	public List<Node> getNodeByUniqueIds(String graphId, SearchCriteria searchCriteria, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Search Criteria: ", searchCriteria);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID
							+ " | ['Get Nodes By Search Criteria' Operation Failed.]");

		if (null == searchCriteria)
			throw new ClientException(DACErrorCodeConstants.INVALID_CRITERIA.name(),
					DACErrorMessageConstants.INVALID_SEARCH_CRITERIA
							+ " | ['Get Nodes By Search Criteria' Operation Failed.]");

		List<Node> nodes = new ArrayList<Node>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.searchCriteria.name(), searchCriteria);
			parameterMap.put(GraphDACParams.request.name(), request);

			String query = QueryUtil.getQuery(Neo4JOperation.GET_NODES_BY_SEARCH_CRITERIA, parameterMap);
			Map<String, Object> params = searchCriteria.getParams();
			StatementResult result = session.run(query, params);
			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> nodeMap = new HashMap<Long, Object>();
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Nodes By Search Criteria' Operation Finished.", record);
					if (null != record)
						getRecordValues(record, nodeMap, relationMap, startNodeMap, endNodeMap);
				}
			}
			PlatformLogger.log("Node Map: ", nodeMap);
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!nodeMap.isEmpty()) {
				for (Entry<Long, Object> entry : nodeMap.entrySet())
					nodes.add(new Node(graphId, (org.neo4j.driver.v1.types.Node) entry.getValue(), relationMap,
							startNodeMap, endNodeMap));
			}
		}
		PlatformLogger.log("Returning Node By Search Criteria: ", nodes);
		return nodes;
	}

	public List<Map<String, Object>> executeQueryForProps(String graphId, String query, List<String> propKeys) {
		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(), DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Execute Query For Nodes' Operation Failed.]");
		List<Map<String, Object>> propsList = new ArrayList<Map<String, Object>>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");

			StatementResult result = session.run(query);
			PlatformLogger.log("Initializing the Result Maps.");
			if (null != result) {
				for (Record record : result.list()) {
					if (null != record) {
						Map<String, Object> row = new HashMap<String, Object>();
						for (int i = 0; i < propKeys.size(); i++) {
							String key = propKeys.get(i);
							Value value = record.get(key);
							if (null != value) 
								row.put(key, value.asObject());
						}
						if (!row.isEmpty())
							propsList.add(row);
					}
				}
			}
		}
		return propsList;
	}
	
	/**
	 * Gets the node property.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param key
	 *            the key
	 * @param request
	 *            the request
	 * @return the node property
	 */
	public Property getNodeProperty(String graphId, String nodeId, String key, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Node Id: ", nodeId);
		PlatformLogger.log("Property (Key): ", key);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Node Property' Operation Failed.]");

		if (StringUtils.isBlank(nodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_IDENTIFIER + " | ['Get Node Property' Operation Failed.]");

		if (StringUtils.isBlank(key))
			throw new ClientException(DACErrorCodeConstants.INVALID_PROPERTY.name(),
					DACErrorMessageConstants.INVALID_PROPERTY_KEY + " | ['Get Node Property' Operation Failed.]");

		Property property = new Property();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.nodeId.name(), nodeId);
			parameterMap.put(GraphDACParams.key.name(), key);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.GET_NODE_PROPERTY, parameterMap));
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Node Property' Operation Finished.", record);
					if (null != record && null != record.get(key)) {
						property.setPropertyName(key);
						property.setPropertyValue(record.get(key));
					}
				}
			}
		}
		PlatformLogger.log("Returning Node Property: ", property);
		return property;
	}

	/**
	 * Gets the all nodes.
	 *
	 * @param graphId
	 *            the graph id
	 * @param request
	 *            the request
	 * @return the all nodes
	 */
	public List<Node> getAllNodes(String graphId, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get All Nodes' Operation Failed.]");

		List<Node> nodes = new ArrayList<Node>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.GET_ALL_NODES, parameterMap));
			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> nodeMap = new HashMap<Long, Object>();
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get All Nodes' Operation Finished.", record);
					if (null != record)
						getRecordValues(record, nodeMap, relationMap, startNodeMap, endNodeMap);
				}
			}
			PlatformLogger.log("Node Map: ", nodeMap);
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!nodeMap.isEmpty()) {
				for (Entry<Long, Object> entry : nodeMap.entrySet())
					nodes.add(new Node(graphId, (org.neo4j.driver.v1.types.Node) entry.getValue(), relationMap,
							startNodeMap, endNodeMap));
			}
		}
		PlatformLogger.log("Returning All Nodes: ", nodes);
		return nodes;
	}

	/**
	 * Gets the all relations.
	 *
	 * @param graphId
	 *            the graph id
	 * @param request
	 *            the request
	 * @return the all relations
	 */
	public List<Relation> getAllRelations(String graphId, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get All Relations' Operation Failed.]");

		List<Relation> relations = new ArrayList<Relation>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.GET_ALL_RELATIONS, parameterMap));
			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get All Relations' Operation Finished.", record);
					if (null != record)
						getRecordValues(record, null, relationMap, startNodeMap, endNodeMap);
				}
			}
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!relationMap.isEmpty()) {
				for (Entry<Long, Object> entry : relationMap.entrySet())
					relations.add(new Relation(graphId, (org.neo4j.driver.v1.types.Relationship) entry.getValue(),
							startNodeMap, endNodeMap));
			}
		}
		PlatformLogger.log("Returning All Relations: ", relations);
		return relations;
	}

	/**
	 * Gets the relation property.
	 *
	 * @param graphId
	 *            the graph id
	 * @param startNodeId
	 *            the start node id
	 * @param relationType
	 *            the relation type
	 * @param endNodeId
	 *            the end node id
	 * @param key
	 *            the key
	 * @param request
	 *            the request
	 * @return the relation property
	 */
	public Property getRelationProperty(String graphId, String startNodeId, String relationType, String endNodeId,
			String key, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Start Node Id: ", startNodeId);
		PlatformLogger.log("Relation Type: ", relationType);
		PlatformLogger.log("End Node Id: ", endNodeId);
		PlatformLogger.log("Property (Key): ", key);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Relation Property' Operation Failed.]");

		if (StringUtils.isBlank(startNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_START_NODE_ID + " | ['Get Relation Property' Operation Failed.]");

		if (StringUtils.isBlank(relationType))
			throw new ClientException(DACErrorCodeConstants.INVALID_RELATION.name(),
					DACErrorMessageConstants.INVALID_RELATION_TYPE + " | ['Get Relation Property' Operation Failed.]");

		if (StringUtils.isBlank(endNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_END_NODE_ID + " | ['Get Relation Property' Operation Failed.]");

		if (StringUtils.isBlank(key))
			throw new ClientException(DACErrorCodeConstants.INVALID_PROPERTY.name(),
					DACErrorMessageConstants.INVALID_PROPERTY_KEY + " | ['Get Relation Property' Operation Failed.]");

		Property property = new Property();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.startNodeId.name(), startNodeId);
			parameterMap.put(GraphDACParams.relationType.name(), relationType);
			parameterMap.put(GraphDACParams.endNodeId.name(), endNodeId);
			parameterMap.put(GraphDACParams.key.name(), key);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session
					.run(QueryUtil.getQuery(Neo4JOperation.GET_RELATION_PROPERTY, parameterMap));
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Relation Property' Operation Finished.", record);
					if (null != record && null != record.get(key)) {
						property.setPropertyName(key);
						property.setPropertyValue(record.get(key));
					}
				}
			}
		}
		PlatformLogger.log("Returning Relation Property: ", property);
		return property;
	}
	
	public Relation getRelationById(String graphId, Long relationId, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Relation Id: ", relationId);

		
		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Relation By Id' Operation Failed.]");

		if (null == relationId || relationId < 0)
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_IDENTIFIER + " | ['Get Relation' Operation Failed.]");
		
		Relation relation = new Relation();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");
			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.identifier.name(), relationId);
			parameterMap.put(GraphDACParams.request.name(), request);
			
			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.GET_RELATION_BY_ID, parameterMap));
			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Relation' Operation Finished.", record);
					if (null != record)
						getRecordValues(record, null, relationMap, startNodeMap, endNodeMap);
				}
			}
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!relationMap.isEmpty()) {
				for (Entry<Long, Object> entry : relationMap.entrySet())
					relation = new Relation(graphId, (org.neo4j.driver.v1.types.Relationship) entry.getValue(),
							startNodeMap, endNodeMap);
			}
		}
		return relation;
	}

	/**
	 * Gets the relation.
	 *
	 * @param graphId
	 *            the graph id
	 * @param startNodeId
	 *            the start node id
	 * @param relationType
	 *            the relation type
	 * @param endNodeId
	 *            the end node id
	 * @param request
	 *            the request
	 * @return the relation
	 */
	public Relation getRelation(String graphId, String startNodeId, String relationType, String endNodeId,
			Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Start Node Id: ", startNodeId);
		PlatformLogger.log("Relation Type: ", relationType);
		PlatformLogger.log("End Node Id: ", endNodeId);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Relation' Operation Failed.]");

		if (StringUtils.isBlank(startNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_START_NODE_ID + " | ['Get Relation' Operation Failed.]");

		if (StringUtils.isBlank(relationType))
			throw new ClientException(DACErrorCodeConstants.INVALID_RELATION.name(),
					DACErrorMessageConstants.INVALID_RELATION_TYPE + " | ['Get Relation' Operation Failed.]");

		if (StringUtils.isBlank(endNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_END_NODE_ID + " | ['Get Relation' Operation Failed.]");

		Relation relation = new Relation();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.startNodeId.name(), startNodeId);
			parameterMap.put(GraphDACParams.relationType.name(), relationType);
			parameterMap.put(GraphDACParams.endNodeId.name(), endNodeId);
			parameterMap.put(GraphDACParams.request.name(), request);
			
			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.GET_RELATION, parameterMap));
			if (null == result || !result.hasNext())
				throw new ResourceNotFoundException(DACErrorCodeConstants.NOT_FOUND.name(),
						DACErrorMessageConstants.NODE_NOT_FOUND + " | [No Relation found.]");

			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			for (Record record : result.list()) {
				PlatformLogger.log("'Get Relation' Operation Finished.", record);
				if (null != record)
					getRecordValues(record, null, relationMap, startNodeMap, endNodeMap);
			}
			PlatformLogger.log("Relation Map: ", relationMap);
			PlatformLogger.log("Start Node Map: ", startNodeMap);
			PlatformLogger.log("End Node Map: ", endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!relationMap.isEmpty()) {
				for (Entry<Long, Object> entry : relationMap.entrySet())
					relation = new Relation(graphId, (org.neo4j.driver.v1.types.Relationship) entry.getValue(),
							startNodeMap, endNodeMap);
			}
		}
		PlatformLogger.log("Returning Relation: ", relation);
		return relation;
	}

	/**
	 * Check cyclic loop.
	 *
	 * @param graphId
	 *            the graph id
	 * @param startNodeId
	 *            the start node id
	 * @param relationType
	 *            the relation type
	 * @param endNodeId
	 *            the end node id
	 * @param request
	 *            the request
	 * @return the map
	 */
	public Map<String, Object> checkCyclicLoop(String graphId, String startNodeId, String relationType,
			String endNodeId, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Start Node Id: ", startNodeId);
		PlatformLogger.log("Relation Type: ", relationType);
		PlatformLogger.log("End Node Id: ", endNodeId);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Check Cyclic Loop' Operation Failed.]");

		if (StringUtils.isBlank(startNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_START_NODE_ID + " | ['Check Cyclic Loop' Operation Failed.]");

		if (StringUtils.isBlank(relationType))
			throw new ClientException(DACErrorCodeConstants.INVALID_RELATION.name(),
					DACErrorMessageConstants.INVALID_RELATION_TYPE + " | ['Check Cyclic Loop' Operation Failed.]");

		if (StringUtils.isBlank(endNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_END_NODE_ID + " | ['Check Cyclic Loop' Operation Failed.]");

		Map<String, Object> cyclicLoopMap = new HashMap<String, Object>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.WRITE);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.startNodeId.name(), startNodeId);
			parameterMap.put(GraphDACParams.relationType.name(), relationType);
			parameterMap.put(GraphDACParams.endNodeId.name(), endNodeId);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.CHECK_CYCLIC_LOOP, parameterMap));
			if (null != result && result.hasNext()) {
				cyclicLoopMap.put(GraphDACParams.loop.name(), new Boolean(true));
				cyclicLoopMap.put(GraphDACParams.message.name(),
						startNodeId + " and " + endNodeId + " are connected by relation: " + relationType);
			} else {
				cyclicLoopMap.put(GraphDACParams.loop.name(), new Boolean(false));
			}
		}

		PlatformLogger.log("Returning Cyclic Loop Map: ", cyclicLoopMap);
		return cyclicLoopMap;
	}

	/**
	 * Execute query.
	 *
	 * @param graphId
	 *            the graph id
	 * @param query
	 *            the query
	 * @param paramMap
	 *            the param map
	 * @param request
	 *            the request
	 * @return the list
	 */
	public List<Map<String, Object>> executeQuery(String graphId, String query, Map<String, Object> paramMap,
			Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Query: ", query);
		PlatformLogger.log("Param Map: ", paramMap);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Execute Query' Operation Failed.]");

		if (StringUtils.isBlank(query))
			throw new ClientException(DACErrorCodeConstants.INVALID_QUERY.name(),
					DACErrorMessageConstants.INVALID_QUERY + " | ['Execute Query' Operation Failed.]");

		if (null == paramMap || paramMap.isEmpty())
			throw new ClientException(DACErrorCodeConstants.INVALID_PARAMETER.name(),
					DACErrorMessageConstants.INVALID_PARAM_MAP + " | ['Execute Query' Operation Failed.]");

		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.cypherQuery.name(), query);
			parameterMap.put(GraphDACParams.paramMap.name(), paramMap);
			parameterMap.put(GraphDACParams.request.name(), request);

			StatementResult result = session.run(QueryUtil.getQuery(Neo4JOperation.EXECUTE_QUERY, parameterMap),
					paramMap);
			for (Record record : result.list()) {
				PlatformLogger.log("'Execute Query' Operation Finished.", record);
				Map<String, Object> recordMap = record.asMap();
				Map<String, Object> map = new HashMap<String, Object>();
				if (null != recordMap && !recordMap.isEmpty()) {
					for (Entry<String, Object> entry : recordMap.entrySet()) {
						map.put(entry.getKey(), entry.getValue());
					}
					resultList.add(map);
				}
			}
		}
		PlatformLogger.log("Returning Execute Query Result: ", resultList);
		return resultList;
	}

	/**
	 * Search nodes.
	 *
	 * @param graphId
	 *            the graph id
	 * @param searchCriteria
	 *            the search criteria
	 * @param getTags
	 *            the get tags
	 * @param request
	 *            the request
	 * @return the list
	 */
	public List<Node> searchNodes(String graphId, SearchCriteria searchCriteria, Boolean getTags, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Search Criteria: ", searchCriteria);
		PlatformLogger.log("Get Tags ? ", getTags);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Search Nodes' Operation Failed.]");

		if (null == searchCriteria)
			throw new ClientException(DACErrorCodeConstants.INVALID_CRITERIA.name(),
					DACErrorMessageConstants.INVALID_SEARCH_CRITERIA + " | ['Search Nodes' Operation Failed.]");

		List<Node> nodes = new ArrayList<Node>();
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");
			List<String> fields = searchCriteria.getFields();
			boolean returnNode = true;
			if (null != fields && !fields.isEmpty())
				returnNode = false;
			
			PlatformLogger.log("Populating Parameter Map.");
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.searchCriteria.name(), searchCriteria);
			parameterMap.put(GraphDACParams.getTags.name(), getTags);
			parameterMap.put(GraphDACParams.request.name(), request);

			String query = QueryUtil.getQuery(Neo4JOperation.SEARCH_NODES, parameterMap);
			PlatformLogger.log("Search Query: " + query);
			Map<String, Object> params = searchCriteria.getParams();
			PlatformLogger.log("Search Params: " + params);
			StatementResult result = session.run(query, params);
			PlatformLogger.log("Initializing the Result Maps.");
			Map<Long, Object> nodeMap = new LinkedHashMap<Long, Object>();
			Map<Long, Object> relationMap = new HashMap<Long, Object>();
			Map<Long, Object> startNodeMap = new HashMap<Long, Object>();
			Map<Long, Object> endNodeMap = new HashMap<Long, Object>();
			if (null != result) {
				PlatformLogger.log("'Search Nodes' result: " + result);
				for (Record record : result.list()) {
					PlatformLogger.log("'Search Nodes' Operation Finished.", record);
					if (null != record) {
						if (returnNode)
							getRecordValues(record, nodeMap, relationMap, startNodeMap, endNodeMap);
						else {
							Node node = new Node(graphId, record.asMap());
							nodes.add(node);
						}
					}
				}
			}
			PlatformLogger.log("Node Map: " + nodeMap);
			PlatformLogger.log("Relation Map: " + relationMap);
			PlatformLogger.log("Start Node Map: " + startNodeMap);
			PlatformLogger.log("End Node Map: " + endNodeMap);

			PlatformLogger.log("Initializing Node.");
			if (!nodeMap.isEmpty()) {
				for (Entry<Long, Object> entry : nodeMap.entrySet())
					nodes.add(new Node(graphId, (org.neo4j.driver.v1.types.Node) entry.getValue(), relationMap,
							startNodeMap, endNodeMap));
			}
		}
		PlatformLogger.log("Returning Search Nodes: " + nodes);
		return nodes;
	}

	/**
	 * Gets the nodes count.
	 *
	 * @param graphId
	 *            the graph id
	 * @param searchCriteria
	 *            the search criteria
	 * @param request
	 *            the request
	 * @return the nodes count
	 */
	public Long getNodesCount(String graphId, SearchCriteria searchCriteria, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Search Criteria: ", searchCriteria);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Nodes Count' Operation Failed.]");

		if (null == searchCriteria)
			throw new ClientException(DACErrorCodeConstants.INVALID_CRITERIA.name(),
					DACErrorMessageConstants.INVALID_SEARCH_CRITERIA + " | ['Get Nodes Count' Operation Failed.]");

		Long count = (long) 0;
		Driver driver = DriverUtil.getDriver(graphId, GraphOperation.READ);
		PlatformLogger.log("Driver Initialised. | [Graph Id: " + graphId + "]");
		try (Session session = driver.session()) {
			PlatformLogger.log("Session Initialised. | [Graph Id: " + graphId + "]");

			PlatformLogger.log("Populating Parameter Map.");
			searchCriteria.setCountQuery(true);
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			parameterMap.put(GraphDACParams.graphId.name(), graphId);
			parameterMap.put(GraphDACParams.searchCriteria.name(), searchCriteria);
			parameterMap.put(GraphDACParams.request.name(), request);

			String query = QueryUtil.getQuery(Neo4JOperation.GET_NODES_COUNT, parameterMap);
			Map<String, Object> params = searchCriteria.getParams();
			StatementResult result = session.run(query, params);
			if (null != result) {
				for (Record record : result.list()) {
					PlatformLogger.log("'Get Nodes Count' Operation Finished.", record);
					if (null != record && null != record.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_COUNT_OBJECT))
						count = record.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_COUNT_OBJECT).asLong();
				}
			}
		}
		PlatformLogger.log("Returning Nodes Count: ", count);
		return count;
	}

	/**
	 * Traverse.
	 *
	 * @param graphId
	 *            the graph id
	 * @param traverser
	 *            the traverser
	 * @param request
	 *            the request
	 * @return the sub graph
	 */
	public SubGraph traverse(String graphId, Traverser traverser, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Traverser: ", traverser);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Traverse' Operation Failed.]");

		if (null == traverser)
			throw new ClientException(DACErrorCodeConstants.INVALID_TRAVERSER.name(),
					DACErrorMessageConstants.INVALID_TRAVERSER + " | ['Traverse' Operation Failed.]");

		SubGraph subGraph = traverser.traverse();
		PlatformLogger.log("Returning Sub Graph: ", subGraph);
		return subGraph;
	}

	/**
	 * Traverse sub graph.
	 *
	 * @param graphId
	 *            the graph id
	 * @param traverser
	 *            the traverser
	 * @param request
	 *            the request
	 * @return the graph
	 */
	public Graph traverseSubGraph(String graphId, Traverser traverser, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Traverser: ", traverser);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Traverse Sub Graph' Operation Failed.]");

		if (null == traverser)
			throw new ClientException(DACErrorCodeConstants.INVALID_TRAVERSER.name(),
					DACErrorMessageConstants.INVALID_TRAVERSER + " | ['Traverse Sub Graph' Operation Failed.]");

		Graph subGraph = traverser.getSubGraph();
		PlatformLogger.log("Returning Graph : ", subGraph);
		return subGraph;
	}

	/**
	 * Gets the sub graph.
	 *
	 * @param graphId
	 *            the graph id
	 * @param startNodeId
	 *            the start node id
	 * @param relationType
	 *            the relation type
	 * @param depth
	 *            the depth
	 * @param request
	 *            the request
	 * @return the sub graph
	 */
	public Graph getSubGraph(String graphId, String startNodeId, String relationType, Integer depth, Request request) {
		PlatformLogger.log("Graph Id: ", graphId);
		PlatformLogger.log("Start Node Id: ", startNodeId);
		PlatformLogger.log("Relation Type: ", relationType);
		PlatformLogger.log("Depth: ", depth);


		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | ['Get Sub Graph' Operation Failed.]");

		if (StringUtils.isBlank(startNodeId))
			throw new ClientException(DACErrorCodeConstants.INVALID_IDENTIFIER.name(),
					DACErrorMessageConstants.INVALID_START_NODE_ID + " | ['Get Sub Graph' Operation Failed.]");

		Traverser traverser = new Traverser(graphId, startNodeId);
		traverser = traverser.addRelationMap(relationType, Direction.OUTGOING.name());
		if (null != depth && depth.intValue() > 0) {
			traverser.toDepth(depth);
		}
		Graph subGraph = traverser.getSubGraph();
		PlatformLogger.log("Returning Graph : ", subGraph);
		return subGraph;
	}

	private void getRecordValues(Record record, Map<Long, Object> nodeMap, Map<Long, Object> relationMap,
			Map<Long, Object> startNodeMap, Map<Long, Object> endNodeMap) {
		if (null != nodeMap) {
			Value nodeValue = record.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_NODE_OBJECT);
			if (null != nodeValue && StringUtils.equalsIgnoreCase("NODE", nodeValue.type().name())) {
				org.neo4j.driver.v1.types.Node neo4jBoltNode = record
						.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_NODE_OBJECT).asNode();
				nodeMap.put(neo4jBoltNode.id(), neo4jBoltNode);
			}
		}
		if (null != relationMap) {
			Value relValue = record.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_RELATION_OBJECT);
			if (null != relValue && StringUtils.equalsIgnoreCase("RELATIONSHIP", relValue.type().name())) {
				org.neo4j.driver.v1.types.Relationship relationship = record
						.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_RELATION_OBJECT).asRelationship();
				relationMap.put(relationship.id(), relationship);
			}
		}
		if (null != startNodeMap) {
			Value startNodeValue = record.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_START_NODE_OBJECT);
			if (null != startNodeValue && StringUtils.equalsIgnoreCase("NODE", startNodeValue.type().name())) {
				org.neo4j.driver.v1.types.Node startNode = record
						.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_START_NODE_OBJECT).asNode();
				startNodeMap.put(startNode.id(), startNode);
			}
		}
		if (null != endNodeMap) {
			Value endNodeValue = record.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_END_NODE_OBJECT);
			if (null != endNodeValue && StringUtils.equalsIgnoreCase("NODE", endNodeValue.type().name())) {
				org.neo4j.driver.v1.types.Node endNode = record
						.get(CypherQueryConfigurationConstants.DEFAULT_CYPHER_END_NODE_OBJECT).asNode();
				endNodeMap.put(endNode.id(), endNode);
			}
		}
	}

}
