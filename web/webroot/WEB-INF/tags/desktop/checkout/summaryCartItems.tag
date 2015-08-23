<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="cartData" required="true" type="de.hybris.platform.commercefacades.order.data.CartData" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="format" tagdir="/WEB-INF/tags/shared/format" %>
<%@ taglib prefix="product" tagdir="/WEB-INF/tags/desktop/product" %>
<%@ taglib prefix="grid" tagdir="/WEB-INF/tags/desktop/grid" %>



<div class="headline"><spring:theme code="basket.page.title.yourItems"/></div>

<div class="orderList">
	<table class="orderListTable deliveryCartItems">
		<thead>
			<tr>
				<th id="header2" colspan="2"><spring:theme code="text.productDetails" text="Product Details"/></th>
				<th id="header3"><spring:theme code="text.itemPrice" text="Item Price"/></th>
				<th id="header4"><spring:theme code="text.quantity" text="Quantity"/></th>
				<th id="header6"><spring:theme code="text.total" text="Total"/></th>
				<th id="header7">&nbsp;</th>
			</tr>
		</thead>

		<tbody>
			<c:forEach items="${cartData.entries}" var="entry" varStatus="loop">
				<c:url value="${entry.product.url}" var="productUrl"/>
				<tr>
					<td class="thumb">
						<a href="${productUrl}"><product:productPrimaryImage product="${entry.product}" format="thumbnail"/></a>
					</td>
					<td class="desc">
						<div class="name"><a href="${productUrl}">${entry.product.name}</a></div>
										
										
						<c:forEach items="${entry.product.baseOptions}" var="option">
							<c:if test="${not empty option.selected and option.selected.url eq entry.product.url}">
								<c:forEach items="${option.selected.variantOptionQualifiers}" var="selectedOption">
									<div>${selectedOption.name}: ${selectedOption.value}</div>
								</c:forEach>
							</c:if>
						</c:forEach>
				
						<c:if test="${not empty entry.entries}">
							<c:forEach items="${entry.entries}" var="currentEntry" varStatus="stat">
                            	<c:set var="subEntries" value="${stat.first ? '' : subEntries}${currentEntry.product.code}:${currentEntry.quantity},"/>
                        	</c:forEach>
                        	<div style="display:none" id="grid${loop.index}" data-sub-entries="${subEntries}"> </div>
						</c:if>
						
						<c:if test="${ycommerce:doesPotentialPromotionExistForOrderEntry(cartData, entry.entryNumber)}">
							<ul class="cart-promotions">
								<c:forEach items="${cartData.potentialProductPromotions}" var="promotion">
									<c:set var="displayed" value="false"/>
									<c:forEach items="${promotion.consumedEntries}" var="consumedEntry">
										<c:if test="${not displayed && consumedEntry.orderEntryNumber == entry.entryNumber && not empty promotion.description}">
											<c:set var="displayed" value="true"/>
											<li class="cart-promotions-potential">
												${promotion.description}
											</li>
										</c:if>
									</c:forEach>
								</c:forEach>
							</ul>
						</c:if>
								
						<c:if test="${ycommerce:doesAppliedPromotionExistForOrderEntry(cartData, entry.entryNumber)}">
							<ul class="cart-promotions">
								<c:forEach items="${cartData.appliedProductPromotions}" var="promotion">
									<c:set var="displayed" value="false"/>
									<c:forEach items="${promotion.consumedEntries}" var="consumedEntry">
										<c:if test="${not displayed && consumedEntry.orderEntryNumber == entry.entryNumber}">
											<c:set var="displayed" value="true"/>
											<li class="cart-promotions-applied">
												${promotion.description}
											</li>
										</c:if>
									</c:forEach>
								</c:forEach>
							</ul>
						</c:if>
					</td>
				
					<td class="priceRow">
						<c:choose>
							<c:when test="${entry.product.multidimensional and (entry.product.priceRange.minPrice.value ne entry.product.priceRange.maxPrice.value)}" >
								<format:price priceData="${entry.product.priceRange.minPrice}" displayFreeForZero="true"/>
								-
								<format:price priceData="${entry.product.priceRange.maxPrice}" displayFreeForZero="true"/>
							</c:when>
							<c:otherwise>
								<format:price priceData="${entry.basePrice}" displayFreeForZero="true"/>
							</c:otherwise>
						</c:choose>
					</td>
									
					<td class="priceRow"><spring:theme code="basket.page.quantity"/>: ${entry.quantity}</td>
					<td class="priceRow"><format:price priceData="${entry.totalPrice}" displayFreeForZero="true"/></td>
					<td headers="header7" class="multidimensional">
						<c:choose>
							<c:when test="${empty entry.entries}" >
							</c:when>
							<c:otherwise>  
								<a href="#" id="QuantityProductToggle" data-index="${loop.index}" class="showQuantityProduct updateQuantityProduct-toggle">+</a>
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
				
				<tr><td colspan="7"><div id="ajaxGrid${loop.index}" style="display: none"></div></td></tr> 
				
				<c:if test="${not empty entry.entries}" >
                    <tr><th colspan="7">
                        <c:forEach items="${entry.entries}" var="currentEntry" varStatus="stat">
                            <c:set var="subEntries" value="${stat.first ? '' : subEntries}${currentEntry.product.code}:${currentEntry.quantity},"/>
                        </c:forEach>

                        <div style="display:none" id="grid${loop.index}" data-sub-entries="${subEntries}"> </div>
                    </th></tr>
                </c:if>
			</c:forEach>
		</tbody>
	</table>
</div>

<div id="ajaxCartItems"></div>
