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

import de.hybris.platform.acceleratorcms.model.components.SearchBoxComponentModel;
import de.hybris.platform.acceleratorservices.controllers.page.PageType;
import de.hybris.platform.acceleratorservices.customer.CustomerLocationService;
import de.hybris.platform.b2bacceleratorfacades.api.search.SearchFacade;
import de.hybris.platform.b2bacceleratorfacades.search.data.ProductSearchStateData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.servicelayer.services.CMSComponentService;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.data.AutocompleteResultData;
import de.hybris.platform.commercefacades.search.data.AutocompleteSuggestionData;
import de.hybris.platform.commercefacades.search.data.SearchQueryData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.PaginationData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.util.Config;
import org.training.storefront.breadcrumb.impl.SearchBreadcrumbBuilder;
import org.training.storefront.constants.WebConstants;
import org.training.storefront.controllers.ControllerConstants;
import org.training.storefront.forms.AdvancedSearchForm;
import org.training.storefront.util.MetaSanitizerUtil;
import org.training.storefront.util.XSSFilterUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;


/**
 * Controller for search page.
 */
@Controller
@Scope("tenant")
@RequestMapping("/search")
public class SearchPageController extends AbstractSearchPageController
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(SearchPageController.class);

	private static final String ADVANCED_SEARCH_PRODUCT_IDS_DELIMITER = "storefront.advancedsearch.delimiter";
	private static final String ADVANCED_SEARCH_PRODUCT_IDS_DELIMITER_DEFAULT = ",";

	private static final String COMPONENT_UID_PATH_VARIABLE_PATTERN = "{componentUid:.*}";

	private static final String ADVANCED_SEARCH_RESULT_TYPE_CATALOG = "catalog";
	private static final String ADVANCED_SEARCH_RESULT_TYPE_ORDER_FORM = "order-form";

	private static final String SEARCH_CMS_PAGE_ID = "search";
	private static final String NO_RESULTS_CMS_PAGE_ID = "searchEmpty";

	private static final String NO_RESULTS_ADVANCED_PAGE_ID = "searchAdvancedEmpty";

	private static final String FUTURE_STOCK_ENABLED = "storefront.products.futurestock.enabled";

	private static final String INFINITE_SCROLL = "infiniteScroll";
	public static final int PAGE_SIZE = 10;

	@Resource(name = "b2bSolrProductSearchFacade")
	private SearchFacade<ProductData, SearchStateData> b2bSolrProductSearchFacade;

	@Resource(name = "b2bProductFlexibleSearchFacade")
	private SearchFacade<ProductData, SearchStateData> flexibleSearchProductSearchFacade;

	@Resource(name = "searchBreadcrumbBuilder")
	private SearchBreadcrumbBuilder searchBreadcrumbBuilder;

	@Resource(name = "customerLocationService")
	private CustomerLocationService customerLocationService;

	@Resource(name = "cmsComponentService")
	private CMSComponentService cmsComponentService;

	@RequestMapping(method = RequestMethod.GET, params = "!q")
	public String textSearch(@RequestParam(value = "text", defaultValue = StringUtils.EMPTY) final String searchText,
			final HttpServletRequest request, final Model model) throws CMSItemNotFoundException
	{
		if (StringUtils.isNotBlank(searchText))
		{
			final PageableData pageableData = createPageableData(0, getSearchPageSize(), null, ShowMode.Page);
			final SearchStateData searchState = createSearchStateData(searchText, false);

			final ProductSearchPageData<SearchStateData, ProductData> searchPageData = performSearch(searchState, pageableData,
					false);

			if (searchPageData == null)
			{
				storeCmsPageInModel(model, getContentPageForLabelOrId(NO_RESULTS_CMS_PAGE_ID));
			}
			else if (searchPageData.getKeywordRedirectUrl() != null)
			{
				// if the search engine returns a redirect, just
				return "redirect:" + searchPageData.getKeywordRedirectUrl();
			}
			else
			{
				updateCMSInfo(request, model, searchPageData);
			}
			getRequestContextData(request).setSearch(searchPageData);
			if (searchPageData != null)
			{
				model.addAttribute(
						WebConstants.BREADCRUMBS_KEY,
						searchBreadcrumbBuilder.getBreadcrumbs(null, searchText,
								CollectionUtils.isEmpty(searchPageData.getBreadcrumbs())));
			}
		}
		else
		{
			storeCmsPageInModel(model, getContentPageForLabelOrId(NO_RESULTS_CMS_PAGE_ID));
		}

		addMetaData(model, "search.meta.description.results", searchText, "search.meta.description.on", PageType.PRODUCTSEARCH,
				"no-index,follow");

		return getViewForPage(model);
	}

	@RequestMapping(method = RequestMethod.GET, params = "q")
	public String refineSearch(@RequestParam("q") final String searchQuery,
			@RequestParam(value = "page", defaultValue = "0") final int page,
			@RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
			@RequestParam(value = "sort", required = false) final String sortCode,
			@RequestParam(value = "text", required = false) final String searchText, final HttpServletRequest request,
			final Model model) throws CMSItemNotFoundException
	{


		final PageableData pageableData = createPageableData(page, getSearchPageSize(), sortCode, showMode);
		final SearchStateData searchState = createSearchStateData(searchQuery, false);

		final ProductSearchPageData<SearchStateData, ProductData> searchPageData = performSearch(searchState, pageableData, false);

		populateModel(model, searchPageData, showMode);
		model.addAttribute("userLocation", customerLocationService.getUserLocation());

		updateCMSInfo(request, model, searchPageData);
		model.addAttribute(WebConstants.BREADCRUMBS_KEY, searchBreadcrumbBuilder.getBreadcrumbs(null, searchPageData));

		addMetaData(model, "search.meta.description.results", searchText, "search.meta.description.on", PageType.PRODUCTSEARCH,
				"no-index,follow");

		return getViewForPage(model);
	}

	protected void updateCMSInfo(final HttpServletRequest request, final Model model,
			final ProductSearchPageData<SearchStateData, ProductData> searchPageData) throws CMSItemNotFoundException
	{

		if (searchPageData.getPagination().getTotalNumberOfResults() == 0)
		{
			model.addAttribute("searchPageData", searchPageData);
			updatePageTitle(searchPageData.getFreeTextSearch(), model);
			storeCmsPageInModel(model, getContentPageForLabelOrId(NO_RESULTS_CMS_PAGE_ID));
		}
		else
		{
			storeContinueUrl(request);
			populateModel(model, searchPageData, ShowMode.Page);
			storeCmsPageInModel(model, getContentPageForLabelOrId(SEARCH_CMS_PAGE_ID));
			updatePageTitle(searchPageData.getFreeTextSearch(), model);
		}
	}

	protected void updatePageTitle(final String searchText, final Model model)
	{
		storeContentPageTitleInModel(
				model,
				getPageTitleResolver().resolveContentPageTitle(
						getMessageSource().getMessage("search.meta.title", null, getCurrentLocale()) + " " + searchText));
	}


	protected ProductSearchPageData<SearchStateData, ProductData> performSearch(final SearchStateData searchState,
			final PageableData pageableData, final boolean useFlexibleSearch)
	{

		ProductSearchPageData<SearchStateData, ProductData> searchResult = createEmptySearchPageData();

		if (StringUtils.isNotBlank(searchState.getQuery().getValue()))
		{
			if (useFlexibleSearch)
			{
				searchResult = (ProductSearchPageData<SearchStateData, ProductData>) flexibleSearchProductSearchFacade.search(
						searchState, pageableData);
			}
			else
			{
				// search using solr.
				searchResult = (ProductSearchPageData<SearchStateData, ProductData>) b2bSolrProductSearchFacade.search(searchState,
						pageableData);
			}

		}

		return searchResult;
	}

	@RequestMapping(value = "/results", method = RequestMethod.GET)
	public String productListerSearchResults(@RequestParam("q") final String searchQuery,
			@RequestParam(value = "page", defaultValue = "0") final int page,
			@RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
			@RequestParam(value = "sort", required = false) final String sortCode,
			@RequestParam(value = "searchResultType", required = false) final String searchResultType,
			@RequestParam(value = "skuIndex", required = false, defaultValue = "0") final int skuIndex,
			@RequestParam(value = "isOrderForm", required = false, defaultValue = "false") final boolean isOrderForm,
			@RequestParam(value = "isOnlyProductIds", required = false, defaultValue = "false") final boolean isOnlyProductIds,
			@RequestParam(value = "isCreateOrderForm", required = false, defaultValue = "false") final boolean isCreateOrderForm,
			final Model model) throws CMSItemNotFoundException
	{


		// check if it is order form (either order form was selected or "Create Order Form"
		final PageableData pageableData = createPageableData(page, getSearchPageSize(), sortCode, showMode);
		final SearchStateData searchState = createSearchStateData(searchQuery,
				isPopulateVariants(searchResultType, isCreateOrderForm));

		final SearchPageData<ProductData> searchPageData = performSearch(searchState, pageableData,
				isUseFlexibleSearch(isOnlyProductIds, isCreateOrderForm));
		populateModel(model, searchPageData, showMode);


		final SearchResultsData<ProductData> searchResultsData = new SearchResultsData<ProductData>();

		searchResultsData.setResults(searchPageData.getResults());
		searchResultsData.setPagination(searchPageData.getPagination());

		model.addAttribute("searchResultsData", searchResultsData);
		model.addAttribute("skuIndex", Integer.valueOf(skuIndex));
		model.addAttribute("isOrderForm", Boolean.valueOf(isOrderForm));
		model.addAttribute("isCreateOrderForm", Boolean.valueOf(isCreateOrderForm));


		if (isCreateOrderForm)
		{
			model.addAttribute("searchResultType", ADVANCED_SEARCH_RESULT_TYPE_ORDER_FORM);
			model.addAttribute("filterSkus", splitSkusAsList(searchQuery));
		}
		else
		{
			model.addAttribute("searchResultType", searchResultType);
		}

		return ControllerConstants.Views.Fragments.Product.ProductLister;
	}

	private boolean isPopulateVariants(final String searchResultType, final boolean isCreateOrderForm)
	{
		return (searchResultType != null && StringUtils.equals(searchResultType, ADVANCED_SEARCH_RESULT_TYPE_ORDER_FORM))
				|| isCreateOrderForm;
	}

	@RequestMapping(value = "/advanced", method = RequestMethod.GET)
	public String advanceSearchResults(
			@RequestParam(value = "keywords", required = false, defaultValue = StringUtils.EMPTY) String keywords,
			@RequestParam(value = "searchResultType", required = false, defaultValue = ADVANCED_SEARCH_RESULT_TYPE_CATALOG) final String searchResultType,
			@RequestParam(value = "inStockOnly", required = false, defaultValue = "false") final boolean inStockOnly,
			@RequestParam(value = "onlyProductIds", required = false, defaultValue = "false") final boolean onlyProductIds,
			@RequestParam(value = "isCreateOrderForm", required = false, defaultValue = "false") final boolean isCreateOrderForm,
			@RequestParam(value = "q", defaultValue = StringUtils.EMPTY) String searchQuery,
			@RequestParam(value = "page", defaultValue = "0") final int page,
			@RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
			@RequestParam(value = "sort", required = false) final String sortCode, final Model model)
			throws CMSItemNotFoundException
	{

		if (StringUtils.isNotBlank(keywords))
		{
			searchQuery = keywords;
		}
		else
		{
			if (StringUtils.isNotBlank(searchQuery))
			{
				keywords = StringUtils.split(searchQuery, ":")[0];
			}
		}

		// check if it is order form (either order form was selected or "Create Order Form"
		final PageableData pageableData = createPageableData(page, getSearchPageSize(), sortCode, showMode);
		final SearchStateData searchState = createSearchStateData(searchQuery,
				isPopulateVariants(searchResultType, isCreateOrderForm));

		final SearchPageData<ProductData> searchPageData = performSearch(searchState, pageableData,
				isUseFlexibleSearch(onlyProductIds, isCreateOrderForm));
		populateModel(model, searchPageData, showMode);

		String metaInfoText = null;
		if (StringUtils.isEmpty(keywords))
		{
			metaInfoText = MetaSanitizerUtil.sanitizeDescription(getMessageSource().getMessage(
					"search.advanced.meta.description.title", null, getCurrentLocale()));
		}
		else
		{
			metaInfoText = MetaSanitizerUtil.sanitizeDescription(keywords);
		}

		model.addAttribute(WebConstants.BREADCRUMBS_KEY, searchBreadcrumbBuilder.getBreadcrumbs(null, metaInfoText, false));

		final AdvancedSearchForm form = new AdvancedSearchForm();
		form.setOnlyProductIds(Boolean.valueOf(onlyProductIds));
		form.setInStockOnly(Boolean.valueOf(inStockOnly));
		form.setKeywords(keywords);
		form.setCreateOrderForm(isCreateOrderForm);


		if (isCreateOrderForm)
		{
			form.setSearchResultType(ADVANCED_SEARCH_RESULT_TYPE_ORDER_FORM);
			final List<String> filterSkus = splitSkusAsList(keywords);
			form.setFilterSkus(filterSkus);
			form.setCreateOrderForm(Boolean.valueOf(false));
			form.setOnlyProductIds(Boolean.valueOf(true));
		}
		else
		{
			form.setSearchResultType(searchResultType);
		}

		model.addAttribute("advancedSearchForm", form);
		model.addAttribute("futureStockEnabled", Boolean.valueOf(Config.getBoolean(FUTURE_STOCK_ENABLED, false)));

		storeCmsPageInModel(model, getContentPageForLabelOrId(NO_RESULTS_ADVANCED_PAGE_ID));

		addMetaData(model, "search.meta.description.results", metaInfoText, "search.meta.description.on", PageType.PRODUCTSEARCH,
				"no-index,follow");

		return getViewForPage(model);
	}

	@ResponseBody
	@RequestMapping(value = "/autocomplete", method = {RequestMethod.GET, RequestMethod.POST})
	public List<String> getAutocompleteSuggestions(@RequestParam("term") final String term)
	{
		final SearchStateData searchState = createSearchStateData(term, true);

		final List<String> terms = new ArrayList<String>();
		for (final AutocompleteSuggestionData termData : b2bSolrProductSearchFacade.autocomplete(searchState))
		{
			terms.add(termData.getTerm());
		}
		return terms;
	}


	@ResponseBody
	@RequestMapping(value = "/autocomplete/" + COMPONENT_UID_PATH_VARIABLE_PATTERN, method = {RequestMethod.GET, RequestMethod.POST})
	public AutocompleteResultData getAutocompleteSuggestions(@PathVariable final String componentUid,
			@RequestParam("term") final String term) throws CMSItemNotFoundException
	{
		final SearchStateData searchState = createSearchStateData(term, true);

		final AutocompleteResultData resultData = new AutocompleteResultData();

		final SearchBoxComponentModel component = (SearchBoxComponentModel) cmsComponentService.getSimpleCMSComponent(componentUid);

		if (component.isDisplaySuggestions())
		{
			resultData.setSuggestions(subList(b2bSolrProductSearchFacade.autocomplete(searchState), component.getMaxSuggestions()));
		}

		if (component.isDisplayProducts())
		{
			final SearchPageData<ProductData> pageData = b2bSolrProductSearchFacade.search(searchState, null);
			resultData.setProducts(subList(pageData.getResults(), component.getMaxProducts()));
		}

		return resultData;
	}

	@ResponseBody
	@RequestMapping(value = "/autocompleteSecure", method = {RequestMethod.GET, RequestMethod.POST})
	public List<String> getAutocompleteSuggestionsSecure(@RequestParam("term") final String term)
	{
		return getAutocompleteSuggestions(term);
	}

	protected void addMetaData(final Model model, final String metaPrefixKey, final String searchText,
			final String metaPostfixKey, final PageType pageType, final String robotsBehaviour)
	{
		final String metaDescription = MetaSanitizerUtil.sanitizeDescription(getMessageSource().getMessage(metaPrefixKey, null,
				getCurrentLocale())
				+ " "
				+ searchText
				+ " "
				+ getMessageSource().getMessage(metaPostfixKey, null, getCurrentLocale())
				+ " "
				+ getSiteName());
		final String metaKeywords = MetaSanitizerUtil.sanitizeKeywords(searchText);
		setUpMetaData(model, metaKeywords, metaDescription);

		model.addAttribute("pageType", pageType.name());
		model.addAttribute("metaRobots", robotsBehaviour);
	}

	private boolean isUseFlexibleSearch(final boolean onlyProductIds, final boolean isCreateOrderForm)
	{
		return onlyProductIds || isCreateOrderForm;
	}

	private ProductSearchPageData<SearchStateData, ProductData> createEmptySearchPageData()
	{
		final ProductSearchPageData productSearchPageData = new ProductSearchPageData();

		productSearchPageData.setResults(Lists.newArrayList());
		final PaginationData pagination = new PaginationData();
		pagination.setTotalNumberOfResults(0);
		productSearchPageData.setPagination(pagination);
		productSearchPageData.setSorts(Lists.newArrayList());

		return productSearchPageData;
	}

	protected List<String> splitSkusAsList(final String skus)
	{
		return Arrays.asList(StringUtils.split(skus,
				Config.getString(ADVANCED_SEARCH_PRODUCT_IDS_DELIMITER, ADVANCED_SEARCH_PRODUCT_IDS_DELIMITER_DEFAULT)));
	}

	private Locale getCurrentLocale()
	{
		return getI18nService().getCurrentLocale();
	}

	private ProductSearchStateData createSearchStateData(final String term, final boolean populateVariants)
	{

		final ProductSearchStateData searchState = new ProductSearchStateData();
		final SearchQueryData searchQueryData = new SearchQueryData();
		searchQueryData.setValue(XSSFilterUtil.filter(term));
		searchState.setQuery(searchQueryData);
		searchState.setPopulateVariants(populateVariants);

		return searchState;
	}

	protected <E> List<E> subList(final List<E> list, final int maxElements)
	{
		if (CollectionUtils.isEmpty(list))
		{
			return Collections.emptyList();
		}

		if (list.size() > maxElements)
		{
			return list.subList(0, maxElements);
		}

		return list;
	}

	@Override
	protected void populateModel(final Model model, final SearchPageData<?> searchPageData, final ShowMode showMode)
	{
		super.populateModel(model, searchPageData, showMode);

		if (StringUtils.equalsIgnoreCase(getSiteConfigService().getString(PAGINATION_TYPE, PAGINATION), INFINITE_SCROLL))
		{
			model.addAttribute(IS_SHOW_ALLOWED, false);
		}
	}
}
