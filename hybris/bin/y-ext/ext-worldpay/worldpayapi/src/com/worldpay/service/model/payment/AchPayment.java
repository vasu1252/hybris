/**
 *
 */
package com.worldpay.service.model.payment;

import com.worldpay.internal.model.Authentication;
import com.worldpay.internal.model.Deposit;
import com.worldpay.internal.model.Validation;
import com.worldpay.internal.model.Verification;
import com.worldpay.service.model.Address;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * POJO representation of an ACH {@link Payment} type
 *
 * @see PaymentBuilder PaymentBuilder for simple static creation methods
 */
public class AchPayment extends AbstractPayment {

    private AchType achType;
    private String firstName;
    private String lastName;
    private Address address;
    private String bankAccountType;
    private String routingNumber;
    private String accountNumber;

    /**
     * Constructor with full list of fields
     *
     * @param firstName
     * @param lastName
     * @param address
     * @param bankAccountType
     * @param routingNumber
     * @param accountNumber
     * @see PaymentBuilder PaymentBuilder for simple static creation methods
     */
    public AchPayment(PaymentType paymentType, AchType achType, String firstName, String lastName, Address address, String bankAccountType, String routingNumber, String accountNumber) {
        this.setPaymentType(paymentType);
        this.achType = achType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.bankAccountType = bankAccountType;
        this.routingNumber = routingNumber;
        this.accountNumber = accountNumber;
    }

    /**
     * Static convenience method to create an Authentication ACH Payment
     *
     * @param firstName
     * @param lastName
     * @param address
     * @return AchPayment object
     */
    public static AchPayment createAuthenticationAchPayment(String firstName, String lastName, Address address) {
        return new AchPayment(PaymentType.ACH, AchType.AUTHENTICATION, firstName, lastName, address, null, null, null);
    }

    /**
     * Static convenience method to create a Deposit ACH Payment
     *
     * @param firstName
     * @param lastName
     * @param bankAccountType
     * @param routingNumber
     * @param accountNumber
     * @return AchPayment object
     */
    public static AchPayment createDepositAchPayment(String firstName, String lastName, String bankAccountType, String routingNumber, String accountNumber) {
        return new AchPayment(PaymentType.ACH, AchType.DEPOSIT, firstName, lastName, null, bankAccountType, routingNumber, accountNumber);
    }

    /**
     * Static convenience method to create a Validation ACH Payment
     *
     * @param firstName
     * @param lastName
     * @param bankAccountType
     * @param routingNumber
     * @param accountNumber
     * @return AchPayment object
     */
    public static AchPayment createValidationAchPayment(String firstName, String lastName, String bankAccountType, String routingNumber, String accountNumber) {
        return new AchPayment(PaymentType.ACH, AchType.VALIDATION, firstName, lastName, null, bankAccountType, routingNumber, accountNumber);
    }

    /**
     * Static convenience method to create a Verification ACH Payment
     *
     * @param bankAccountType
     * @param routingNumber
     * @param accountNumber
     * @return AchPayment object
     */
    public static AchPayment createVerificationAchPayment(String bankAccountType, String routingNumber, String accountNumber) {
        return new AchPayment(PaymentType.ACH, AchType.VERIFICATION, null, null, null, bankAccountType, routingNumber, accountNumber);
    }

    /* (non-Javadoc)
     * @see AbstractPayment#invokeSetter(java.lang.reflect.Method, java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void invokeSetter(Method method, Object targetObject) throws IllegalAccessException, InvocationTargetException {
        String methodName = method.getName();
        if ("getAuthenticationOrDepositOrValidationOrVerification".equals(methodName)) {
            List<Object> intAchType = (List<Object>) method.invoke(targetObject);
            if (achType.equals(AchType.AUTHENTICATION)) {
                Authentication intAuth = new Authentication();
                intAuth.setFirstName(firstName);
                intAuth.setLastName(lastName);
                intAuth.setAddress((com.worldpay.internal.model.Address) address.transformToInternalModel());

                intAchType.add(intAuth);
            } else if (AchType.DEPOSIT.equals(achType)) {
                Deposit intDeposit = new Deposit();
                intDeposit.setFirstName(firstName);
                intDeposit.setLastName(lastName);
                intDeposit.setBankAccountType(bankAccountType);
                intDeposit.setRoutingNumber(routingNumber);
                intDeposit.setAccountNumber(accountNumber);

                intAchType.add(intDeposit);
            } else if (AchType.VALIDATION.equals(achType)) {
                Validation intValidation = new Validation();
                intValidation.setFirstName(firstName);
                intValidation.setLastName(lastName);
                intValidation.setBankAccountType(bankAccountType);
                intValidation.setRoutingNumber(routingNumber);
                intValidation.setAccountNumber(accountNumber);

                intAchType.add(intValidation);
            } else if (AchType.VERIFICATION.equals(achType)) {
                Verification intVerification = new Verification();
                intVerification.setBankAccountType(bankAccountType);
                intVerification.setRoutingNumber(routingNumber);
                intVerification.setAccountNumber(accountNumber);

                intAchType.add(intVerification);
            }
        }
    }

    public AchType getAchType() {
        return achType;
    }

    public void setAchType(AchType achType) {
        this.achType = achType;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getBankAccountType() {
        return bankAccountType;
    }

    public void setBankAccountType(String bankAccountType) {
        this.bankAccountType = bankAccountType;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    /**
     * (non-Javadoc)
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "AchPayment [achType=" + achType + ", firstName=" + firstName + ", lastName=" + lastName + ", address=" + address + ", bankAccountType="
                + bankAccountType + ", routingNumber=" + routingNumber + ", accountNumber=" + accountNumber + "]";
    }
}
