/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 *  
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
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
package com.wso2telco.dep.operatorservice.exception;

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.core.dbutils.exception.ThrowableError;

public class APIException extends BusinessException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5898653716071770814L;

	public APIException(ThrowableError error) {

		super(error);
	}
	
	public enum APIErrorType implements ThrowableError {

		INVALID_API_ID("APIE0001", "Invalid API id"),
		INVALID_API_NAME("APIE0002", "Invalid API name");

		final String code;
		final String msg;

		APIErrorType(final String code, final String msg) {

			this.code = code;
			this.msg = msg;
		}

		public String getMessage() {
			
			return this.msg;
		}

		public String getCode() {
			
			return this.code;
		}
	}

	@Override
	public String toString() {
		
		return "APIException [getErrorType()=" + getErrorType() + ", getMessage()=" + getMessage() + "]";
	}
}
