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

import de.hybris.platform.b2bacceleratorfacades.api.company.CostCenterFacade;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.exception.EntityValidationException;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BCommentData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BCostCenterData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BDaysOfWeekData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BPaymentTypeData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CardTypeData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.DeliveryModeData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.order.data.ZoneDeliveryModeData;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.TitleData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationStatus;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.order.InvalidCartException;
import org.training.storefront.annotations.RequireHardLogIn;
import org.training.storefront.breadcrumb.impl.ContentPageBreadcrumbBuilder;
import org.training.storefront.constants.WebConstants;
import org.training.storefront.controllers.ControllerConstants;
import org.training.storefront.controllers.util.GlobalMessages;
import org.training.storefront.forms.AddressForm;
import org.training.storefront.forms.PaymentDetailsForm;
import org.training.storefront.forms.PlaceOrderForm;
import org.training.storefront.forms.validation.PaymentDetailsValidator;
import org.training.storefront.security.B2BUserGroupProvider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * SingleStepCheckoutController
 */
@Controller
@Scope("tenant")
@RequestMapping(value = "/checkout/single")
public class SingleStepCheckoutController extends AbstractCheckoutController
{
	@SuppressWarnings("unused")
	protected static final Logger LOG = Logger.getLogger(SingleStepCheckoutController.class);

	private static final String SINGLE_STEP_CHECKOUT_SUMMARY_CMS_PAGE = "singleStepCheckoutSummaryPage";

	@Resource(name = "paymentDetailsValidator")
	private PaymentDetailsValidator paymentDetailsValidator;

	@Resource(name = "b2bProductFacade")
	private ProductFacade productFacade;

	@Resource(name = "costCenterFacade")
	private CostCenterFacade costCenterFacade;

	@Resource(name = "b2bUserGroupProvider")
	private B2BUserGroupProvider b2bUserGroupProvider;

	@Resource(name = "b2bContentPageBreadcrumbBuilder")
	private ContentPageBreadcrumbBuilder contentPageBreadcrumbBuilder;

	@Deprecated
	@Resource(name = "cartFacade")
	private CartFacade doNotUsecartFacade;

	@ModelAttribute("titles")
	public Collection<TitleData> getTitles()
	{
		return getUserFacade().getTitles();
	}

	@ModelAttribute("countries")
	public Collection<CountryData> getCountries()
	{
		return getCheckoutFacade().getDeliveryCountries();
	}

	@ModelAttribute("billingCountries")
	public Collection<CountryData> getBillingCountries()
	{
		return getCheckoutFacade().getBillingCountries();
	}

	@ModelAttribute("costCenters")
	public List<? extends B2BCostCenterData> getVisibleActiveCostCenters()
	{
		final List<? extends B2BCostCenterData> costCenterData = costCenterFacade.getActiveCostCenters();
		return costCenterData == null ? Collections.<B2BCostCenterData> emptyList() : costCenterData;
	}

	@ModelAttribute("paymentTypes")
	public Collection<B2BPaymentTypeData> getAllB2BPaymentTypes()
	{
		return getB2BCheckoutFacade().getPaymentTypes();
	}

	@ModelAttribute("daysOfWeek")
	public Collection<B2BDaysOfWeekData> getAllDaysOfWeek()
	{
		return getB2BCheckoutFacade().getDaysOfWeekForReplenishmentCheckoutSummary();
	}

	@ModelAttribute("startYears")
	public List<SelectOption> getStartYears()
	{
		final List<SelectOption> startYears = new ArrayList<SelectOption>();
		final Calendar calender = new GregorianCalendar();

		for (int i = calender.get(Calendar.YEAR); i > (calender.get(Calendar.YEAR) - 6); i--)
		{
			startYears.add(new SelectOption(String.valueOf(i), String.valueOf(i)));
		}

		return startYears;
	}

	@ModelAttribute("expiryYears")
	public List<SelectOption> getExpiryYears()
	{
		final List<SelectOption> expiryYears = new ArrayList<SelectOption>();
		final Calendar calender = new GregorianCalendar();

		for (int i = calender.get(Calendar.YEAR); i < (calender.get(Calendar.YEAR) + 11); i++)
		{
			expiryYears.add(new SelectOption(String.valueOf(i), String.valueOf(i)));
		}

		return expiryYears;
	}

	@ModelAttribute("cardTypes")
	public Collection<CardTypeData> getCardTypes()
	{
		return getCheckoutFacade().getSupportedCardTypes();
	}

	@ModelAttribute("months")
	public List<SelectOption> getMonths()
	{
		final List<SelectOption> months = new ArrayList<SelectOption>();

		months.add(new SelectOption("1", "01"));
		months.add(new SelectOption("2", "02"));
		months.add(new SelectOption("3", "03"));
		months.add(new SelectOption("4", "04"));
		months.add(new SelectOption("5", "05"));
		months.add(new SelectOption("6", "06"));
		months.add(new SelectOption("7", "07"));
		months.add(new SelectOption("8", "08"));
		months.add(new SelectOption("9", "09"));
		months.add(new SelectOption("10", "10"));
		months.add(new SelectOption("11", "11"));
		months.add(new SelectOption("12", "12"));

		return months;
	}

	@InitBinder
	protected void initBinder(final HttpServletRequest request, final ServletRequestDataBinder binder)
	{
		final DateFormat dateFormat = new SimpleDateFormat(getMessageSource().getMessage("text.store.dateformat", null,
				getI18nService().getCurrentLocale()));
		final CustomDateEditor editor = new CustomDateEditor(dateFormat, true);
		binder.registerCustomEditor(Date.class, editor);
	}

	@RequestMapping(method =
	{ RequestMethod.GET, RequestMethod.POST })
	public String checkoutSummary()
	{
		if (hasItemsInCart())
		{
			return REDIRECT_PREFIX + "/checkout/single/summary";
		}
		return REDIRECT_PREFIX + "/cart";
	}

	@RequestMapping(value = "/summary", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public String checkoutSummary(final Model model) throws CMSItemNotFoundException
	{

		if (!b2bUserGroupProvider.isCurrentUserAuthorizedToCheckOut())
		{
			GlobalMessages.addErrorMessage(model, "checkout.error.invalid.accountType");
			return FORWARD_PREFIX + "/cart";
		}

		if (!hasItemsInCart())
		{
			// no items in the cart
			return FORWARD_PREFIX + "/cart";
		}

		getCheckoutFacade().setDeliveryAddressIfAvailable();
		getCheckoutFacade().setDeliveryModeIfAvailable();
		getCheckoutFacade().setPaymentInfoIfAvailable();

		// Set to default payment type
		final B2BPaymentTypeData paymentTypeData = new B2BPaymentTypeData();
		paymentTypeData.setCode(CheckoutPaymentType.ACCOUNT.getCode());

		final CartData tempCartData = new CartData();
		tempCartData.setPaymentType(paymentTypeData);

		final CartData cartData = getB2BCheckoutFacade().updateCheckoutCart(tempCartData);

		//final CartData cartData = getCheckoutFacade().getCheckoutCart();
		if (cartData.getEntries() != null && !cartData.getEntries().isEmpty())
		{
			for (final OrderEntryData entry : cartData.getEntries())
			{
				final String productCode = entry.getProduct().getCode();
				final ProductData product = productFacade.getProductForCodeAndOptions(productCode, Arrays.asList(ProductOption.BASIC,
						ProductOption.PRICE, ProductOption.PRICE_RANGE, ProductOption.VARIANT_MATRIX));
				entry.setProduct(product);
			}
		}

		model.addAttribute("cartData", cartData);
		model.addAttribute("allItems", cartData.getEntries());
		model.addAttribute("deliveryAddress", cartData.getDeliveryAddress());
		model.addAttribute("deliveryMode", cartData.getDeliveryMode());
		model.addAttribute("paymentInfo", cartData.getPaymentInfo());
		model.addAttribute("costCenter", cartData.getCostCenter());
		model.addAttribute("quoteText", new B2BCommentData());
		// TODO:Make configuration hmc driven than hardcoding in controllers
		model.addAttribute("nDays", getNumberRange(1, 30));
		model.addAttribute("nthDayOfMonth", getNumberRange(1, 31));
		model.addAttribute("nthWeek", getNumberRange(1, 12));

		model.addAttribute(new AddressForm());
		model.addAttribute(new PaymentDetailsForm());
		if (!model.containsAttribute("placeOrderForm"))
		{
			final PlaceOrderForm placeOrderForm = new PlaceOrderForm();
			// TODO: Make setting of default recurrence enum value hmc driven rather hard coding in controller
			placeOrderForm.setReplenishmentRecurrence(B2BReplenishmentRecurrenceEnum.MONTHLY);
			placeOrderForm.setnDays("14");
			final List<DayOfWeek> daysOfWeek = new ArrayList<DayOfWeek>();
			daysOfWeek.add(DayOfWeek.MONDAY);
			placeOrderForm.setnDaysOfWeek(daysOfWeek);
			model.addAttribute("placeOrderForm", placeOrderForm);
		}
		storeCmsPageInModel(model, getContentPageForLabelOrId(SINGLE_STEP_CHECKOUT_SUMMARY_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(SINGLE_STEP_CHECKOUT_SUMMARY_CMS_PAGE));
		model.addAttribute("metaRobots", "no-index,no-follow");
		return ControllerConstants.Views.Pages.SingleStepCheckout.CheckoutSummaryPage;
	}

	@RequestMapping(value = "/getProductVariantMatrix", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getProductVariantMatrix(@RequestParam("productCode") final String productCode, final Model model)
	{
		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, Arrays.asList(ProductOption.BASIC,
				ProductOption.CATEGORIES, ProductOption.VARIANT_MATRIX_BASE, ProductOption.VARIANT_MATRIX_PRICE,
				ProductOption.VARIANT_MATRIX_MEDIA, ProductOption.VARIANT_MATRIX_STOCK));

		model.addAttribute("product", productData);

		return ControllerConstants.Views.Fragments.Checkout.ReadOnlyExpandedOrderForm;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/getCheckoutCart.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public CartData getCheckoutCart()
	{
		final CartData cartData = getCheckoutFacade().getCheckoutCart();

		return cartData;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/getCostCenters.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public List<? extends B2BCostCenterData> getCostCenters()
	{
		return getVisibleActiveCostCenters();
	}

	@ResponseBody
	@RequestMapping(value = "/summary/getDeliveryAddresses.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public List<? extends AddressData> getDeliveryAddresses()
	{
		final List<? extends AddressData> deliveryAddresses = getCheckoutFacade().getSupportedDeliveryAddresses(true);
		if (deliveryAddresses == null)
		{
			return Collections.<AddressData> emptyList();
		}
		for (final AddressData address : deliveryAddresses)
		{
			if (getUserFacade().isDefaultAddress(address.getId()))
			{
				address.setDefaultAddress(true);
				break;
			}
		}
		return deliveryAddresses;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setDefaultAddress.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public List<? extends AddressData> setDefaultAddress(@RequestParam(value = "addressId") final String addressId)
	{
		getUserFacade().setDefaultAddress(getUserFacade().getAddressForCode(addressId));
		return getDeliveryAddresses();
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setDeliveryAddress.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public CartData setDeliveryAddress(@RequestParam(value = "addressId") final String addressId)
	{
		final AddressData addressData = new AddressData();
		addressData.setId(addressId);

		final CartData cartData = new CartData();
		cartData.setDeliveryAddress(addressData);

		return getB2BCheckoutFacade().updateCheckoutCart(cartData);
	}

	@ResponseBody
	@RequestMapping(value = "/summary/getDeliveryModes.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public List<? extends DeliveryModeData> getDeliveryModes()
	{
		final List<? extends DeliveryModeData> deliveryModes = getCheckoutFacade().getSupportedDeliveryModes();
		return deliveryModes == null ? Collections.<ZoneDeliveryModeData> emptyList() : deliveryModes;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setDeliveryMode.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public CartData setDeliveryMode(@RequestParam(value = "modeCode") final String modeCode)
	{
		if (getCheckoutFacade().setDeliveryMode(modeCode))
		{
			final CartData cartData = getCheckoutFacade().getCheckoutCart();
			return cartData;
		}
		else
		{
			return null;
		}
	}

	@RequestMapping(value = "/summary/getDeliveryAddressForm.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public String getDeliveryAddressForm(final Model model, @RequestParam(value = "addressId") final String addressId,
			@RequestParam(value = "createUpdateStatus") final String createUpdateStatus)
	{
		AddressData addressData = null;
		if (addressId != null && !addressId.isEmpty())
		{
			addressData = getCheckoutFacade().getDeliveryAddressForCode(addressId);
		}

		final AddressForm addressForm = new AddressForm();

		final boolean hasAddressData = addressData != null;
		if (hasAddressData)
		{
			addressForm.setAddressId(addressData.getId());
			addressForm.setTitleCode(addressData.getTitleCode());
			addressForm.setFirstName(addressData.getFirstName());
			addressForm.setLastName(addressData.getLastName());
			addressForm.setLine1(addressData.getLine1());
			addressForm.setLine2(addressData.getLine2());
			addressForm.setTownCity(addressData.getTown());
			addressForm.setPostcode(addressData.getPostalCode());
			addressForm.setCountryIso(addressData.getCountry().getIsocode());
			addressForm.setShippingAddress(Boolean.valueOf(addressData.isShippingAddress()));
			addressForm.setBillingAddress(Boolean.valueOf(addressData.isBillingAddress()));
		}

		model.addAttribute("edit", Boolean.valueOf(hasAddressData));
		model.addAttribute("noAddresses", Boolean.valueOf(getUserFacade().isAddressBookEmpty()));

		model.addAttribute(addressForm);
		model.addAttribute("createUpdateStatus", createUpdateStatus);

		// Work out if the address form should be displayed based on the payment type
		final B2BPaymentTypeData paymentType = getCheckoutFacade().getCheckoutCart().getPaymentType();
		final boolean payOnAccount = paymentType != null && CheckoutPaymentType.ACCOUNT.getCode().equals(paymentType.getCode());
		model.addAttribute("showAddressForm", Boolean.valueOf(!payOnAccount));

		return ControllerConstants.Views.Fragments.SingleStepCheckout.DeliveryAddressFormPopup;
	}

	@RequestMapping(value = "/summary/createUpdateDeliveryAddress.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public String createUpdateDeliveryAddress(final Model model, @Valid final AddressForm form, final BindingResult bindingResult)
	{
		if (bindingResult.hasErrors())
		{
			model.addAttribute("edit", Boolean.valueOf(StringUtils.isNotBlank(form.getAddressId())));
			// Work out if the address form should be displayed based on the payment type
			final B2BPaymentTypeData paymentType = getCheckoutFacade().getCheckoutCart().getPaymentType();
			final boolean payOnAccount = paymentType != null && CheckoutPaymentType.ACCOUNT.getCode().equals(paymentType.getCode());
			model.addAttribute("showAddressForm", Boolean.valueOf(!payOnAccount));

			return ControllerConstants.Views.Fragments.SingleStepCheckout.DeliveryAddressFormPopup;
		}

		// create delivery address and set it on cart
		final AddressData addressData = new AddressData();
		addressData.setId(form.getAddressId());
		addressData.setTitleCode(form.getTitleCode());
		addressData.setFirstName(form.getFirstName());
		addressData.setLastName(form.getLastName());
		addressData.setLine1(form.getLine1());
		addressData.setLine2(form.getLine2());
		addressData.setTown(form.getTownCity());
		addressData.setPostalCode(form.getPostcode());
		addressData.setCountry(getI18NFacade().getCountryForIsocode(form.getCountryIso()));
		addressData.setShippingAddress(Boolean.TRUE.equals(form.getShippingAddress())
				|| Boolean.TRUE.equals(form.getSaveInAddressBook()));

		addressData.setVisibleInAddressBook(Boolean.TRUE.equals(form.getSaveInAddressBook())
				|| StringUtils.isNotBlank(form.getAddressId()));
		addressData.setDefaultAddress(Boolean.TRUE.equals(form.getDefaultAddress()));

		if (StringUtils.isBlank(form.getAddressId()))
		{
			getUserFacade().addAddress(addressData);
		}
		else
		{
			getUserFacade().editAddress(addressData);
		}

		getCheckoutFacade().setDeliveryAddress(addressData);

		if (getCheckoutFacade().getCheckoutCart().getDeliveryMode() == null)
		{
			getCheckoutFacade().setDeliveryModeIfAvailable();
		}

		model.addAttribute("createUpdateStatus", "Success");
		model.addAttribute("addressId", addressData.getId());

		return REDIRECT_PREFIX + "/checkout/single/summary/getDeliveryAddressForm.json?addressId=" + addressData.getId()
				+ "&createUpdateStatus=Success";
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setCostCenter.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public CartData setCostCenter(@RequestParam(value = "costCenterId") final String costCenterId)
	{
		final B2BCostCenterData costCenter = new B2BCostCenterData();
		costCenter.setCode(costCenterId);

		CartData cartData = new CartData();
		cartData.setCostCenter(costCenter);

		cartData = getB2BCheckoutFacade().updateCheckoutCart(cartData);

		return cartData;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/updateCostCenter.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public CartData updateCostCenterForCart(@RequestParam(value = "costCenterId") final String costCenterId)
	{
		final B2BCostCenterData costCenter = new B2BCostCenterData();
		costCenter.setCode(costCenterId);

		CartData cartData = new CartData();
		cartData.setCostCenter(costCenter);

		cartData = getB2BCheckoutFacade().updateCheckoutCart(cartData);

		return cartData;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/getSavedCards.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public List<CCPaymentInfoData> getSavedCards()
	{
		final List<CCPaymentInfoData> paymentInfos = getUserFacade().getCCPaymentInfos(true);
		return paymentInfos == null ? Collections.<CCPaymentInfoData> emptyList() : paymentInfos;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setPaymentDetails.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public CartData setPaymentDetails(@RequestParam(value = "paymentId") final String paymentId)
	{
		if (StringUtils.isNotBlank(paymentId) && getCheckoutFacade().setPaymentDetails(paymentId))
		{
			final CartData cartData = getCheckoutFacade().getCheckoutCart();
			return cartData;
		}

		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setPaymentType.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public CartData setPaymentType(@RequestParam(value = "paymentType") final String paymentType)
	{
		final B2BPaymentTypeData paymentTypeData = new B2BPaymentTypeData();
		paymentTypeData.setCode(paymentType);

		CartData cartData = new CartData();
		cartData.setPaymentType(paymentTypeData);

		cartData = getB2BCheckoutFacade().updateCheckoutCart(cartData);
		return cartData;
	}

	@ResponseBody
	@RequestMapping(value = "/summary/setPurchaseOrderNumber.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public CartData setPurchaseOrderNumber(@RequestParam(value = "purchaseOrderNumber") final String purchaseOrderNumber)
	{
		CartData cartData = new CartData();
		cartData.setPurchaseOrderNumber(purchaseOrderNumber);
		cartData = getB2BCheckoutFacade().updateCheckoutCart(cartData);

		return cartData;
	}

	@RequestMapping(value = "/summary/getPaymentDetailsForm.json", method =
	{ RequestMethod.GET, RequestMethod.POST })
	@RequireHardLogIn
	public String getPaymentDetailsForm(final Model model, @RequestParam(value = "paymentId") final String paymentId,
			@RequestParam(value = "createUpdateStatus") final String createUpdateStatus)
	{
		CCPaymentInfoData paymentInfoData = null;
		if (StringUtils.isNotBlank(paymentId))
		{
			paymentInfoData = getUserFacade().getCCPaymentInfoForCode(paymentId);
		}

		final PaymentDetailsForm paymentDetailsForm = new PaymentDetailsForm();

		if (paymentInfoData != null)
		{
			paymentDetailsForm.setPaymentId(paymentInfoData.getId());
			paymentDetailsForm.setCardTypeCode(paymentInfoData.getCardType());
			paymentDetailsForm.setNameOnCard(paymentInfoData.getAccountHolderName());
			paymentDetailsForm.setCardNumber(paymentInfoData.getCardNumber());
			paymentDetailsForm.setStartMonth(paymentInfoData.getStartMonth());
			paymentDetailsForm.setStartYear(paymentInfoData.getStartYear());
			paymentDetailsForm.setExpiryMonth(paymentInfoData.getExpiryMonth());
			paymentDetailsForm.setExpiryYear(paymentInfoData.getExpiryYear());
			paymentDetailsForm.setSaveInAccount(Boolean.valueOf(paymentInfoData.isSaved()));
			paymentDetailsForm.setIssueNumber(paymentInfoData.getIssueNumber());

			final AddressForm addressForm = new AddressForm();
			final AddressData addressData = paymentInfoData.getBillingAddress();
			if (addressData != null)
			{
				addressForm.setAddressId(addressData.getId());
				addressForm.setTitleCode(addressData.getTitleCode());
				addressForm.setFirstName(addressData.getFirstName());
				addressForm.setLastName(addressData.getLastName());
				addressForm.setLine1(addressData.getLine1());
				addressForm.setLine2(addressData.getLine2());
				addressForm.setTownCity(addressData.getTown());
				addressForm.setPostcode(addressData.getPostalCode());
				addressForm.setCountryIso(addressData.getCountry().getIsocode());
				addressForm.setShippingAddress(Boolean.valueOf(addressData.isShippingAddress()));
				addressForm.setBillingAddress(Boolean.valueOf(addressData.isBillingAddress()));
			}

			paymentDetailsForm.setBillingAddress(addressForm);
		}

		model.addAttribute("edit", Boolean.valueOf(paymentInfoData != null));
		model.addAttribute("paymentInfoData", getUserFacade().getCCPaymentInfos(true));
		model.addAttribute(paymentDetailsForm);
		model.addAttribute("createUpdateStatus", createUpdateStatus);
		return ControllerConstants.Views.Fragments.SingleStepCheckout.PaymentDetailsFormPopup;
	}

	@RequestMapping(value = "/summary/createUpdatePaymentDetails.json", method = RequestMethod.POST)
	@RequireHardLogIn
	public String createUpdatePaymentDetails(final Model model, @Valid final PaymentDetailsForm form,
			final BindingResult bindingResult)
	{
		paymentDetailsValidator.validate(form, bindingResult);

		final boolean editMode = StringUtils.isNotBlank(form.getPaymentId());

		if (bindingResult.hasErrors())
		{
			model.addAttribute("edit", Boolean.valueOf(editMode));

			return ControllerConstants.Views.Fragments.SingleStepCheckout.PaymentDetailsFormPopup;
		}

		final CCPaymentInfoData paymentInfoData = new CCPaymentInfoData();
		paymentInfoData.setId(form.getPaymentId());
		paymentInfoData.setCardType(form.getCardTypeCode());
		paymentInfoData.setAccountHolderName(form.getNameOnCard());
		paymentInfoData.setCardNumber(form.getCardNumber());
		paymentInfoData.setStartMonth(form.getStartMonth());
		paymentInfoData.setStartYear(form.getStartYear());
		paymentInfoData.setExpiryMonth(form.getExpiryMonth());
		paymentInfoData.setExpiryYear(form.getExpiryYear());
		paymentInfoData.setSaved(Boolean.TRUE.equals(form.getSaveInAccount()));
		paymentInfoData.setIssueNumber(form.getIssueNumber());

		final AddressData addressData;
		if (!editMode && Boolean.FALSE.equals(form.getNewBillingAddress()))
		{
			addressData = getCheckoutCart().getDeliveryAddress();
			if (addressData == null)
			{
				GlobalMessages.addErrorMessage(model, "checkout.paymentMethod.createSubscription.billingAddress.noneSelected");

				model.addAttribute("edit", Boolean.valueOf(editMode));
				return ControllerConstants.Views.Fragments.SingleStepCheckout.PaymentDetailsFormPopup;
			}

			addressData.setBillingAddress(true); // mark this as billing address
		}
		else
		{
			final AddressForm addressForm = form.getBillingAddress();

			addressData = new AddressData();
			if (addressForm != null)
			{
				addressData.setId(addressForm.getAddressId());
				addressData.setTitleCode(addressForm.getTitleCode());
				addressData.setFirstName(addressForm.getFirstName());
				addressData.setLastName(addressForm.getLastName());
				addressData.setLine1(addressForm.getLine1());
				addressData.setLine2(addressForm.getLine2());
				addressData.setTown(addressForm.getTownCity());
				addressData.setPostalCode(addressForm.getPostcode());
				addressData.setCountry(getI18NFacade().getCountryForIsocode(addressForm.getCountryIso()));
				addressData.setShippingAddress(Boolean.TRUE.equals(addressForm.getShippingAddress()));
				addressData.setBillingAddress(Boolean.TRUE.equals(addressForm.getBillingAddress()));
			}
		}

		paymentInfoData.setBillingAddress(addressData);

		final CCPaymentInfoData newPaymentSubscription = getCheckoutFacade().createPaymentSubscription(paymentInfoData);
		if (newPaymentSubscription != null && StringUtils.isNotBlank(newPaymentSubscription.getSubscriptionId()))
		{
			if (Boolean.TRUE.equals(form.getSaveInAccount()) && getUserFacade().getCCPaymentInfos(true).size() <= 1)
			{
				getUserFacade().setDefaultPaymentInfo(newPaymentSubscription);
			}
			getCheckoutFacade().setPaymentDetails(newPaymentSubscription.getId());
		}
		else
		{
			GlobalMessages.addErrorMessage(model, "checkout.paymentMethod.createSubscription.failed");

			model.addAttribute("edit", Boolean.valueOf(editMode));
			return ControllerConstants.Views.Fragments.SingleStepCheckout.PaymentDetailsFormPopup;
		}

		model.addAttribute("createUpdateStatus", "Success");
		model.addAttribute("paymentId", newPaymentSubscription.getId());

		return REDIRECT_PREFIX + "/checkout/single/summary/getPaymentDetailsForm.json?paymentId=" + paymentInfoData.getId()
				+ "&createUpdateStatus=Success";
	}

	@RequestMapping(value = "/termsAndConditions")
	@RequireHardLogIn
	public String getTermsAndConditions(final Model model) throws CMSItemNotFoundException
	{
		final ContentPageModel pageForRequest = getCmsPageService().getPageForLabel("/termsAndConditions");
		storeCmsPageInModel(model, pageForRequest);
		setUpMetaDataForContentPage(model, pageForRequest);
		model.addAttribute(WebConstants.BREADCRUMBS_KEY, contentPageBreadcrumbBuilder.getBreadcrumbs(pageForRequest));
		return ControllerConstants.Views.Fragments.Checkout.TermsAndConditionsPopup;
	}

	@RequestMapping(value = "/placeOrder")
	@RequireHardLogIn
	public String placeOrder(final Model model, @Valid final PlaceOrderForm placeOrderForm, final BindingResult result)
			throws CMSItemNotFoundException
	{
		if (result.hasErrors())
		{
			GlobalMessages.addErrorMessage(model, "checkout.placeOrder.failed");

			placeOrderForm.setTermsCheck(false);
			model.addAttribute(placeOrderForm);

			return checkoutSummary(model);
		}

		// placeOrderData is the same as placeOrderForm, should use placeOrderData replace placeOrderForm
		final PlaceOrderData placeOrderData = new PlaceOrderData();
		placeOrderData.setNDays(placeOrderForm.getnDays());
		placeOrderData.setNDaysOfWeek(placeOrderForm.getnDaysOfWeek());
		placeOrderData.setNegotiateQuote(placeOrderForm.isNegotiateQuote());
		placeOrderData.setNthDayOfMonth(placeOrderForm.getNthDayOfMonth());
		placeOrderData.setNWeeks(placeOrderForm.getnWeeks());
		placeOrderData.setQuoteRequestDescription(placeOrderForm.getQuoteRequestDescription());
		placeOrderData.setReplenishmentOrder(placeOrderForm.isReplenishmentOrder());
		placeOrderData.setReplenishmentRecurrence(placeOrderForm.getReplenishmentRecurrence());
		placeOrderData.setReplenishmentStartDate(placeOrderForm.getReplenishmentStartDate());
		placeOrderData.setSecurityCode(placeOrderForm.getSecurityCode());
		placeOrderData.setTermsCheck(placeOrderForm.isTermsCheck());

		try
		{
			final AbstractOrderData orderData = getB2BCheckoutFacade().placeOrder(placeOrderData);

			if (placeOrderForm.isReplenishmentOrder())
			{
				return REDIRECT_PREFIX + "/checkout/replenishmentConfirmation/" + ((ScheduledCartData) orderData).getJobCode();
			}
			else if (placeOrderForm.isNegotiateQuote())
			{
				return REDIRECT_PREFIX + "/checkout/quoteOrderConfirmation/" + orderData.getCode();
			}
			else
			{
				return REDIRECT_PREFIX + "/checkout/orderConfirmation/" + orderData.getCode();
			}
		}
		catch (final EntityValidationException ve)
		{
			GlobalMessages.addErrorMessage(model, ve.getLocalizedMessage());

			placeOrderForm.setTermsCheck(false);
			model.addAttribute(placeOrderForm);

			return checkoutSummary(model);
		}
		catch (final Exception e)
		{
			GlobalMessages.addErrorMessage(model, "checkout.placeOrder.failed");

			placeOrderForm.setTermsCheck(false);
			model.addAttribute(placeOrderForm);

			return checkoutSummary(model);
		}
	}

	@RequestMapping(value = "/summary/reorder", method =
	{ RequestMethod.PUT, RequestMethod.POST })
	@RequireHardLogIn
	public String reorder(@RequestParam(value = "orderCode") final String orderCode, final RedirectAttributes redirectModel)
			throws CMSItemNotFoundException, InvalidCartException, ParseException, CommerceCartModificationException
	{
		// create a cart from the order and set it as session cart.
		getB2BCheckoutFacade().createCartFromOrder(orderCode);
		// validate for stock and availability
		final List<CartModificationData> cartModifications = doNotUsecartFacade.validateCartData();
		for (final CartModificationData cartModification : cartModifications)
		{
			if (CommerceCartModificationStatus.NO_STOCK.equals(cartModification.getStatusCode()))
			{
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
						"basket.page.message.update.reducedNumberOfItemsAdded.noStock", new Object[]
						{ cartModification.getEntry().getProduct().getName() });
				break;
			}
			else if (cartModification.getQuantity() != cartModification.getQuantityAdded())
			{
				// item has been modified to match available stock levels
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
						"basket.information.quantity.adjusted");
				break;
			}
			// TODO: handle more specific messaging, i.e. out of stock, product not available
		}
		return REDIRECT_PREFIX + "/checkout/single/summary";//checkoutSummary(model);
	}

	/**
	 * Need to move out of controller utility method for Replenishment
	 *
	 */
	protected List<String> getNumberRange(final int startNumber, final int endNumber)
	{
		final List<String> numbers = new ArrayList<String>();
		for (int number = startNumber; number <= endNumber; number++)
		{
			numbers.add(String.valueOf(number));
		}
		return numbers;
	}

	/**
	 * Data class used to hold a drop down select option value. Holds the code identifier as well as the display name.
	 */
	public static class SelectOption
	{
		private final String code;
		private final String name;

		public SelectOption(final String code, final String name)
		{
			this.code = code;
			this.name = name;
		}

		public String getCode()
		{
			return code;
		}

		public String getName()
		{
			return name;
		}
	}
}
