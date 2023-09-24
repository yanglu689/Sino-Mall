package com.sino.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Catelog2Vo {
    public String catalog1Id;
    public List<Catelog3Vo> catalog3List;
    public String id;
    public String name;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Catelog3Vo{
        public String catalog2Id;
        public String id;
        public String name;
    }
}
