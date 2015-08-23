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
package org.training.storefront.forms.validation;

import org.training.storefront.forms.UpdatePwdForm;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


/**
 * Validator for {@link UpdatePwdForm}.
 *
 */
@Component("b2bUpdatePwdFormValidator")
@Scope("tenant")
public class B2BUpdatePwdFormValidator implements Validator
{
	@Resource(name = "passwordPattern")
	private String passwordPattern;

	@Override
	public boolean supports(final Class<?> clazz)
	{
		return UpdatePwdForm.class.equals(clazz);
	}

	@Override
	public void validate(final Object obj, final Errors errors)
	{
		final UpdatePwdForm form = (UpdatePwdForm) obj;
		if (StringUtils.isBlank(form.getPwd()))
		{
			errors.rejectValue("pwd", "updatePwd.pwd.invalid");
		}
		if (!matchPattern(form.getCheckPwd()))
		{
			errors.rejectValue("checkPwd", "updatePwd.checkPwd.invalid");
		}
		isCheckEquals(form, errors);
	}

	public boolean matchPattern(final String password)
	{
		boolean isValid = false;
		if (StringUtils.isNotBlank(passwordPattern))
		{
			isValid = password.matches(passwordPattern);
		}
		return isValid;
	}

	public void isCheckEquals(final UpdatePwdForm form, final Errors errors)
	{
		if (!form.getPwd().equals(form.getCheckPwd()))
		{
			errors.rejectValue("checkPwd", "validation.checkPwd.equals", new Object[] {}, "validation.checkPwd.equals");
		}
	}

	public String getPasswordPattern()
	{
		return passwordPattern;
	}

	public void setPasswordPattern(final String passwordPattern)
	{
		this.passwordPattern = passwordPattern;
	}

}
