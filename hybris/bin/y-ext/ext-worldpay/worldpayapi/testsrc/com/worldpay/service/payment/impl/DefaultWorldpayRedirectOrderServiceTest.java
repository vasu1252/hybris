package com.worldpay.service.payment.impl;

import com.worldpay.core.services.WorldpayPaymentInfoService;
import com.worldpay.data.AdditionalAuthInfo;
import com.worldpay.enums.order.AuthorisedStatus;
import com.worldpay.exception.WorldpayException;
import com.worldpay.hostedorderpage.data.RedirectAuthoriseResult;
import com.worldpay.hostedorderpage.service.WorldpayURIService;
import com.worldpay.service.WorldpayServiceGateway;
import com.worldpay.service.WorldpayUrlService;
import com.worldpay.service.model.*;
import com.worldpay.service.model.payment.PaymentType;
import com.worldpay.service.model.token.TokenRequest;
import com.worldpay.service.payment.WorldpayOrderService;
import com.worldpay.service.payment.WorldpayTokenEventReferenceCreationStrategy;
import com.worldpay.service.request.RedirectAuthoriseServiceRequest;
import com.worldpay.service.response.RedirectAuthoriseServiceResponse;
import com.worldpay.strategy.WorldpayAuthenticatedShopperIdStrategy;
import com.worldpay.strategy.WorldpayDeliveryAddressStrategy;
import com.worldpay.transaction.WorldpayPaymentTransactionService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorservices.payment.data.PaymentData;
import de.hybris.platform.commerceservices.customer.CustomerEmailResolutionService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.commerceservices.strategies.GenerateMerchantTransactionCodeStrategy;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.AddressService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.worldpay.enums.order.AuthorisedStatus.AUTHORISED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.junit.Assert.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.*;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultWorldpayRedirectOrderServiceTest {

    private static final String REDIRECT_URL = "http://www.example.com";
    private static final double TOTAL_PRICE = 100D;
    private static final double PAYMENT_AMOUNT = 100d;
    private static final String GBP = "GBP";
    private static final String LANGUAGE_ISO_CODE = "en";
    private static final String WORLDPAY_ORDER_CODE = "worldpayOrderCode";
    private static final String CUSTOMER_EMAIL = "customerEmail";
    private static final String MERCHANT_CODE = "merchantCode";
    private static final String COUNTRY_CODE = "countryCode";
    private static final String FULL_SUCCESS_URL = "fullSuccessUrl";
    private static final String FULL_PENDING_URL = "fullPendingUrl";
    private static final String FULL_FAILURE_URL = "fullFailureUrl";
    private static final String FULL_CANCEL_URL = "fullCancelUrl";
    private static final String FULL_ERROR_URL = "fullErrorUrl";
    private static final String AUTHENTICATED_SHOPPER_ID = "authenticatedShopperId";
    private static final String TOKEN_EVENT_REFERENCE = "tokenEventReference";


    private static final String KEY_MAC = "mac";
    private static final String KEY_MAC2 = "mac2";
    private static final String ORDER_KEY = "orderKey";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_ERROR_URL = "errorURL";
    private static final String KEY_CANCEL_URL = "cancelURL";
    private static final String KEY_SUCCESS_URL = "successURL";
    private static final String KEY_PENDING_URL = "pendingURL";
    private static final String KEY_FAILURE_URL = "failureURL";
    private static final String PAYMENT_STATUS = "paymentStatus";
    private static final String KEY_PAYMENT_AMOUNT = "paymentAmount";
    private static final String KEY_PAYMENT_CURRENCY = "paymentCurrency";
    private static final String WORLDPAY_MERCHANT_CODE = "worldpayMerchantCode";

    @Spy
    @InjectMocks
    private DefaultWorldpayRedirectOrderService testObj;

    @Mock
    private MerchantInfo merchantInfoMock;
    @Mock
    private CartModel cartModelMock;
    @Mock
    private AdditionalAuthInfo additionalAuthInfoMock;
    @Mock
    private CustomerEmailResolutionService customerEmailResolutionServiceMock;
    @Mock
    private CustomerModel customerModelMock;
    @Mock
    private Converter<AddressModel, Address> worldpayAddressConverterMock;
    @Mock
    private BasicOrderInfo basicOrderInfoMock;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private RedirectAuthoriseServiceRequest redirectAuthoriseServiceRequestMock;
    @Mock
    private RedirectAuthoriseServiceResponse redirectAuthoriseServiceResponseMock;
    @Mock
    private WorldpayServiceGateway worldpayServiceGatewayMock;
    @Mock
    private RedirectReference redirectReferenceMock;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private CommonI18NService commonI18NServiceMock;
    @Mock
    private WorldpayUrlService worldpayUrlServiceMock;
    @Mock
    private RedirectAuthoriseResult redirectAuthoriseResultMock;
    @Mock
    private ModelService modelServiceMock;
    @Mock
    private PaymentInfoModel paymentInfoModelMock;
    @Mock
    private Address billingAddressMock, potentialPickupAddressMock, cartDeliveryAddressMock;
    @Mock
    private AddressModel potentialPickupAddressModelMock, cartDeliveryAddressModelMock, cartPaymentAddressModelMock, clonedAddressMock;
    @Mock
    private CommerceCheckoutService commerceCheckoutServiceMock;
    @Mock
    private PaymentTransactionModel paymentTransactionModelMock;
    @Mock
    private WorldpayPaymentTransactionService worldpayPaymentTransactionServiceMock;
    @Mock
    private SessionService sessionServiceMock;
    @Mock
    private WorldpayURIService worldpayURIServiceMock;
    @Mock
    private WorldpayOrderService worldpayOrderServiceMock;
    @Mock
    private CommerceCheckoutParameter commerceCheckoutParameterMock;
    @Mock
    private WorldpayPaymentInfoService worldpayPaymentInfoServiceMock;
    @Mock
    private Shopper shopperMock;
    @Mock
    private TokenRequest tokenRequestMock;
    @Mock
    private Shopper authenticatedShopperMock;
    @Mock
    private WorldpayAuthenticatedShopperIdStrategy worldpayAuthenticatedShopperIdStrategyMock;
    @Mock
    private WorldpayTokenEventReferenceCreationStrategy worldpayTokenEventReferenceCreationStrategyMock;
    @Mock
    private GenerateMerchantTransactionCodeStrategy worldpayGenerateMerchantTransactionCodeStrategyMock;
    @Mock
    private WorldpayDeliveryAddressStrategy worldpayDeliveryAddressStrategyMock;
    @Mock
    private List<PaymentType> paymentTypeListMock;
    @Mock
    private BigDecimal bigDecimalMock;
    @Mock
    private AddressService addressServiceMock;
    @Mock
    private CurrencyModel currencyModelMock;

    @Before
    public void setUp() throws WorldpayException {
        when(cartModelMock.getTotalPrice()).thenReturn(TOTAL_PRICE);
        when(cartModelMock.getCurrency()).thenReturn(currencyModelMock);
        when(currencyModelMock.getIsocode()).thenReturn(GBP);
        when(cartModelMock.getUser()).thenReturn(customerModelMock);
        when(cartModelMock.getPaymentAddress()).thenReturn(cartPaymentAddressModelMock);
        when(customerEmailResolutionServiceMock.getEmailForCustomer(customerModelMock)).thenReturn(CUSTOMER_EMAIL);
        when(worldpayAddressConverterMock.convert(cartModelMock.getPaymentAddress())).thenReturn(billingAddressMock);
        when(redirectAuthoriseServiceResponseMock.getRedirectReference()).thenReturn(redirectReferenceMock);
        when(commonI18NServiceMock.getCurrentLanguage().getIsocode()).thenReturn(LANGUAGE_ISO_CODE);
        when(modelServiceMock.create(PaymentInfoModel.class)).thenReturn(paymentInfoModelMock);
        when(worldpayOrderServiceMock.createBasicOrderInfo(eq(WORLDPAY_ORDER_CODE), eq(WORLDPAY_ORDER_CODE), any(Amount.class))).thenReturn(basicOrderInfoMock);
        when(worldpayOrderServiceMock.createShopper(CUSTOMER_EMAIL, null, null)).thenReturn(shopperMock);
        when(worldpayServiceGatewayMock.redirectAuthorise(redirectAuthoriseServiceRequestMock)).thenReturn(redirectAuthoriseServiceResponseMock);
        when(merchantInfoMock.getMerchantCode()).thenReturn(MERCHANT_CODE);
        when(redirectReferenceMock.getValue()).thenReturn(REDIRECT_URL);
        when(worldpayUrlServiceMock.getFullSuccessURL()).thenReturn(FULL_SUCCESS_URL);
        when(worldpayUrlServiceMock.getFullPendingURL()).thenReturn(FULL_PENDING_URL);
        when(worldpayUrlServiceMock.getFullFailureURL()).thenReturn(FULL_FAILURE_URL);
        when(worldpayUrlServiceMock.getFullCancelURL()).thenReturn(FULL_CANCEL_URL);
        when(worldpayUrlServiceMock.getFullErrorURL()).thenReturn(FULL_ERROR_URL);
        when(worldpayPaymentInfoServiceMock.createPaymentInfo(cartModelMock)).thenReturn(paymentInfoModelMock);
        when(worldpayAuthenticatedShopperIdStrategyMock.getAuthenticatedShopperId(customerModelMock)).thenReturn(AUTHENTICATED_SHOPPER_ID);
        when(worldpayTokenEventReferenceCreationStrategyMock.createTokenEventReference()).thenReturn(TOKEN_EVENT_REFERENCE);
        when(worldpayGenerateMerchantTransactionCodeStrategyMock.generateCode(cartModelMock)).thenReturn(WORLDPAY_ORDER_CODE);
        when(worldpayDeliveryAddressStrategyMock.getDeliveryAddress(cartModelMock)).thenReturn(potentialPickupAddressModelMock);
        when(worldpayAddressConverterMock.convert(cartDeliveryAddressModelMock)).thenReturn(cartDeliveryAddressMock);
        when(worldpayAddressConverterMock.convert(potentialPickupAddressModelMock)).thenReturn(potentialPickupAddressMock);
        when(redirectAuthoriseServiceRequestMock.getOrder().getBillingAddress().getCountryCode()).thenReturn(COUNTRY_CODE);
        when(addressServiceMock.cloneAddressForOwner(cartPaymentAddressModelMock, paymentInfoModelMock)).thenReturn(clonedAddressMock);

        doReturn(paymentTypeListMock).when(testObj).getIncludedPaymentTypeList(additionalAuthInfoMock);
        doReturn(commerceCheckoutParameterMock).when(testObj).createCommerceCheckoutParameter(cartModelMock, paymentInfoModelMock, bigDecimalMock);
        doReturn(redirectAuthoriseServiceRequestMock).when(testObj).createRedirectAuthoriseRequest(merchantInfoMock, additionalAuthInfoMock, shopperMock, basicOrderInfoMock, paymentTypeListMock, null, potentialPickupAddressMock, cartDeliveryAddressMock);
        doReturn(redirectAuthoriseServiceRequestMock).when(testObj).createRedirectAuthoriseRequest(merchantInfoMock, additionalAuthInfoMock, shopperMock, basicOrderInfoMock, paymentTypeListMock, null, potentialPickupAddressMock, billingAddressMock);
        doReturn(redirectAuthoriseServiceRequestMock).when(testObj).createRedirectTokenAndAuthoriseRequest(merchantInfoMock, additionalAuthInfoMock, authenticatedShopperMock, basicOrderInfoMock, paymentTypeListMock, null, potentialPickupAddressMock, billingAddressMock, tokenRequestMock);
    }

    @Test
    public void shouldGenerateOrderCode() throws WorldpayException {

        testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);

        verify(worldpayOrderServiceMock).createBasicOrderInfo(eq(WORLDPAY_ORDER_CODE), eq(WORLDPAY_ORDER_CODE), any(Amount.class));
        verify(worldpayDeliveryAddressStrategyMock).getDeliveryAddress(cartModelMock);
    }

    @Test
    public void testRedirectAuthoriseUseShippingAsBilling() throws WorldpayException {
        when(additionalAuthInfoMock.getUsingShippingAsBilling()).thenReturn(TRUE);
        when(cartModelMock.getDeliveryAddress()).thenReturn(cartDeliveryAddressModelMock);

        final PaymentData result = testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);

        assertEquals(REDIRECT_URL, result.getPostUrl());
        assertEquals(COUNTRY_CODE, result.getParameters().get(KEY_COUNTRY));
        assertEquals(LANGUAGE_ISO_CODE, result.getParameters().get(KEY_LANGUAGE));
        assertEquals(FULL_SUCCESS_URL, result.getParameters().get(KEY_SUCCESS_URL));
        assertEquals(FULL_PENDING_URL, result.getParameters().get(KEY_PENDING_URL));
        assertEquals(FULL_FAILURE_URL, result.getParameters().get(KEY_FAILURE_URL));
        assertEquals(FULL_CANCEL_URL, result.getParameters().get(KEY_CANCEL_URL));
        verify(testObj).createRedirectAuthoriseRequest(merchantInfoMock, additionalAuthInfoMock, shopperMock, basicOrderInfoMock, paymentTypeListMock, null, potentialPickupAddressMock, cartDeliveryAddressMock);
    }

    @Test
    public void shouldUseBillingAddressWhenShippingAddressIsNull() throws WorldpayException {
        when(cartModelMock.getDeliveryAddress()).thenReturn(null);

        testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);

        verify(testObj).createRedirectAuthoriseRequest(merchantInfoMock, additionalAuthInfoMock, shopperMock, basicOrderInfoMock, paymentTypeListMock, null, potentialPickupAddressMock, billingAddressMock);
    }

    @Test
    public void shouldUseBillingAddress() throws WorldpayException {
        when(additionalAuthInfoMock.getUsingShippingAsBilling()).thenReturn(FALSE);

        testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);

        verify(testObj).createRedirectAuthoriseRequest(merchantInfoMock, additionalAuthInfoMock, shopperMock, basicOrderInfoMock, paymentTypeListMock, null, potentialPickupAddressMock, billingAddressMock);
    }

    @Test(expected = WorldpayException.class)
    public void testRedirectAuthoriseShouldThrowWorldpayExceptionIfRedirectUrlIsNull() throws WorldpayException {
        doReturn(redirectAuthoriseServiceRequestMock).when(testObj).createRedirectAuthoriseRequest(eq(merchantInfoMock), eq(additionalAuthInfoMock), eq(shopperMock), eq(basicOrderInfoMock), anyListOf(PaymentType.class), anyListOf(PaymentType.class), eq(potentialPickupAddressMock), eq(potentialPickupAddressMock));
        when(redirectReferenceMock.getValue()).thenReturn(EMPTY);

        testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);
    }

    @Test
    public void testRedirectAuthoriseNotUseShippingAsBilling() throws WorldpayException {
        when(additionalAuthInfoMock.getUsingShippingAsBilling()).thenReturn(FALSE);
        when(worldpayOrderServiceMock.createAuthenticatedShopper(CUSTOMER_EMAIL, AUTHENTICATED_SHOPPER_ID, null, null)).thenReturn(authenticatedShopperMock);
        when(worldpayOrderServiceMock.createTokenRequest(TOKEN_EVENT_REFERENCE, null)).thenReturn(tokenRequestMock);
        when(additionalAuthInfoMock.getSaveCard()).thenReturn(TRUE);

        final PaymentData result = testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);

        assertEquals(REDIRECT_URL, result.getPostUrl());
        assertEquals(COUNTRY_CODE, result.getParameters().get(KEY_COUNTRY));
        assertEquals(LANGUAGE_ISO_CODE, result.getParameters().get(KEY_LANGUAGE));
        assertEquals(FULL_SUCCESS_URL, result.getParameters().get(KEY_SUCCESS_URL));
        assertEquals(FULL_PENDING_URL, result.getParameters().get(KEY_PENDING_URL));
        assertEquals(FULL_FAILURE_URL, result.getParameters().get(KEY_FAILURE_URL));
        assertEquals(FULL_CANCEL_URL, result.getParameters().get(KEY_CANCEL_URL));
        assertEquals(FULL_ERROR_URL, result.getParameters().get(KEY_ERROR_URL));

        verify(worldpayURIServiceMock).extractUrlParamsToMap(eq(REDIRECT_URL), anyMapOf(String.class, String.class));
        verify(sessionServiceMock).setAttribute(WORLDPAY_MERCHANT_CODE, MERCHANT_CODE);
    }

    @Test
    public void shouldCreateRedirectRequestForTokenisationWhenSavedCardIsEnabled() throws WorldpayException {
        when(additionalAuthInfoMock.getUsingShippingAsBilling()).thenReturn(FALSE);
        when(additionalAuthInfoMock.getSaveCard()).thenReturn(TRUE);
        when(worldpayOrderServiceMock.createAuthenticatedShopper(CUSTOMER_EMAIL, AUTHENTICATED_SHOPPER_ID, null, null)).thenReturn(authenticatedShopperMock);
        when(worldpayOrderServiceMock.createTokenRequest(TOKEN_EVENT_REFERENCE, null)).thenReturn(tokenRequestMock);

        final PaymentData result = testObj.redirectAuthorise(merchantInfoMock, cartModelMock, additionalAuthInfoMock);

        assertEquals(REDIRECT_URL, result.getPostUrl());
        assertEquals(COUNTRY_CODE, result.getParameters().get(KEY_COUNTRY));
        assertEquals(LANGUAGE_ISO_CODE, result.getParameters().get(KEY_LANGUAGE));
        assertEquals(FULL_SUCCESS_URL, result.getParameters().get(KEY_SUCCESS_URL));
        assertEquals(FULL_PENDING_URL, result.getParameters().get(KEY_PENDING_URL));
        assertEquals(FULL_FAILURE_URL, result.getParameters().get(KEY_FAILURE_URL));
        assertEquals(FULL_CANCEL_URL, result.getParameters().get(KEY_CANCEL_URL));
        assertEquals(FULL_CANCEL_URL, result.getParameters().get(KEY_CANCEL_URL));

        verify(worldpayURIServiceMock).extractUrlParamsToMap(eq(REDIRECT_URL), anyMapOf(String.class, String.class));
        verify(sessionServiceMock).setAttribute(WORLDPAY_MERCHANT_CODE, MERCHANT_CODE);
    }

    @Test
    public void testCompleteRedirectAuthoriseAndSetSavedPaymentInfoToTrue() {
        when(redirectAuthoriseResultMock.getSaveCard()).thenReturn(true);
        setUpRedirectAuthoriseResultMock(AUTHORISED);
        when(worldpayPaymentTransactionServiceMock.createPaymentTransaction(true, MERCHANT_CODE, commerceCheckoutParameterMock)).thenReturn(paymentTransactionModelMock);

        testObj.completeRedirectAuthorise(redirectAuthoriseResultMock, MERCHANT_CODE, cartModelMock);

        verify(testObj).cloneAndSetBillingAddressFromCart(cartModelMock, paymentInfoModelMock);
        verify(testObj).createCommerceCheckoutParameter(cartModelMock, paymentInfoModelMock, bigDecimalMock);
        verify(commerceCheckoutServiceMock).setPaymentInfo(commerceCheckoutParameterMock);
        verify(worldpayPaymentTransactionServiceMock).createPendingAuthorisePaymentTransactionEntry(paymentTransactionModelMock, MERCHANT_CODE, cartModelMock, bigDecimalMock);
    }

    @Test
    public void testCompleteRedirectAuthoriseAndSetSavedPaymentInfoToFalse() {
        setUpRedirectAuthoriseResultMock(AUTHORISED);
        when(worldpayPaymentTransactionServiceMock.createPaymentTransaction(true, MERCHANT_CODE, commerceCheckoutParameterMock)).thenReturn(paymentTransactionModelMock);

        testObj.completeRedirectAuthorise(redirectAuthoriseResultMock, MERCHANT_CODE, cartModelMock);

        verify(testObj).cloneAndSetBillingAddressFromCart(cartModelMock, paymentInfoModelMock);
        verify(testObj).createCommerceCheckoutParameter(cartModelMock, paymentInfoModelMock, bigDecimalMock);
        verify(worldpayPaymentTransactionServiceMock).createPaymentTransaction(true, MERCHANT_CODE, commerceCheckoutParameterMock);
        verify(worldpayPaymentTransactionServiceMock).createPendingAuthorisePaymentTransactionEntry(paymentTransactionModelMock, MERCHANT_CODE, cartModelMock, bigDecimalMock);
    }

    @Test
    public void testCheckResponseIsValidWhenNotUsingMacValidation() {
        when(merchantInfoMock.isUsingMacValidation()).thenReturn(false);

        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createWorldpayResponseWithOrderKeyOnly());

        assertTrue(result);
    }

    @Test
    public void testCheckResponseIsValidWhenRedirectResponseStatusIsOPEN() {
        when(merchantInfoMock.isUsingMacValidation()).thenReturn(true);

        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createWorldpayResponseWithOrderKeyOnly());

        assertTrue(result);
    }

    @Test
    public void testCheckResponseIsValidWhenUsingMacValidationAndRedirectResultStatusIsAUTHORISED() {
        when(merchantInfoMock.isUsingMacValidation()).thenReturn(true);

        doReturn(true).when(testObj).validateResponse(merchantInfoMock, ORDER_KEY, KEY_MAC, String.valueOf(PAYMENT_AMOUNT), GBP, AUTHORISED);

        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createFullWorldpayResponse());

        assertTrue(result);
    }

    @Test
    public void testCheckResponseIsNotValidWhenUsingMacValidationAndRedirectResultStatusIsAUTHORISED() {
        when(merchantInfoMock.isUsingMacValidation()).thenReturn(true);
        setUpRedirectAuthoriseResultMock(AUTHORISED);

        doReturn(false).when(testObj).validateResponse(merchantInfoMock, ORDER_KEY, KEY_MAC, String.valueOf(PAYMENT_AMOUNT), GBP, AUTHORISED);

        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createFullWorldpayResponse());

        assertFalse(result);
    }

    @Test
    public void testCheckResponseIsValidWhenUsingMacValidationWithMac2ParameterAndRedirectResultStatusIsAUTHORISED() {
        when(merchantInfoMock.isUsingMacValidation()).thenReturn(true);
        setUpRedirectAuthoriseResultMock(AUTHORISED);

        doReturn(true).when(testObj).validateResponse(merchantInfoMock, ORDER_KEY, KEY_MAC2, String.valueOf(PAYMENT_AMOUNT), GBP, AUTHORISED);

        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createFullWorldpayResponseWithMac2());

        assertTrue(result);
    }

    @Test
    public void testCheckResponseIsNotValidWhenUsingMacValidationWithMac2ParameterAndRedirectResultStatusIsAUTHORISED() {
        when(merchantInfoMock.isUsingMacValidation()).thenReturn(true);
        setUpRedirectAuthoriseResultMock(AUTHORISED);

        doReturn(false).when(testObj).validateResponse(merchantInfoMock, ORDER_KEY, KEY_MAC2, String.valueOf(PAYMENT_AMOUNT), GBP, AUTHORISED);

        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createFullWorldpayResponseWithMac2());

        assertFalse(result);
    }

    @Test
    public void testCheckResponseIsNotValidWhenNoOrderKeyPresent() {
        final boolean result = testObj.validateRedirectResponse(merchantInfoMock, createWorldpayResponseWithNoOrderKey());

        assertFalse(result);
    }

    private Map<String, String> createFullWorldpayResponse() {
        final Map<String, String> worldpayResponse = new HashMap<>();
        worldpayResponse.put(PAYMENT_STATUS, AUTHORISED.name());
        worldpayResponse.put(ORDER_KEY, ORDER_KEY);
        worldpayResponse.put(KEY_MAC, KEY_MAC);
        worldpayResponse.put(KEY_PAYMENT_AMOUNT, String.valueOf(PAYMENT_AMOUNT));
        worldpayResponse.put(KEY_PAYMENT_CURRENCY, GBP);
        return worldpayResponse;
    }

    private Map<String, String> createFullWorldpayResponseWithMac2() {
        final Map<String, String> worldpayResponse = new HashMap<>();
        worldpayResponse.put(PAYMENT_STATUS, AUTHORISED.name());
        worldpayResponse.put(ORDER_KEY, ORDER_KEY);
        worldpayResponse.put(KEY_MAC2, KEY_MAC2);
        worldpayResponse.put(KEY_PAYMENT_AMOUNT, String.valueOf(PAYMENT_AMOUNT));
        worldpayResponse.put(KEY_PAYMENT_CURRENCY, GBP);
        return worldpayResponse;
    }

    private Map<String, String> createWorldpayResponseWithNoOrderKey() {
        final Map<String, String> worldpayResponse = new HashMap<>();
        worldpayResponse.put(PAYMENT_STATUS, AUTHORISED.name());
        worldpayResponse.put(KEY_MAC, KEY_MAC);
        worldpayResponse.put(KEY_PAYMENT_AMOUNT, String.valueOf(PAYMENT_AMOUNT));
        worldpayResponse.put(KEY_PAYMENT_CURRENCY, GBP);
        return worldpayResponse;
    }

    private Map<String, String> createWorldpayResponseWithOrderKeyOnly() {
        final Map<String, String> worldpayResponse = new HashMap<>();
        worldpayResponse.put(ORDER_KEY, ORDER_KEY);
        return worldpayResponse;
    }

    private void setUpRedirectAuthoriseResultMock(final AuthorisedStatus paymentStatus) {
        when(redirectAuthoriseResultMock.getOrderCode()).thenReturn(WORLDPAY_ORDER_CODE);
        when(redirectAuthoriseResultMock.getPaymentStatus()).thenReturn(paymentStatus);
        when(redirectAuthoriseResultMock.getOrderKey()).thenReturn(ORDER_KEY);
        when(redirectAuthoriseResultMock.getPending()).thenReturn(true);
        when(redirectAuthoriseResultMock.getPaymentAmount()).thenReturn(bigDecimalMock);
    }
}
