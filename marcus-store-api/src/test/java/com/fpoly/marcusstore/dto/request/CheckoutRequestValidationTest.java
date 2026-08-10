package com.fpoly.marcusstore.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void checkoutRejectsClientControlledStatusLikePaymentMethodAndMalformedQuantityContext() {
        CheckoutRequestDTO request = validCheckout();
        request.setPaymentMethod("PAID");
        request.setCartItemIds(List.of(0));

        var fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();

        assertThat(fields).contains("paymentMethod", "cartItemIds[0].<list element>");
    }

    @Test
    void checkoutRejectsOversizedAndMalformedCustomerInput() {
        CheckoutRequestDTO request = validCheckout();
        request.setRecipientName("a".repeat(101));
        request.setRecipientPhone("abc");
        request.setNote("a".repeat(501));

        var fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();

        assertThat(fields).contains("recipientName", "recipientPhone", "note");
    }

    @Test
    void cartRejectsMissingOrInvalidSkuAndQuantity() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setSkuId(0);
        request.setQuantity(-1);

        var fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();

        assertThat(fields).contains("skuId", "quantity");
    }

    @Test
    void validCheckoutPassesDtoValidation() {
        assertThat(validator.validate(validCheckout())).isEmpty();
    }

    @Test
    void contactRejectsMalformedEmailEvenWhenBrowserValidationIsBypassed() {
        CreateContactRequest request = new CreateContactRequest();
        request.setName("Marcus");
        request.setPhone("0912345678");
        request.setEmail("marcus-sai-dinh-dang");
        request.setMessage("Tôi cần được hỗ trợ");

        var fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();

        assertThat(fields).contains("email");
    }

    private static CheckoutRequestDTO validCheckout() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setCartItemIds(List.of(1));
        // Marcus thêm: mỗi lần bấm đặt hàng có một khóa chống gửi lặp.
        request.setCheckoutRequestId("checkout-test-request-0001");
        request.setRecipientName("Marcus");
        request.setRecipientPhone("0912345678");
        request.setShippingAddress("118 Cát Bi");
        request.setToDistrictId(1);
        request.setToWardCode("12345");
        request.setFulfillmentMethod("DELIVERY");
        request.setPaymentMethod("VNPAY");
        request.setNote("Giao giờ hành chính");
        return request;
    }
}
