IF OBJECT_ID('dbo.Customer_Behavior_Events', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID('dbo.Customer_Behavior_Events')
          AND name = 'CK_CustomerBehaviorEvents_Type'
    )
        ALTER TABLE dbo.Customer_Behavior_Events DROP CONSTRAINT CK_CustomerBehaviorEvents_Type;

    ALTER TABLE dbo.Customer_Behavior_Events WITH CHECK ADD CONSTRAINT CK_CustomerBehaviorEvents_Type CHECK (
        event_type IN ('PRODUCT_VIEW','CART_ADDED','CHECKOUT_STARTED','ORDER_CREATED','PAYMENT_SUCCESS','AI_QUESTION','AI_PRODUCT_CLICK')
    );
END;
