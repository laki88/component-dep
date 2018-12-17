package com.wso2telco.dep.validator.handler;

import com.wso2telco.dep.validator.handler.exceptions.ValidatorException;
import com.wso2telco.dep.validator.handler.utils.APIType;
import com.wso2telco.dep.validator.handler.utils.HandlerEncriptionUtils;
import com.wso2telco.dep.validator.handler.utils.HandlerUtils;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.api.APIManagementException;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class APIMaskHandler extends AbstractHandler {

	private static final Log log = LogFactory.getLog(APIMaskHandler.class);

	@Override
	public boolean handleRequest(MessageContext messageContext) {
		try {
		    // Valudate this requires masking
			if (HandlerUtils.isUserAnonymizationEnabled() && isMaskingAllowAPI(messageContext)) {
				maskRequestData(messageContext);
				setEndpointURL(messageContext);
			} else {
				messageContext.setProperty("CUSTOM_ENDPOINT", "false");
			}

		} catch (APIManagementException e) {
			log.error("Error while creating the APIIdentifier and retreiving api id from database", e);
		} catch (ValidatorException e) {
			log.error("Error while getting validator class", e);
		} catch (Exception e) {
			log.error("Error while getting validator class", e);
		}
		return true;
	}

	private void maskRequestData(MessageContext messageContext) throws Exception {

		// Getting the json payload to string
		String jsonString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) messageContext).getAxis2MessageContext());
		JSONObject jsonBody = new JSONObject(jsonString);
		
		Object headers = ((Axis2MessageContext) messageContext).getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		Map headersMap = (Map) headers;
		String resource = (String)headersMap.get("RESOURCE");
		APIType apiType = HandlerUtils.getAPIType(messageContext);
		String maskingSecretKey = HandlerUtils.getApiConfiguration("UserMaskingSecretKey");

		if (APIType.PAYMENT.equals(apiType)) {
			// Extract user ID with request
			JSONObject objAmountTransaction = (JSONObject) jsonBody.get("amountTransaction");
			String transactionOperationStatus = objAmountTransaction.get("transactionOperationStatus").toString();
			String originalMSISDN = (String) objAmountTransaction.get("endUserId");
			// User ID for request
			String userId = originalMSISDN;
			//Convert UserID to PCR if it is not in PCR format

			if (!HandlerEncriptionUtils.isMaskedUserId(userId)) {
				//userId = HandlerUtils.getPCR(userId, appConsureId, sector);
				userId = HandlerEncriptionUtils.maskUserId(userId, true, maskingSecretKey);
                String sss = HandlerEncriptionUtils.maskUserId(userId, false, maskingSecretKey);
			}
			// Replace resource property with updated user ID since resource path contains user ID
			if(!resource.contains(userId)) {
				String urlUserId = resource.substring(resource.indexOf("/") + 1, resource.lastIndexOf("/transactions/amount"));
				if (!HandlerEncriptionUtils.isMaskedUserId(urlUserId)) {
					String resourceUserId = HandlerEncriptionUtils.maskUserId(URLDecoder.decode(urlUserId, "UTF-8"), true, maskingSecretKey);
					resource = "/" + URLEncoder.encode(resourceUserId, "UTF-8") + "/transactions/amount";
				}
				headersMap.put("RESOURCE", resource);
			}
			// Updated payload with pcr user ID
			objAmountTransaction.put("endUserId", userId);
			jsonBody.put("amountTransaction", objAmountTransaction);
			updateJsonPayload(jsonBody.toString(), messageContext);

		} else if (APIType.SMS.equals(apiType)) {
			// Extract user IDs with request
			JSONObject outboundSMSMessageRequest = (JSONObject) jsonBody.get("outboundSMSMessageRequest");
			JSONArray addressArray = outboundSMSMessageRequest.getJSONArray("address");
			// New PCR  user ID array
			JSONArray newAddressArray = new JSONArray();
			for (int i = 0; i < addressArray.length(); i++) {
				//Convert UserID to PCR if it is not in PCR format
               if (!HandlerEncriptionUtils.isMaskedUserId(addressArray.getString(i))) {
                   String maskedAddress = HandlerEncriptionUtils.maskUserId(addressArray.getString(i), true, maskingSecretKey);
                   newAddressArray.put(maskedAddress);
               } else {
                   newAddressArray.put(addressArray.getString(i));
               }
			}
			// Set updated address list to request
			outboundSMSMessageRequest.put("address", newAddressArray);
			jsonBody.put("outboundSMSMessageRequest", outboundSMSMessageRequest);
			updateJsonPayload(jsonBody.toString(), messageContext);
		}

		// Set transport header indicating the request uses user anonymization
		headersMap.put("USER_ANONYMIZATION", "true");
	}

	@Override
	public boolean handleResponse(MessageContext messageContext) {
		return true;
	}


    /**
     *  This method is to decide whether the request need to mask user ID
     * @param messageContext
     * @return
     * @throws IOException
     * @throws XMLStreamException
     */
	private boolean isMaskingAllowAPI(MessageContext messageContext) throws IOException, XMLStreamException {

		boolean isMaskingAllowAPI = false;
		org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
				.getAxis2MessageContext();
		RelayUtils.buildMessage(axis2MessageContext);

		String jsonString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) messageContext).getAxis2MessageContext());
		JSONObject jsonBody = new JSONObject(jsonString);

		// Get Requested API type
		APIType apiType = HandlerUtils.getAPIType(messageContext);

		if (APIType.PAYMENT.equals(apiType) ) {
			JSONObject objAmountTransaction = (JSONObject) jsonBody.get("amountTransaction");
			String transactionOperationStatus = objAmountTransaction.get("transactionOperationStatus").toString();
            String userId = objAmountTransaction.get("endUserId").toString();
			if (transactionOperationStatus.equalsIgnoreCase("Charged")) {
				isMaskingAllowAPI = true;
			}

		} else if (APIType.SMS.equals(apiType) ) {
			if (!jsonBody.isNull("outboundSMSMessageRequest")) {
				JSONObject outboundSMSMessageRequest = (JSONObject) jsonBody.get("outboundSMSMessageRequest");
				if(!outboundSMSMessageRequest.isNull("address")) {
					isMaskingAllowAPI = true;
				}
			}
		}
		return isMaskingAllowAPI;
	}

    /**
     *  Update JSON payload
     * @param jsonBody
     * @param messageContext
     */
	private void updateJsonPayload(String jsonBody, MessageContext messageContext) {
		if(jsonBody != null) {
			JsonUtil.newJsonPayload(((Axis2MessageContext) messageContext).getAxis2MessageContext(), jsonBody,
					true, true);
		}
	}

    /**
     *  Set API endpoint  URL
	 *  This should be ONLY called when UserAnonymization is Enabled
     * @param messageContext
     */
	private void setEndpointURL(MessageContext messageContext) {
		Object headers = ((Axis2MessageContext) messageContext).getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		Map headersMap = (Map) headers;
		String resource = (String)headersMap.get("RESOURCE");
		// Properties from API-manager.xml
		String endpointURL = null;
		APIType type = HandlerUtils.getAPIType(messageContext);
		if(APIType.PAYMENT.equals(type)) {
			endpointURL = HandlerUtils.getUrlProperty("ESBPaymentEndpointURL") + resource;
		} else if (APIType.SMS.equals(type))  {
			endpointURL = HandlerUtils.getUrlProperty("ESBSMSEndpointURL") + resource;
		}
		EndpointReference endpointReference = new EndpointReference();
		endpointReference.setAddress(endpointURL);
		messageContext.setTo(endpointReference);
		messageContext.setProperty("CUSTOM_ENDPOINT", "true");
	}
}
