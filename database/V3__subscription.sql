/* =====================================================
   PREMIUM PACKAGES
===================================================== */

CREATE TABLE premiums
(
    premium_id BIGINT IDENTITY(1,1) PRIMARY KEY,

    package_name VARCHAR(100) NOT NULL UNIQUE,

    amount DECIMAL(12,2) NOT NULL,

    duration_days INT NOT NULL,

    description NVARCHAR(500),

    is_active BIT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    updated_at DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* =====================================================
   DISCOUNTS
===================================================== */

CREATE TABLE discounts
(
    discount_id BIGINT IDENTITY(1,1) PRIMARY KEY,

    discount_name NVARCHAR(255) NOT NULL,

    discount_percent DECIMAL(5,2) NOT NULL,

    from_date DATETIME NOT NULL,

    to_date DATETIME NOT NULL,

    is_active BIT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* =====================================================
   PREMIUM - DISCOUNT (MANY TO MANY)
===================================================== */

CREATE TABLE premium_discounts
(
    premium_id BIGINT NOT NULL,

    discount_id BIGINT NOT NULL,

    PRIMARY KEY (premium_id, discount_id),

    CONSTRAINT FK_PD_PREMIUM
        FOREIGN KEY (premium_id)
            REFERENCES premiums(premium_id),

    CONSTRAINT FK_PD_DISCOUNT
        FOREIGN KEY (discount_id)
            REFERENCES discounts(discount_id)
);
GO

/* =====================================================
   INVOICES
===================================================== */

CREATE TABLE invoices
(
    invoice_id BIGINT IDENTITY(1,1) PRIMARY KEY,

    user_id BIGINT NOT NULL,

    premium_id BIGINT NOT NULL,

    discount_id BIGINT NULL,

    original_amount DECIMAL(12,2) NOT NULL,

    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,

    final_amount DECIMAL(12,2) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_INVOICE_USER
        FOREIGN KEY (user_id)
            REFERENCES users(user_id),

    CONSTRAINT FK_INVOICE_PREMIUM
        FOREIGN KEY (premium_id)
            REFERENCES premiums(premium_id),

    CONSTRAINT FK_INVOICE_DISCOUNT
        FOREIGN KEY (discount_id)
            REFERENCES discounts(discount_id)
);
GO

/* =====================================================
   PAYMENT TRANSACTIONS
===================================================== */

CREATE TABLE payment_transactions
(
    transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY,

    invoice_id BIGINT NOT NULL,

    payment_method VARCHAR(50) NOT NULL,

    amount_paid DECIMAL(12,2) NOT NULL,

    transaction_status VARCHAR(30) NOT NULL,

    transaction_date DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_PAYMENT_INVOICE
        FOREIGN KEY (invoice_id)
            REFERENCES invoices(invoice_id)
);
GO

/* =====================================================
   USER SUBSCRIPTIONS
===================================================== */

CREATE TABLE user_subscriptions
(
    subscription_id BIGINT IDENTITY(1,1) PRIMARY KEY,

    user_id BIGINT NOT NULL,

    premium_id BIGINT NOT NULL,

    start_date DATETIME NOT NULL,

    end_date DATETIME NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_SUB_USER
        FOREIGN KEY (user_id)
            REFERENCES users(user_id),

    CONSTRAINT FK_SUB_PREMIUM
        FOREIGN KEY (premium_id)
            REFERENCES premiums(premium_id)
);
GO