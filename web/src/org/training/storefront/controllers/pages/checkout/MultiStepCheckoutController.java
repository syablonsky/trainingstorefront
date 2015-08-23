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

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductData;
import org.training.storefront.annotations.RequireHardLogIn;
import org.training.storefront.controllers.ControllerConstants;
import org.training.storefront.controllers.util.GlobalMessages;
import org.training.storefront.security.B2BUserGroupProvider;

import java.util.Arrays;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * MultiStepCheckoutController
 */
@Controller
@Scope("tenant")
@RequestMapping(value = "/checkout/multi")
public class MultiStepCheckoutController extends AbstractCheckoutController
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(MultiStepCheckoutController.class);

	private static final String MULTI_STEP_CHECKOUT_CMS_PAGE_LABEL = "multiStepCheckout";
	private static final String REDIRECT_URL_CART = REDIRECT_PREFIX + "/cart";

	@Resource(name = "b2bUserGroupProvider")
	private B2BUserGroupProvider b2bUserGroupProvider;

	@Resource(name = "b2bProductFacade")
	private ProductFacade productFacade;

	/**
	 * This is the entry point (first page) for the the multi-step checkout process. The page returned by this call acts
	 * as a template landing page and an example for actual implementation.
	 * 
	 * @param model
	 *           - the model for the view.
	 * @return - the multi-step checkout page if the basket contains any items or the cart page otherwise.
	 * @throws CMSItemNotFoundException
	 *            - when a CMS page is not found
	 */
	@RequestMapping(method = RequestMethod.GET)
	@RequireHardLogIn
	public String gotoFirstStep(final Model model) throws CMSItemNotFoundException
	{
		if (!b2bUserGroupProvider.isCurrentUserAuthorizedToCheckOut())
		{
			GlobalMessages.addErrorMessage(model, "checkout.error.invalid.accountType");
			return FORWARD_PREFIX + "/cart";
		}

		if (hasItemsInCart())
		{
			final CartData cartData = getCheckoutFacade().getCheckoutCart();
			if (cartData.getEntries() != null && !cartData.getEntries().isEmpty())
			{
				for (final OrderEntryData entry : cartData.getEntries())
				{
					final String productCode = entry.getProduct().getCode();
					final ProductData product = productFacade.getProductForCodeAndOptions(productCode,
							Arrays.asList(ProductOption.BASIC, ProductOption.PRICE));
					entry.setProduct(product);
				}
			}

			model.addAttribute("cartData", cartData);
			model.addAttribute("allItems", cartData.getEntries());
			model.addAttribute("metaRobots", "no-index,no-follow");
			storeCmsPageInModel(model, getContentPageForLabelOrId(MULTI_STEP_CHECKOUT_CMS_PAGE_LABEL));
			setUpMetaDataForContentPage(model, getContentPageForLabelOrId(MULTI_STEP_CHECKOUT_CMS_PAGE_LABEL));

			return ControllerConstants.Views.Pages.MultiStepCheckout.CheckoutSampleLandingPage;
		}

		LOG.info("Missing or empty cart");
		return REDIRECT_URL_CART;
	}
}
