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
package org.training.storefront.controllers.pages;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.user.UserService;
import org.training.storefront.controllers.ControllerConstants;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.CookieGenerator;


/**
 * Login Controller. Handles login and register for the account flow.
 */
@Controller
@Scope("tenant")
@RequestMapping(value = "/login")
public class LoginPageController extends AbstractLoginPageController
{
	public static final String SECURE_GUID_SESSION_KEY = "acceleratorSecureGUID";

	@Resource(name = "guidCookieGenerator")
	private CookieGenerator cookieGenerator;

	@Resource(name = "httpSessionRequestCache")
	private HttpSessionRequestCache httpSessionRequestCache;

	@Resource
	private UserService userService;

	public void setHttpSessionRequestCache(final HttpSessionRequestCache accHttpSessionRequestCache)
	{
		this.httpSessionRequestCache = accHttpSessionRequestCache;
	}


	@RequestMapping(method = RequestMethod.GET)
	public String doLogin(@RequestHeader(value = "referer", required = false) final String referer,
			@RequestParam(value = "error", defaultValue = "false") final boolean loginError, final Model model,
			final HttpServletRequest request, final HttpServletResponse response, final HttpSession session)
			throws CMSItemNotFoundException, IOException
	{

		final UserModel user = userService.getCurrentUser();
		final boolean isUserAnonymous = user == null || userService.isAnonymousUser(user);
		final String guid = (String) request.getSession().getAttribute(SECURE_GUID_SESSION_KEY);

		if (!doRedirect(request, response, isUserAnonymous, guid))
		{
			return REDIRECT_PREFIX + ROOT;
		}

		if (!loginError)
		{
			storeReferer(referer, request, response);
		}
		return getDefaultLoginPage(loginError, session, model);
	}


	protected boolean doRedirect(final HttpServletRequest request, final HttpServletResponse response,
			final boolean isUserAnonymous, final String guid)
	{
		boolean redirect = true;

		if (!isUserAnonymous && guid != null && request.getCookies() != null)
		{
			final String guidCookieName = cookieGenerator.getCookieName();
			if (guidCookieName != null)
			{
				for (final Cookie cookie : request.getCookies())
				{
					if (guidCookieName.equals(cookie.getName()))
					{
						if (guid.equals(cookie.getValue()))
						{
							redirect = false;
							break;
						}
						else
						{
							cookieGenerator.removeCookie(response);
						}
					}
				}
			}
		}
		return redirect;
	}

	@Override
	protected String getLoginView()
	{
		return ControllerConstants.Views.Pages.Account.AccountLoginPage;
	}

	@Override
	protected String getSuccessRedirect(final HttpServletRequest request, final HttpServletResponse response)
	{
		if (httpSessionRequestCache.getRequest(request, response) != null)
		{
			return httpSessionRequestCache.getRequest(request, response).getRedirectUrl();
		}

		return "/my-account";
	}

	@Override
	protected AbstractPageModel getLoginCmsPage() throws CMSItemNotFoundException
	{
		return getContentPageForLabelOrId("login");
	}

	protected void storeReferer(final String referer, final HttpServletRequest request, final HttpServletResponse response)
	{
		if (StringUtils.isNotBlank(referer))
		{
			httpSessionRequestCache.saveRequest(request, response);
		}
	}

}
