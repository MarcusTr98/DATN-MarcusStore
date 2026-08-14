package com.fpoly.marcusstore.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarcusModuleValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void skuBatchValidatesNestedItems() {
        SkuBatchCreateRequest.SkuItem item = new SkuBatchCreateRequest.SkuItem();
        item.setSkuCode("<script>");
        item.setPrice(BigDecimal.ZERO);
        item.setValueIds(List.of(0));

        SkuBatchCreateRequest request = new SkuBatchCreateRequest();
        request.setProductId(0);
        request.setSkus(List.of(item));

        var fields = fieldNames(request);
        assertThat(fields).contains(
                "productId",
                "skus[0].skuCode",
                "skus[0].price",
                "skus[0].valueIds[0].<list element>");
    }

    @Test
    void skuBulkUpdateValidatesNestedItems() {
        SkuBulkUpdateRequest.SkuUpdateItem item = new SkuBulkUpdateRequest.SkuUpdateItem();
        item.setSkuId(0);
        item.setPrice(new BigDecimal("-1"));
        item.setOriginalPrice(BigDecimal.ZERO);

        SkuBulkUpdateRequest request = new SkuBulkUpdateRequest();
        request.setSkus(List.of(item));

        assertThat(fieldNames(request)).contains(
                "skus[0].skuId",
                "skus[0].price",
                "skus[0].originalPrice");
    }

    @Test
    void attributeAndValueRejectMalformedInput() {
        AttributeRequest attribute = new AttributeRequest();
        attribute.setName("<script>");

        AttributeValueRequest value = new AttributeValueRequest();
        value.setAttributeId(0);
        value.setValueString(" ");

        assertThat(fieldNames(attribute)).contains("name");
        assertThat(fieldNames(value)).contains("attributeId", "valueString");
    }

    @Test
    void administrativeRequestsRejectMissingOrOversizedInput() {
        AdminAvailabilityRequest availability = new AdminAvailabilityRequest();
        FinancialReconcileRequest reconcile = new FinancialReconcileRequest();
        BulkUpdateSettingsRequest settings = new BulkUpdateSettingsRequest();
        settings.setSettings(Map.of("BAD KEY", "x"));

        assertThat(fieldNames(availability)).contains("available");
        assertThat(fieldNames(reconcile)).contains("status");
        assertThat(fieldNames(settings)).contains("settings<K>[BAD KEY].<map key>");
    }

    @Test
    void ghnWebhookRejectsUntrustedShape() {
        GhnWebhookRequest request = new GhnWebhookRequest();
        request.setOrderCode("../order");
        request.setStatus("INVALID STATUS");

        assertThat(fieldNames(request)).contains("orderCode", "status");
    }

    @Test
    void orderStatusRequestRejectsUnknownStatusAndOversizedNote() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("DELETE_ALL");
        request.setNote("x".repeat(501));

        assertThat(fieldNames(request)).contains("status", "note");
    }

    private static List<String> fieldNames(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }
}
