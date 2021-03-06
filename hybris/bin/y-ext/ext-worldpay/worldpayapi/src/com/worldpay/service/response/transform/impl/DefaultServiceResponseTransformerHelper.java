package com.worldpay.service.response.transform.impl;

import com.worldpay.enums.order.AuthorisedStatus;
import com.worldpay.internal.model.*;
import com.worldpay.internal.model.Error;
import com.worldpay.service.model.*;
import com.worldpay.service.model.Address;
import com.worldpay.service.model.Amount;
import com.worldpay.service.model.Date;
import com.worldpay.service.model.RiskScore;
import com.worldpay.service.model.payment.PaymentType;
import com.worldpay.service.model.token.DeleteTokenReply;
import com.worldpay.service.model.token.TokenReply;
import com.worldpay.service.model.token.UpdateTokenReply;
import com.worldpay.service.response.ServiceResponse;
import com.worldpay.service.response.transform.ServiceResponseTransformerHelper;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class DefaultServiceResponseTransformerHelper implements ServiceResponseTransformerHelper {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkForError(ServiceResponse response, Reply reply) {
        final Object replyType = reply.getOrderStatusOrBatchStatusOrErrorOrAddressCheckResponseOrRefundableAmountOrAccountBatchOrShopperOrOkOrFuturePayAgreementStatusOrShopperAuthenticationResultOrFuturePayPaymentResultOrPricePointOrPaymentOptionOrToken().get(0);
        if (replyType instanceof com.worldpay.internal.model.Error) {
            final com.worldpay.internal.model.Error intError = (com.worldpay.internal.model.Error) replyType;
            final ErrorDetail errorDtl = buildErrorDetail(intError);
            response.setError(errorDtl);
            return true;
        } else if (replyType instanceof OrderStatus) {
            OrderStatus ordStatus = (OrderStatus) replyType;
            Object statusType = ordStatus.getReferenceOrBankAccountOrApmEnrichedDataOrErrorOrPaymentOrCardBalanceOrPaymentAdditionalDetailsOrBillingAddressDetailsOrOrderModificationOrJournalOrRequestInfoOrFxApprovalRequiredOrZappRTPOrContent().get(0);
            if (statusType instanceof com.worldpay.internal.model.Error) {
                final com.worldpay.internal.model.Error intError = (com.worldpay.internal.model.Error) statusType;
                final ErrorDetail errorDtl = buildErrorDetail(intError);
                response.setError(errorDtl);
                return true;
            }
        }
        return false;
    }

    private ErrorDetail buildErrorDetail(com.worldpay.internal.model.Error intError) {
        ErrorDetail errorDtl = null;
        if (intError != null) {
            errorDtl = new ErrorDetail(intError.getCode(), intError.getvalue());
        }
        return errorDtl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentReply buildPaymentReply(final Payment intPayment) {
        PaymentReply paymentReply = new PaymentReply();
        paymentReply.setMethodCode(intPayment.getPaymentMethod());
        final PaymentMethodDetail paymentMethodDetail = intPayment.getPaymentMethodDetail();
        if (paymentMethodDetail != null) {
            paymentReply.setCardDetails(transformCard(paymentMethodDetail.getCard(), intPayment.getCardHolderName()));
        }

        com.worldpay.internal.model.Amount intAmount = intPayment.getAmount();
        final Amount amount = transformAmount(intAmount);
        paymentReply.setAmount(amount);
        paymentReply.setAuthStatus(AuthorisedStatus.valueOf(intPayment.getLastEvent()));
        CVCResultCode intCvcResultCode = intPayment.getCVCResultCode();
        if (intCvcResultCode != null) {
            String cvcResultDescription = intCvcResultCode.getDescription().get(0);
            paymentReply.setCvcResultDescription(cvcResultDescription);
        }
        final List<Balance> balanceList = intPayment.getBalance();
        if (balanceList != null && !balanceList.isEmpty()) {
            Balance balance = balanceList.get(0);
            paymentReply.setBalanceAccountType(balance.getAccountType());
            com.worldpay.internal.model.Amount intBalAmount = balance.getAmount();
            Amount balAmount = transformAmount(intBalAmount);
            paymentReply.setBalanceAmount(balAmount);
        }

        if (intPayment.getISO8583ReturnCode() != null) {
            paymentReply.setReturnCode(intPayment.getISO8583ReturnCode().getCode());
        }
        final RiskScore riskScore = buildRiskScore(intPayment.getRiskScore());
        paymentReply.setRiskScore(riskScore);

        setAAVCodes(intPayment, paymentReply);

        paymentReply.setRefundReference(intPayment.getRefundReference());

        final AuthorisationId authorisationId = intPayment.getAuthorisationId();
        if (authorisationId != null) {
            paymentReply.setAuthorisationId(authorisationId.getId());
            final String authorisedBy = intPayment.getAuthorisationId().getBy();
            if (authorisedBy != null) {
                paymentReply.setAuthorisedBy(authorisedBy);
            }
        }

        return paymentReply;
    }

    private RiskScore buildRiskScore(final com.worldpay.internal.model.RiskScore intRiskScore) {
        final RiskScore riskScore = new RiskScore();
        riskScore.setExtendedResponse(intRiskScore.getExtendedResponse());
        riskScore.setFinalScore(intRiskScore.getFinalScore());
        riskScore.setId(intRiskScore.getId());
        riskScore.setValue(intRiskScore.getValue());
        riskScore.setMessage(intRiskScore.getMessage());
        riskScore.setProvider(intRiskScore.getProvider());
        riskScore.setRGID(intRiskScore.getRGID());
        riskScore.setTRisk(intRiskScore.getTRisk());
        riskScore.setTScore(intRiskScore.getTScore());
        return riskScore;
    }

    @Override
    public UpdateTokenReply buildUpdateTokenReply(final UpdateTokenReceived intUpdateTokenReceived) {
        final UpdateTokenReply updateTokenReply = new UpdateTokenReply();
        updateTokenReply.setPaymentTokenId(intUpdateTokenReceived.getPaymentTokenID());
        return updateTokenReply;
    }

    @Override
    public DeleteTokenReply buildDeleteTokenReply(final DeleteTokenReceived intDeleteTokenReceived) {
        final DeleteTokenReply deleteTokenReply = new DeleteTokenReply();
        deleteTokenReply.setPaymentTokenId(intDeleteTokenReceived.getPaymentTokenID());
        return deleteTokenReply;
    }

    @Override
    public TokenReply buildTokenReply(final Token intToken) {
        final TokenReply tokenReply = new TokenReply();
        tokenReply.setAuthenticatedShopperID(intToken.getAuthenticatedShopperID());
        tokenReply.setTokenEventReference(intToken.getTokenEventReference());

        final List<Object> tokenInformationFields = intToken.getTokenReasonOrTokenDetailsOrPaymentInstrumentOrError();
        for (final Object tokenInformationField : tokenInformationFields) {
            if (tokenInformationField instanceof TokenReason) {
                tokenReply.setTokenReason(((TokenReason) tokenInformationField).getvalue());
            } else if (tokenInformationField instanceof TokenDetails) {
                final com.worldpay.service.model.token.TokenDetails tokenDetails = transformTokenDetails((TokenDetails) tokenInformationField);
                tokenReply.setTokenDetails(tokenDetails);
            } else if (tokenInformationField instanceof PaymentInstrument) {
                final Object paymentInstrument = ((PaymentInstrument) tokenInformationField).getCardDetailsOrPaypalOrSepaOrEmvcoTokenDetails().get(0);
                if (paymentInstrument instanceof CardDetails) {
                    final com.worldpay.service.model.payment.Card card = transformCard((CardDetails) paymentInstrument);
                    tokenReply.setPaymentInstrument(card);
                } else if (paymentInstrument instanceof Paypal) {
                    tokenReply.setPaypalDetails(((Paypal) paymentInstrument).getvalue());
                }
            } else if (tokenInformationField instanceof Error) {
                tokenReply.setError(buildErrorDetail((Error) tokenInformationField));
            }
        }
        return tokenReply;
    }

    private com.worldpay.service.model.token.TokenDetails transformTokenDetails(final TokenDetails tokenInformationField) {
        final com.worldpay.service.model.token.TokenDetails tokenDetails = new com.worldpay.service.model.token.TokenDetails();
        final TokenReason intTokenReason = tokenInformationField.getTokenReason();
        if (intTokenReason != null) {
            tokenDetails.setTokenReason(tokenInformationField.getTokenReason().getvalue());
        }
        tokenDetails.setTokenEventReference(tokenInformationField.getTokenEventReference());
        tokenDetails.setTokenEvent(tokenInformationField.getTokenEvent());
        tokenDetails.setPaymentTokenExpiry(transformDate(tokenInformationField.getPaymentTokenExpiry().getDate()));
        tokenDetails.setPaymentTokenID(tokenInformationField.getPaymentTokenID());
        tokenDetails.setReportingTokenID(tokenInformationField.getReportingTokenID());
        if (tokenInformationField.getReportingTokenExpiry() != null) {
            tokenDetails.setReportingTokenExpiry(transformDate(tokenInformationField.getReportingTokenExpiry().getDate()));
        }
        return tokenDetails;
    }

    @Override
    public WebformRefundReply buildWebformRefundReply(final ShopperWebformRefundDetails intShopperWebformRefundDetails) {
        final WebformRefundReply webformRefundReply = new WebformRefundReply();
        webformRefundReply.setWebformURL(intShopperWebformRefundDetails.getWebformURL());
        webformRefundReply.setWebformId(intShopperWebformRefundDetails.getWebformId());
        webformRefundReply.setWebformStatus(intShopperWebformRefundDetails.getWebformStatus());
        webformRefundReply.setPaymentId(intShopperWebformRefundDetails.getPaymentId());
        webformRefundReply.setReason(intShopperWebformRefundDetails.getReason());
        webformRefundReply.setAmount(transformAmount(intShopperWebformRefundDetails.getAmount()));
        webformRefundReply.setRefundId(intShopperWebformRefundDetails.getRefundId());
        return webformRefundReply;
    }

    private Amount transformAmount(final com.worldpay.internal.model.Amount intAmount) {
        return new Amount(intAmount.getValue(), intAmount.getCurrencyCode(), intAmount.getExponent(), DebitCreditIndicator.getDebitCreditIndicator(intAmount.getDebitCreditIndicator()));
    }

    private void setAAVCodes(final Payment intPayment, final PaymentReply paymentReply) {
        final AAVCardholderNameResultCode aavCardholderNameResultCode = intPayment.getAAVCardholderNameResultCode();
        if (aavCardholderNameResultCode != null && CollectionUtils.isNotEmpty(aavCardholderNameResultCode.getDescription())) {
            paymentReply.setAavCardholderNameResultCode(aavCardholderNameResultCode.getDescription().get(0));
        }
        final AAVAddressResultCode aavAddressResultCode = intPayment.getAAVAddressResultCode();
        if (aavAddressResultCode != null && CollectionUtils.isNotEmpty(aavAddressResultCode.getDescription())) {
            paymentReply.setAavAddressResultCode(aavAddressResultCode.getDescription().get(0));
        }
        final AAVEmailResultCode aavEmailResultCode = intPayment.getAAVEmailResultCode();
        if (aavEmailResultCode != null && CollectionUtils.isNotEmpty(aavEmailResultCode.getDescription())) {
            paymentReply.setAavEmailResultCode(aavEmailResultCode.getDescription().get(0));
        }
        final AAVPostcodeResultCode aavPostcodeResultCode = intPayment.getAAVPostcodeResultCode();
        if (aavPostcodeResultCode != null && CollectionUtils.isNotEmpty(aavPostcodeResultCode.getDescription())) {
            paymentReply.setAavPostcodeResultCode(aavPostcodeResultCode.getDescription().get(0));
        }
        final AAVTelephoneResultCode aavTelephoneResultCode = intPayment.getAAVTelephoneResultCode();
        if (aavTelephoneResultCode != null && CollectionUtils.isNotEmpty(aavTelephoneResultCode.getDescription())) {
            paymentReply.setAavTelephoneResultCode(aavTelephoneResultCode.getDescription().get(0));
        }
    }

    private com.worldpay.service.model.payment.Card transformCard(final CardDetails intCardDetails) {
        final Derived derived = intCardDetails.getDerived();
        if (derived != null) {
            final String cvc = intCardDetails.getCvc() == null ? null : intCardDetails.getCvc().getvalue();
            Date expiryDate = null;
            if (intCardDetails.getExpiryDate() != null) {
                expiryDate = transformDate(intCardDetails.getExpiryDate().getDate());
            }
            final Address address = transformAddress(intCardDetails.getCardAddress());
            setCardBrand(derived);
            final String cardHolderName = transformCardHolderName(intCardDetails.getCardHolderName());
            return new com.worldpay.service.model.payment.Card(PaymentType.getPaymentType(derived.getCardBrand()),
                    derived.getObfuscatedPAN(), cvc, expiryDate, cardHolderName, address, null, null, null);
        }
        return null;
    }

    private void setCardBrand(final Derived derived) {
        if (derived.getCardCoBrand() != null) {
            if ("VISA".equals(derived.getCardBrand()) && "CARTEBLEUE".equals(derived.getCardCoBrand())) {
                derived.setCardBrand(PaymentType.CARTE_BLEUE.getMethodCode());
            }

            if ("ECMC".equals(derived.getCardBrand()) && "CB".equals(derived.getCardCoBrand())) {
                derived.setCardBrand(PaymentType.CARTE_BANCAIRE.getMethodCode());
            }
        } else {
            switch (derived.getCardBrand()) {
                case "VISA":
                    derived.setCardBrand(PaymentType.VISA.getMethodCode());
                    break;
                case "ECMC":
                    derived.setCardBrand(PaymentType.MASTERCARD.getMethodCode());
                    break;
                case "AIRPLUS":
                    derived.setCardBrand(PaymentType.AIRPLUS.getMethodCode());
                    break;
                case "AMEX":
                    derived.setCardBrand(PaymentType.AMERICAN_EXPRESS.getMethodCode());
                    break;
                case "DANKORT":
                    derived.setCardBrand(PaymentType.DANKORT.getMethodCode());
                    break;
                case "DINERS":
                    derived.setCardBrand(PaymentType.DINERS.getMethodCode());
                    break;
                case "DISCOVER":
                    derived.setCardBrand(PaymentType.DISCOVER.getMethodCode());
                    break;
                case "JCB":
                    derived.setCardBrand(PaymentType.JCB.getMethodCode());
                    break;
                case "MAESTRO":
                    derived.setCardBrand(PaymentType.MAESTRO.getMethodCode());
                    break;
                case "UATP":
                    derived.setCardBrand(PaymentType.UATP.getMethodCode());
                    break;
                default:
                    derived.setCardBrand(PaymentType.CARD_SSL.getMethodCode());
                    break;
            }
        }
    }

    private com.worldpay.service.model.payment.Card transformCard(final Card intCard, final CardHolderName cardHolderName) {
        Date transformedDate = null;
        String cardHolderNameValue = transformCardHolderName(cardHolderName);
        if (intCard.getExpiryDate() != null) {
            transformedDate = transformDate(intCard.getExpiryDate().getDate());
        }
        return new com.worldpay.service.model.payment.Card(PaymentType.getPaymentType(intCard.getType()),
                intCard.getNumber(), null, transformedDate, cardHolderNameValue, null, null, null, null);
    }

    private String transformCardHolderName(final CardHolderName cardHolderName) {
        if (cardHolderName != null && cardHolderName.getvalue() != null && cardHolderName.getvalue().length() > 0) {
            return cardHolderName.getvalue();
        }
        return null;
    }

    private Address transformAddress(final com.worldpay.internal.model.CardAddress intCardAddress) {
        if (intCardAddress != null && intCardAddress.getAddress() != null) {
            com.worldpay.internal.model.Address intAddress = intCardAddress.getAddress();
            final List<Object> streetOrHouseNameOrHouseNumberOrHouseNumberExtensionOrAddress1OrAddress2OrAddress3 = intAddress.getStreetOrHouseNameOrHouseNumberOrHouseNumberExtensionOrAddress1OrAddress2OrAddress3();
            Address address = new Address();
            setAddressFields(streetOrHouseNameOrHouseNumberOrHouseNumberExtensionOrAddress1OrAddress2OrAddress3, address);
            address.setFirstName(intAddress.getFirstName());
            address.setLastName(intAddress.getLastName());
            address.setCity(intAddress.getCity());
            address.setCountryCode(intAddress.getCountryCode());
            address.setPostalCode(intAddress.getPostalCode());
            address.setState(intAddress.getState());
            address.setTelephoneNumber(intAddress.getTelephoneNumber());
            return address;
        }
        return null;
    }

    private void setAddressFields(final List<Object> streetOrHouseNameOrHouseNumberOrHouseNumberExtensionOrAddress1OrAddress2OrAddress3, final Address address) {
        for (final Object intAddressDetails : streetOrHouseNameOrHouseNumberOrHouseNumberExtensionOrAddress1OrAddress2OrAddress3) {
            if (intAddressDetails instanceof Street) {
                address.setStreet(((Street) intAddressDetails).getvalue());
            }
            if (intAddressDetails instanceof HouseName) {
                address.setHouseName(((HouseName) intAddressDetails).getvalue());
            }
            if (intAddressDetails instanceof HouseNumber) {
                address.setHouseNumber(((HouseNumber) intAddressDetails).getvalue());
            }
            if (intAddressDetails instanceof HouseNumberExtension) {
                address.setHouseNumberExtension(((HouseNumberExtension) intAddressDetails).getvalue());
            }
            if (intAddressDetails instanceof Address1) {
                address.setAddress1(((Address1) intAddressDetails).getvalue());
            }
            if (intAddressDetails instanceof Address2) {
                address.setAddress2(((Address2) intAddressDetails).getvalue());
            }
            if (intAddressDetails instanceof Address3) {
                address.setAddress3(((Address3) intAddressDetails).getvalue());
            }
        }
    }

    private Date transformDate(final com.worldpay.internal.model.Date intDate) {
        return new Date(intDate.getDayOfMonth(), intDate.getMonth(), intDate.getYear(), intDate.getHour(), intDate.getMinute(), intDate.getSecond());
    }

    @Override
    public JournalReply buildJournalReply(Journal intJournal) {
        JournalReply journalReply = new JournalReply();
        journalReply.setJournalType(AuthorisedStatus.valueOf(intJournal.getJournalType()));
        populateBookingDate(intJournal, journalReply);
        populateAccountTransactions(intJournal, journalReply);
        return journalReply;
    }

    private void populateBookingDate(Journal intJournal, JournalReply journalReply) {
        Optional.ofNullable(intJournal.getBookingDate())
                .ifPresent(bookingDate -> Optional.ofNullable(bookingDate.getDate()).ifPresent(intDate -> {
                    final Date date = transformDate(intDate);
                    journalReply.setBookingDate(date);
                }));
    }

    private void populateAccountTransactions(Journal intJournal, JournalReply journalReply) {
        List<AccountTx> accountTxs = intJournal.getAccountTx();
        for (AccountTx accountTx : accountTxs) {
            AccountTransaction accountTransaction = new AccountTransaction();

            accountTransaction.setAccountType(accountTx.getAccountType());
            accountTransaction.setBatchId(accountTx.getBatchId());

            com.worldpay.internal.model.Amount amount = accountTx.getAmount();

            if (amount != null) {
                accountTransaction.setAmount(transformAmount(amount));
            }

            journalReply.addAccountTransaction(accountTransaction);
        }
    }
}
