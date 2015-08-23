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
package org.training.storefront.controllers.pages.checkout;

import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import org.training.storefront.controllers.pages.AbstractPageController;

import javax.annotation.Resource;


/**
 * Base controller for all checkout page controllers. Provides common functionality for all checkout page controllers.
 */
public abstract class AbstractCheckoutController extends AbstractPageController
{
	@Resource(name = "b2bCheckoutFacade")
	private de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFacade b2bCheckoutFacade;

	@Resource(name = "b2bCheckoutFlowFacade")
	private de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFlowFacade b2bCheckoutFlowFacade;

	@Deprecated
	@Resource(name = "b2bCheckoutFacade")
	private CheckoutFacade checkoutFacade;

	@Resource(name = "i18NFacade")
	private I18NFacade i18NFacade;

	public de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFacade getB2BCheckoutFacade()
	{
		return b2bCheckoutFacade;
	}

	@Deprecated
	public CheckoutFacade getCheckoutFacade()
	{
		return checkoutFacade;
	}

	public I18NFacade getI18NFacade()
	{
		return i18NFacade;
	}

	public de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFlowFacade getB2bCheckoutFlowFacade()
	{
		return b2bCheckoutFlowFacade;
	}

	/**
	 * Checks if there are any items in the cart.
	 * 
	 * @return returns true if items found in cart.
	 */
	protected boolean hasItemsInCart()
	{
		final CartData cartData = getCheckoutFacade().getCheckoutCart();

		return (cartData.getEntries() != null && !cartData.getEntries().isEmpty());
	}
}
