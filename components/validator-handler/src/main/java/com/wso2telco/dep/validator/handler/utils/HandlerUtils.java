package com.wso2telco.dep.validator.handler.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Map;

public class HandlerUtils {

	private static final Log log = LogFactory.getLog(HandlerUtils.class);

	/**
	 *
	 * @param messageContext
	 * @return APIType
	 */
	public static APIType getAPIType(MessageContext messageContext) {
		APIType apiType = null;
		Object headers = ((Axis2MessageContext) messageContext).getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		Map headersMap = (Map) headers;
		String resource = (String)headersMap.get("RESOURCE");
		if (resource != null && resource.contains("/transactions/amount")) {
			apiType = APIType.PAYMENT;
		} else if (resource != null && resource.contains("/outbound/") && resource.contains("/requests")) {
			apiType = APIType.SMS;
		}
		return  apiType;
	}

	/**
	 * Get api-manager config URL
	 */
	public static String getUrlProperty(String propertyName) {
		String esbUrl = getApiConfiguration(propertyName);
		if(esbUrl != null && esbUrl.endsWith("/")) {
			esbUrl = esbUrl.substring(0, esbUrl.lastIndexOf("/"));
		}
		return esbUrl;
	}

	/**
	 * Get api-manager config UserAnonymization
	 */
	public static boolean isUserAnonymizationEnabled() {
		boolean isUserAnonymizationEnabled = false;
		if(getApiConfiguration("UserAnonymization") != null) {
			try {
				isUserAnonymizationEnabled = Boolean.valueOf(getApiConfiguration("UserAnonymization"));
			} catch (Exception e) {
				String errorMessage = "Invalid for configration in api-manager.xml :  userAnonymization";
				log.error(errorMessage, e);
			}
		}
		return isUserAnonymizationEnabled;
	}

	/**
	 * Get system configuration status for give config
	 * @return String
	 */
	public static String getApiConfiguration(String property) {
		String carbonHome = System.getProperty("carbon.home");
		String apiManagerFile = carbonHome + "/repository/conf/api-manager.xml";
		String config =  null;
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(new File(apiManagerFile));
			Element rootElement = document.getDocumentElement();
			NodeList nodeList = rootElement.getElementsByTagName(property);
			if(nodeList != null) {
				Node node = nodeList.item(0);
				config = ((Element)node).getTextContent();
			}
		} catch (Exception e) {
			String errorMessage = "Invalid for configration in api-manager.xml :  userAnonymization";
			log.error(errorMessage, e);
		}
		return config;
	}
}
