package com.moses.dse_track.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DsePriceDto {

    private boolean success;
    private List<DseStock> data;

    @Data
    public static class DseStock {
        private Integer id;

        @JsonProperty("company")
        private String ticker;

        private BigDecimal price;
        private BigDecimal change;
    }
}