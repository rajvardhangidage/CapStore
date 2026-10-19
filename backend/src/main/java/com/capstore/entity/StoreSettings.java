package com.capstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "store_settings")
@Getter @Setter @NoArgsConstructor
public class StoreSettings {
    @Id
    private Long id = 1L;

    @Column(name = "gst_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRatePercent;

    @Column(name = "shipping_flat_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFlatRate;

    @Column(name = "free_shipping_above", nullable = false, precision = 10, scale = 2)
    private BigDecimal freeShippingAbove;
}
