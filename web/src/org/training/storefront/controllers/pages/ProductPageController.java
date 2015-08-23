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

import de.hybris.platform.acceleratorservices.controllers.page.PageType;
import de.hybris.platform.b2bacceleratorfacades.futurestock.B2BFutureStockFacade;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.BaseOptionData;
import de.hybris.platform.commercefacades.product.data.FutureStockData;
import de.hybris.platform.commercefacades.product.data.ImageData;
import de.hybris.platform.commercefacades.product.data.ImageDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.ReviewData;
import de.hybris.platform.commerceservices.url.UrlResolver;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.util.Config;
import org.training.storefront.breadcrumb.impl.ProductBreadcrumbBuilder;
import org.training.storefront.constants.WebConstants;
import org.training.storefront.controllers.ControllerConstants;
import org.training.storefront.controllers.util.GlobalMessages;
import org.training.storefront.forms.FutureStockForm;
import org.training.storefront.forms.ReviewForm;
import org.training.storefront.util.XSSFilterUtil;
import org.training.storefront.variants.VariantSortStrategy;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.collect.Maps;


/**
 * Controller for product details page.
 */
@Controller
@Scope("tenant")
@RequestMapping(value = "/**/p")
public class ProductPageController extends AbstractPageController
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(ProductPageController.class);

	/**
	 * We use this suffix pattern because of an issue with Spring 3.1 where a Uri value is incorrectly extracted if it
	 * contains on or more '.' characters. Please see https://jira.springsource.org/browse/SPR-6164 for a discussion on
	 * the issue and future resolution.
	 */
	private static final String PRODUCT_CODE_PATH_VARIABLE_PATTERN = "/{productCode:.*}";
	private static final String REVIEWS_PATH_VARIABLE_PATTERN = "{numberOfReviews:.*}";

	private static final String FUTURE_STOCK_ENABLED = "storefront.products.futurestock.enabled";
	private static final String STOCK_SERVICE_UNAVAILABLE = "basket.page.viewFuture.unavailable";
	private static final String NOT_MULTISKU_ITEM_ERROR = "basket.page.viewFuture.not.multisku";

	@Resource(name = "productDataUrlResolver")
	private UrlResolver<ProductData> productDataUrlResolver;

	@Resource(name = "b2bProductFacade")
	private ProductFacade productFacade;

	@Resource(name = "productBreadcrumbBuilder")
	private ProductBreadcrumbBuilder productBreadcrumbBuilder;

	@Resource(name = "variantSortStrategy")
	private VariantSortStrategy variantSortStrategy;

	@Resource(name = "b2bFutureStockFacade")
	private B2BFutureStockFacade b2bFutureStockFacade;

	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	public String productDetail(@PathVariable("productCode") final String productCode, final Model model,
			final HttpServletRequest request, final HttpServletResponse response) throws CMSItemNotFoundException,
			UnsupportedEncodingException
	{
		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, null);

		final String redirection = checkRequestUrl(request, response, productDataUrlResolver.resolve(productData));
		if (StringUtils.isNotEmpty(redirection))
		{
			return redirection;
		}

		updatePageTitle(productData, model);
		final List<ProductOption> extraOptions = Arrays.asList(ProductOption.VARIANT_MATRIX_BASE, ProductOption.VARIANT_MATRIX_URL,
				ProductOption.VARIANT_MATRIX_MEDIA);
		populateProductDetailForDisplay(productCode, model, request, extraOptions);
		model.addAttribute(new ReviewForm());
		model.addAttribute("pageType", PageType.PRODUCT.name());
		model.addAttribute("futureStockEnabled", Boolean.valueOf(Config.getBoolean(FUTURE_STOCK_ENABLED, false)));

		return getViewForPage(model);
	}


	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/orderForm", method = RequestMethod.GET)
	public String productOrderForm(@PathVariable("productCode") final String productCode, final Model model,
			final HttpServletRequest request, final HttpServletResponse response) throws CMSItemNotFoundException
	{
		final List<ProductOption> extraOptions = Arrays.asList(ProductOption.VARIANT_MATRIX_BASE,
				ProductOption.VARIANT_MATRIX_PRICE, ProductOption.VARIANT_MATRIX_MEDIA, ProductOption.VARIANT_MATRIX_STOCK);

		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, extraOptions);
		updatePageTitle(productData, model);

		populateProductDetailForDisplay(productCode, model, request, extraOptions);

		if (!model.containsAttribute(WebConstants.MULTI_DIMENSIONAL_PRODUCT))
		{
			return REDIRECT_PREFIX + productDataUrlResolver.resolve(productData);
		}

		return ControllerConstants.Views.Pages.Product.OrderForm;
	}


	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/futureStock", method = RequestMethod.GET)
	public String productFutureStock(@PathVariable("productCode") final String productCode, final Model model,
			final HttpServletRequest request, final HttpServletResponse response) throws CMSItemNotFoundException
	{
		final boolean futureStockEnabled = Config.getBoolean(FUTURE_STOCK_ENABLED, false);
		if (futureStockEnabled)
		{
			final List<FutureStockData> futureStockList = b2bFutureStockFacade.getFutureAvailability(productCode);
			if (futureStockList == null)
			{
				GlobalMessages.addErrorMessage(model, STOCK_SERVICE_UNAVAILABLE);
			}
			else if (futureStockList.isEmpty())
			{
				GlobalMessages.addErrorMessage(model, "product.product.details.future.nostock");
			}
			populateProductDetailForDisplay(productCode, model, request, Collections.EMPTY_LIST);
			model.addAttribute("futureStocks", futureStockList);

			return ControllerConstants.Views.Fragments.Product.FutureStockPopup;
		}
		else
		{
			return ControllerConstants.Views.Pages.Error.ErrorNotFoundPage;
		}

	}

	@ResponseBody
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/grid/skusFutureStock", method =
	{ RequestMethod.POST }, produces = MediaType.APPLICATION_JSON_VALUE)
	public final Map<String, Object> productSkusFutureStock(final FutureStockForm form, final Model model)
	{
		final String productCode = form.getProductCode();
		final List<String> skus = form.getSkus();
		final boolean futureStockEnabled = Config.getBoolean(FUTURE_STOCK_ENABLED, false);

		Map<String, Object> result = new HashMap<>();
		if (futureStockEnabled && CollectionUtils.isNotEmpty(skus) && StringUtils.isNotBlank(productCode))
		{
			final Map<String, List<FutureStockData>> futureStockData = b2bFutureStockFacade
					.getFutureAvailabilityForSelectedVariants(productCode, skus);

			if (futureStockData == null)
			{
				// future availability service is down, we show this to the user
				result = Maps.newHashMap();
				final String errorMessage = getMessageSource().getMessage(NOT_MULTISKU_ITEM_ERROR, null,
						getI18nService().getCurrentLocale());
				result.put(NOT_MULTISKU_ITEM_ERROR, errorMessage);
			}
			else
			{
				for (final Entry<String, List<FutureStockData>> entry : futureStockData.entrySet())
				{
					result.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return result;
	}

	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/zoomImages", method = RequestMethod.GET)
	public String showZoomImages(@PathVariable("productCode") final String productCode,
			@RequestParam(value = "galleryPosition", required = false) final String galleryPosition, final Model model)
	{

		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode,
				Collections.singleton(ProductOption.GALLERY));
		final List<Map<String, ImageData>> images = getGalleryImages(productData);
		model.addAttribute("galleryImages", images);
		model.addAttribute("product", productData);
		if (galleryPosition != null)
		{
			try
			{
				model.addAttribute("zoomImageUrl", images.get(Integer.parseInt(galleryPosition)).get("zoom").getUrl());
			}
			catch (final IndexOutOfBoundsException ignore)
			{
				model.addAttribute("zoomImageUrl", "");
			}
			catch (final NumberFormatException ignore)
			{
				model.addAttribute("zoomImageUrl", "");
			}
		}
		return ControllerConstants.Views.Fragments.Product.ZoomImagesPopup;
	}

	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/quickView", method = RequestMethod.GET)
	public String showQuickView(@PathVariable("productCode") final String productCode, final Model model,
			final HttpServletRequest request)
	{

		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, Arrays.asList(ProductOption.BASIC,
				ProductOption.PRICE, ProductOption.SUMMARY, ProductOption.DESCRIPTION, ProductOption.CATEGORIES,
				ProductOption.PROMOTIONS, ProductOption.STOCK, ProductOption.REVIEW, ProductOption.VOLUME_PRICES));

		populateProductData(productData, model, request);

		return ControllerConstants.Views.Fragments.Product.QuickViewPopup;
	}

	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/review", method = RequestMethod.POST)
	public String postReview(@PathVariable final String productCode, @Valid final ReviewForm form, final BindingResult result,
			final Model model, final HttpServletRequest request, final RedirectAttributes redirectAttrs)
			throws CMSItemNotFoundException
	{
		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, null);

		if (result.hasErrors())
		{
			updatePageTitle(productData, model);
			GlobalMessages.addErrorMessage(model, "review.general.error");
			model.addAttribute("showReviewForm", Boolean.TRUE);
			populateProductDetailForDisplay(productCode, model, request, Collections.EMPTY_LIST);
			storeCmsPageInModel(model, getPageForProduct(productCode));
			return getViewForPage(model);
		}

		final ReviewData review = new ReviewData();
		review.setHeadline(XSSFilterUtil.filter(form.getHeadline()));
		review.setComment(XSSFilterUtil.filter(form.getComment()));
		review.setRating(form.getRating());
		review.setAlias(XSSFilterUtil.filter(form.getAlias()));
		productFacade.postReview(productCode, review);
		GlobalMessages.addFlashMessage(redirectAttrs, GlobalMessages.CONF_MESSAGES_HOLDER, "review.confirmation.thank.you.title");
		return "redirect:" + productDataUrlResolver.resolve(productData);
	}

	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/reviewhtml/" + REVIEWS_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	public String reviewHtml(@PathVariable("productCode") final String productCode,
			@PathVariable("numberOfReviews") final String numberOfReviews, final Model model, final HttpServletRequest request)
	{
		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, null);

		final List<ReviewData> reviews;

		if ("all".equals(numberOfReviews))
		{
			reviews = productFacade.getReviews(productCode);
		}
		else
		{
			reviews = productFacade.getReviews(productCode, Integer.valueOf(numberOfReviews));
		}

		model.addAttribute("reviews", reviews);
		model.addAttribute("reviewsTotal", productData.getNumberOfReviews());
		model.addAttribute(new ReviewForm());

		return ControllerConstants.Views.Fragments.Product.ReviewsTab;
	}

	protected void updatePageTitle(final ProductData productData, final Model model)
	{
		storeContentPageTitleInModel(model, getPageTitleResolver().resolveProductPageTitle(productData.getCode()));
	}

	@ExceptionHandler(UnknownIdentifierException.class)
	public String handleUnknownIdentifierException(final UnknownIdentifierException exception, final HttpServletRequest request)
	{
		request.setAttribute("message", exception.getMessage());
		return FORWARD_PREFIX + "/404";
	}

	//where the example call is
	protected void populateProductDetailForDisplay(final String productCode, final Model model, final HttpServletRequest request,
			final List<ProductOption> extraOptions) throws CMSItemNotFoundException
	{


		final List<ProductOption> options = new ArrayList<>(Arrays.asList(ProductOption.VARIANT_FIRST_VARIANT, ProductOption.BASIC,
				ProductOption.URL, ProductOption.PRICE, ProductOption.SUMMARY, ProductOption.DESCRIPTION, ProductOption.GALLERY,
				ProductOption.CATEGORIES, ProductOption.REVIEW, ProductOption.PROMOTIONS, ProductOption.CLASSIFICATION,
				ProductOption.VARIANT_FULL, ProductOption.STOCK, ProductOption.VOLUME_PRICES, ProductOption.PRICE_RANGE));

		options.addAll(extraOptions);
		final ProductData productData = productFacade.getProductForCodeAndOptions(productCode, options);

		sortVariantOptionData(productData);
		storeCmsPageInModel(model, getPageForProduct(productCode));
		populateProductData(productData, model, request);
		final ProductData baseProductData = getBaseProduct(productData, options);
		model.addAttribute(WebConstants.BREADCRUMBS_KEY, productBreadcrumbBuilder.getBreadcrumbs(baseProductData));
		if (CollectionUtils.isNotEmpty(productData.getVariantMatrix()))
		{
			model.addAttribute(WebConstants.MULTI_DIMENSIONAL_PRODUCT,
					Boolean.valueOf(CollectionUtils.isNotEmpty(productData.getVariantMatrix())));
		}
	}

	protected ProductData getBaseProduct(final ProductData product, final List<ProductOption> options)
	{
		final String baseProductCode = product.getBaseProduct();
		if (StringUtils.isNotBlank(baseProductCode))
		{
			return productFacade.getProductForCodeAndOptions(baseProductCode, options);
		}
		return product;
	}

	protected void populateProductData(final ProductData productData, final Model model, final HttpServletRequest request)
	{
		model.addAttribute("galleryImages", getGalleryImages(productData));
		model.addAttribute("product", productData);
	}

	protected void sortVariantOptionData(final ProductData productData)
	{
		if (CollectionUtils.isNotEmpty(productData.getBaseOptions()))
		{
			for (final BaseOptionData baseOptionData : productData.getBaseOptions())
			{
				if (CollectionUtils.isNotEmpty(baseOptionData.getOptions()))
				{
					Collections.sort(baseOptionData.getOptions(), variantSortStrategy);
				}
			}
		}

		if (CollectionUtils.isNotEmpty(productData.getVariantOptions()))
		{
			Collections.sort(productData.getVariantOptions(), variantSortStrategy);
		}
	}

	protected AbstractPageModel getPageForProduct(final String productCode) throws CMSItemNotFoundException
	{
		return getCmsPageService().getPageForProductCode(productCode);
	}

	protected List<Map<String, ImageData>> getGalleryImages(final ProductData productData)
	{
		final List<Map<String, ImageData>> galleryImages = new ArrayList<Map<String, ImageData>>();
		if (CollectionUtils.isNotEmpty(productData.getImages()))
		{
			final List<ImageData> images = new ArrayList<ImageData>();
			for (final ImageData image : productData.getImages())
			{
				if (ImageDataType.GALLERY.equals(image.getImageType()))
				{
					images.add(image);
				}
			}
			Collections.sort(images, new Comparator<ImageData>()
			{
				@Override
				public int compare(final ImageData image1, final ImageData image2)
				{
					return image1.getGalleryIndex().compareTo(image2.getGalleryIndex());
				}
			});

			if (CollectionUtils.isNotEmpty(images))
			{
				int currentIndex = images.get(0).getGalleryIndex().intValue();
				Map<String, ImageData> formats = new HashMap<String, ImageData>();
				for (final ImageData image : images)
				{
					if (currentIndex != image.getGalleryIndex().intValue())
					{
						galleryImages.add(formats);
						formats = new HashMap<String, ImageData>();
						currentIndex = image.getGalleryIndex().intValue();
					}
					formats.put(image.getFormat(), image);
				}
				if (!formats.isEmpty())
				{
					galleryImages.add(formats);
				}
			}
		}
		return galleryImages;
	}

}
