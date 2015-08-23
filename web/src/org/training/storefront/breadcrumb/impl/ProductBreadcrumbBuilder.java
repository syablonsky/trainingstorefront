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
package org.training.storefront.breadcrumb.impl;

import de.hybris.platform.catalog.model.classification.ClassificationClassModel;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commerceservices.url.UrlResolver;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.product.ProductService;
import org.training.storefront.breadcrumb.Breadcrumb;
import org.training.storefront.history.BrowseHistory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;


/**
 * ProductBreadcrumbBuilder implementation for {@link ProductData}
 */
public class ProductBreadcrumbBuilder
{
	private static final String LAST_LINK_CLASS = "active";

	private UrlResolver<ProductData> productDataUrlResolver;
	private UrlResolver<CategoryModel> categoryModelUrlResolver;
	private BrowseHistory browseHistory;
	// FIXME - remove product service
	private ProductService productService;

	public List<Breadcrumb> getBreadcrumbs(final ProductData baseProductData) throws IllegalArgumentException
	{
		final List<Breadcrumb> breadcrumbs = new ArrayList<Breadcrumb>();


		final Breadcrumb last = getProductBreadcrumb(baseProductData);
		last.setLinkClass(LAST_LINK_CLASS);

		breadcrumbs.add(last);

		addCategoryBreadCrumbs(breadcrumbs, baseProductData.getCode());


		Collections.reverse(breadcrumbs);
		return breadcrumbs;
	}

	protected void addCategoryBreadCrumbs(final List<Breadcrumb> breadcrumbs, final String baseProductCode)
	{
		final Collection<CategoryModel> categoryModels = new ArrayList<CategoryModel>();

		final ProductModel baseProductModel = productService.getProductForCode(baseProductCode);

		categoryModels.addAll(baseProductModel.getSupercategories());
		while (!categoryModels.isEmpty())
		{
			CategoryModel toDisplay = null;
			for (final CategoryModel categoryModel : categoryModels)
			{
				if (!(categoryModel instanceof ClassificationClassModel))
				{
					if (toDisplay == null)
					{
						toDisplay = categoryModel;
					}
					if (getBrowseHistory().findUrlInHistory(categoryModel.getCode()) != null)
					{
						break;
					}
				}
			}
			categoryModels.clear();
			if (toDisplay != null)
			{
				breadcrumbs.add(getCategoryBreadcrumb(toDisplay));
				categoryModels.addAll(toDisplay.getSupercategories());
			}
		}
	}

	protected Breadcrumb getProductBreadcrumb(final ProductData product)
	{
		final String productUrl = getProductDataUrlResolver().resolve(product);
		return new Breadcrumb(productUrl, product.getName(), null);
	}

	protected Breadcrumb getCategoryBreadcrumb(final CategoryModel category)
	{
		final String categoryUrl = getCategoryModelUrlResolver().resolve(category);
		return new Breadcrumb(categoryUrl, category.getName(), null);
	}

	protected UrlResolver<ProductData> getProductDataUrlResolver()
	{
		return productDataUrlResolver;
	}

	@Required
	public void setProductDataUrlResolver(final UrlResolver<ProductData> productDataUrlResolver)
	{
		this.productDataUrlResolver = productDataUrlResolver;
	}

	protected UrlResolver<CategoryModel> getCategoryModelUrlResolver()
	{
		return categoryModelUrlResolver;
	}

	@Required
	public void setCategoryModelUrlResolver(final UrlResolver<CategoryModel> categoryModelUrlResolver)
	{
		this.categoryModelUrlResolver = categoryModelUrlResolver;
	}

	protected BrowseHistory getBrowseHistory()
	{
		return browseHistory;
	}

	@Required
	public void setBrowseHistory(final BrowseHistory browseHistory)
	{
		this.browseHistory = browseHistory;
	}

	public ProductService getProductService()
	{
		return productService;
	}

	@Required
	public void setProductService(final ProductService productService)
	{
		this.productService = productService;
	}

}
