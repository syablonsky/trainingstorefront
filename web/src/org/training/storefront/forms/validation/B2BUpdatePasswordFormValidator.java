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

import org.training.storefront.forms.UpdatePasswordForm;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


/**
 * Validator for {@link UpdatePasswordForm}.
 *
 */
@Component("b2bUpdatePasswordFormValidator")
@Scope("tenant")
public class B2BUpdatePasswordFormValidator implements Validator
{
	@Resource(name = "passwordPattern")
	private String passwordPattern;

	@Override
	public boolean supports(final Class<?> clazz)
	{
		return UpdatePasswordForm.class.equals(clazz);
	}

	@Override
	public void validate(final Object obj, final Errors errors)
	{
		final UpdatePasswordForm form = (UpdatePasswordForm) obj;
		if (StringUtils.isBlank(form.getCurrentPassword()))
		{
			errors.rejectValue("currentPassword", "profile.currentPassword.invalid");
		}
		if (!matchPattern(form.getNewPassword()))
		{
			errors.rejectValue("newPassword", "updatePwd.pwd.invalid");
		}
		if (!matchPattern(form.getCheckNewPassword()))
		{
			errors.rejectValue("checkNewPassword", "updatePwd.checkPwd.invalid");
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

	public void isCheckEquals(final UpdatePasswordForm form, final Errors errors)
	{
		if (!form.getNewPassword().equals(form.getCheckNewPassword()))
		{
			errors.rejectValue("checkNewPassword", "validation.checkPwd.equals", new Object[] {}, "validation.checkPwd.equals");
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
