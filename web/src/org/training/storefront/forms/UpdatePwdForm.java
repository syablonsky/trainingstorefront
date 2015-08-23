/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2015 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 *
 *  
 */
package org.training.storefront.forms;

import org.training.storefront.forms.validation.EqualAttributes;



/**
 * Form object for updating the password.
 */
@EqualAttributes(message = "{validation.checkPwd.equals}", value =
{ "pwd", "checkPwd" })
public class UpdatePwdForm
{
	private String pwd;
	private String checkPwd;
	private String token;

	public String getPwd()
	{
		return pwd;
	}

	public void setPwd(final String pwd)
	{
		this.pwd = pwd;
	}

	public String getCheckPwd()
	{
		return checkPwd;
	}

	public void setCheckPwd(final String checkPwd)
	{
		this.checkPwd = checkPwd;
	}

	public String getToken()
	{
		return token;
	}

	public void setToken(final String token)
	{
		this.token = token;
	}
}
