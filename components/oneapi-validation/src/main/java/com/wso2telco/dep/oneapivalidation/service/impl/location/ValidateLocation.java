/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * 
 * WSO2.Telco Inc. licences this file to you under  the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.dep.oneapivalidation.service.impl.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.util.UrlValidator;
import com.wso2telco.dep.oneapivalidation.util.Validation;
import com.wso2telco.dep.oneapivalidation.util.ValidationRule;

 
// TODO: Auto-generated Javadoc
/**
 * The Class ValidateLocation.
 */
public class ValidateLocation implements IServiceValidate {

    /** The validation rules. */
    private final String[] validationRules = {"queries","location"};

    static Logger logger = Logger.getLogger(ValidateLocation.class);

    private List<String> addresses = new ArrayList<String>();
    private double requestAccuracy = 0.0;
    
    /* (non-Javadoc)
     * @see com.wso2telco.oneapivalidation.service.IServiceValidate#validate(java.lang.String[])
     */
	public void validate(String[] params) throws CustomException {
		boolean foundAddressParam = false;

		if (params == null || params.length == 0) {
			throw new CustomException("SVC0002",
					new String[] { "Missing mandatory parameters address and requestedAccuracy" });
		} else {
			for (String param : Arrays.asList(params)) {
				if (param.contains("=")) {
					String[] pair = param.split("=");
					if (pair.length > 1 && pair[0].equalsIgnoreCase("address")) {
						foundAddressParam = true;
						this.addresses.add(pair[1]);
						if (logger.isDebugEnabled()) {
							logger.debug("Adding MSISDN number to request : " + pair[1]);
						}
					} else if (pair.length > 1 && pair[0].equalsIgnoreCase("requestedAccuracy")) {
						try {
							this.requestAccuracy = Double.parseDouble(pair[1]);
							if (logger.isDebugEnabled()) {
								logger.debug("Adding requestAccuracy to request : " + pair[1]);
							}
						} catch (NumberFormatException e) {
							throw new CustomException("SVC0002", new String[] { "requestedAccuracy" });
						}
					}
				} else {
					// This adds additional MSISDN numbers when user passes multiple MSISDNs
					// TODO. This Assume additional query params are always MSISDN. Therefore those add directory to
					// to address list. 
					// Future this need a validation logic to identify whether this is a valid MSISDN 
					if (param != null && !param.isEmpty()) {
						this.addresses.add(param);
						if (logger.isDebugEnabled()) {
							logger.debug("Adding MSISDN number to request : " + param);
						}
					}
				}
			}
		}

		if (this.addresses.size() == 0 && this.requestAccuracy == 0.0) {
			throw new CustomException("SVC0002", new String[] { "Missing mandatory parameters address and requestedAccuracy" });
		} else if (!foundAddressParam || this.addresses.size() == 0) {
			throw new CustomException("SVC0002", new String[] { "Missing mandatory parameter address" });
		} else if (this.requestAccuracy == 0.0) {
			throw new CustomException("SVC0002", new String[] { "Missing mandatory parameter requestedAccuracy" });
		}

		ValidationRule[] rules = {
				new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY_TEL, "address",
						this.addresses.toArray(new String[addresses.size()])),
				new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY_LOC_ACCURACY, "requestedAccuracy",
						this.requestAccuracy) };

		Validation.checkRequestParams(rules);

	}

    /* (non-Javadoc)
     * @see com.wso2telco.oneapivalidation.service.IServiceValidate#validate(java.lang.String)
     */
    public void validate(String json) throws CustomException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see com.wso2telco.oneapivalidation.service.IServiceValidate#validateUrl(java.lang.String)
     */
    public void validateUrl(String pathInfo) throws CustomException {
        String[] requestParts = null;
        if (pathInfo != null) {
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            requestParts = pathInfo.split("/");
        }

        //remove batchSize param
        int reqlength = requestParts.length -1;
        requestParts[reqlength] = requestParts[reqlength].split("\\?")[0];

        UrlValidator.validateRequest(requestParts, validationRules);
    }
    
    /**
     * Returns array of MSISDNs
     */
    public String[] getMsisdns() {
		return this.addresses.toArray(new String[addresses.size()]);
	}

	public double getRequestAccuracy() {
		return requestAccuracy;
	}
}
